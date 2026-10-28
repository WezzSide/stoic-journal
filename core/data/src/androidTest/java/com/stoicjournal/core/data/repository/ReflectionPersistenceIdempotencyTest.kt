package com.stoicjournal.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stoicjournal.core.data.dao.MentorReflectionLinkDao
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.entity.MentorReflectionLinkEntity
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.model.Virtue
import com.stoicjournal.core.domain.usecase.ReflectionCandidateValidator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * QA-07 acceptance invariant: for one candidate and one conversation exactly one journal entry is
 * created regardless of repeated, concurrent, duplicated or interrupted save attempts.
 * Uses an in-memory Room database — never the user's data.
 */
@RunWith(AndroidJUnit4::class)
class ReflectionPersistenceIdempotencyTest {

    private lateinit var db: StoicJournalDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, StoicJournalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun candidate(id: String = "cand-1", conv: String = "conv-1", title: String = "QA Reflection 001") = ReflectionCandidate(
        candidateId = id, conversationId = conv, title = title, summary = "QA summary", whatHappened = "x",
        keyInsights = "k", withinControl = "w", outsideControl = "o", virtues = listOf(Virtue.WISDOM),
        nextAction = "n", generatedAt = Instant.now(), providerId = "fake_provider", modelId = "fake-model-1"
    )

    private fun coordinator(linkDao: MentorReflectionLinkDao = db.mentorReflectionLinkDao()) =
        ReflectionPersistenceCoordinatorImpl(db, db.journalEntryDao(), linkDao, ReflectionCandidateValidator())

    @Test
    fun repeatedSaves_createExactlyOneEntry() = runBlocking {
        val c = coordinator()
        val first = c.saveReflectionCandidate(candidate(), "REVIEW_REQUIRED").getOrThrow()
        val second = c.saveReflectionCandidate(candidate(title = "edited again"), "REVIEW_REQUIRED").getOrThrow()
        val third = c.saveReflectionCandidate(candidate(), "MANUAL").getOrThrow()
        assertEquals(first.id, second.id)
        assertEquals(first.id, third.id)
        assertEquals(1, db.journalEntryDao().getAll().first().size)
    }

    @Test
    fun concurrentSaves_duplicateCallbacks_createExactlyOneEntry() = runBlocking {
        val c = coordinator()
        val results = coroutineScope { (1..25).map { async { c.saveReflectionCandidate(candidate(), "REVIEW_REQUIRED") } }.awaitAll() }
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, results.map { it.getOrThrow().id }.toSet().size)
        assertEquals(1, db.journalEntryDao().getAll().first().size)
    }

    @Test
    fun differentCandidates_sameConversation_createSeparateEntries() = runBlocking {
        val c = coordinator()
        c.saveReflectionCandidate(candidate(id = "cand-1"), "REVIEW_REQUIRED").getOrThrow()
        c.saveReflectionCandidate(candidate(id = "cand-2"), "REVIEW_REQUIRED").getOrThrow()
        assertEquals(2, db.journalEntryDao().getAll().first().size)
    }

    @Test
    fun invalidCandidate_writesNothing() = runBlocking {
        val result = coordinator().saveReflectionCandidate(candidate(title = "   "), "REVIEW_REQUIRED")
        assertTrue(result.isFailure)
        assertEquals(0, db.journalEntryDao().getAll().first().size)
    }

    @Test
    fun interruptedSave_rollsBackJournalEntry() = runBlocking {
        val failingLinkDao = object : MentorReflectionLinkDao {
            override suspend fun getLink(conversationId: String, candidateId: String) = db.mentorReflectionLinkDao().getLink(conversationId, candidateId)
            override suspend fun insertLink(link: MentorReflectionLinkEntity): Long = throw IllegalStateException("simulated write failure")
        }
        val result = coordinator(failingLinkDao).saveReflectionCandidate(candidate(), "REVIEW_REQUIRED")
        assertTrue(result.isFailure)
        // The journal insert happened inside the same transaction and must have been rolled back.
        assertEquals(0, db.journalEntryDao().getAll().first().size)
        // A subsequent healthy save succeeds exactly once.
        coordinator().saveReflectionCandidate(candidate(), "REVIEW_REQUIRED").getOrThrow()
        assertEquals(1, db.journalEntryDao().getAll().first().size)
    }

    @Test
    fun savedEntry_isVisibleThroughJournalDaoWithMentorContent() = runBlocking {
        val entry = coordinator().saveReflectionCandidate(candidate(), "REVIEW_REQUIRED").getOrThrow()
        val stored = db.journalEntryDao().getById(entry.id)!!
        assertEquals("QA Reflection 001", stored.title)
        assertTrue(stored.body.contains("Within My Control"))
        assertEquals("WISDOM", stored.virtues)
    }
}
