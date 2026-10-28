package com.stoicjournal.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.domain.model.ConversationState
import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Regression for D-34: updating a conversation row must not cascade-delete its messages. */
@RunWith(AndroidJUnit4::class)
class MentorConversationPersistenceTest {
    private lateinit var db: StoicJournalDatabase
    private lateinit var repo: MentorConversationRepositoryImpl

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, StoicJournalDatabase::class.java).build()
        repo = MentorConversationRepositoryImpl(db.mentorConversationDao(), db.mentorMessageDao())
    }

    @After fun tearDown() = db.close()

    private fun conv(state: ConversationState) = MentorConversation(
        id = "c1", title = "QA Conversation 001", createdAt = Instant.EPOCH, updatedAt = Instant.now(),
        providerId = "fake_provider", modelId = "fake-model-1", state = state, reflectionStatus = null, linkedJournalEntryId = null
    )

    @Test
    fun updatingConversation_keepsMessages_andRemainsResumable() = runBlocking {
        repo.saveConversation(conv(ConversationState.CREATED))
        repo.saveMessage(MentorMessage("m1", "c1", MessageRole.USER, "QA user", Instant.now(), 1))
        repo.saveMessage(MentorMessage("m2", "c1", MessageRole.MENTOR, "QA mentor", Instant.now(), 2))

        repo.saveConversation(conv(ConversationState.ACTIVE))          // update, as the coordinator does on Completed
        repo.saveMessage(MentorMessage("m2", "c1", MessageRole.MENTOR, "QA mentor edited", Instant.now(), 2)) // upsert message

        val messages = repo.getMessagesForConversation("c1").first()
        assertEquals(listOf("m1", "m2"), messages.map { it.id })
        assertEquals("QA mentor edited", messages[1].content)
        assertEquals(ConversationState.ACTIVE, repo.getConversation("c1").first()!!.state)
        assertEquals(1, repo.getAllConversations().first().size)

        repo.deleteConversation("c1")                                   // explicit delete still cascades
        assertEquals(0, repo.getMessagesForConversation("c1").first().size)
    }
}
