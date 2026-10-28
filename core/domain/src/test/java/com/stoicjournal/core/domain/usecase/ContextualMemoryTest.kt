package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.MentorMemory
import com.stoicjournal.core.domain.model.MentorMemoryCategory
import com.stoicjournal.core.domain.model.MentorMemoryStatus
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.MentorMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ContextualMemoryTest {

    private lateinit var journalRepository: FakeJournalRepository
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var retriever: HistoricalReflectionRetriever

    @Before
    fun setUp() {
        journalRepository = FakeJournalRepository()
        memoryRepository = FakeMemoryRepository()
        retriever = HistoricalReflectionRetriever(journalRepository, memoryRepository)
    }

    @Test
    fun `retrieveContext collects approved memories and matching entries`() = runBlocking {
        memoryRepository.saveMemory(
            MentorMemory(
                id = "mem1",
                category = MentorMemoryCategory.USER_PREFERENCE,
                content = "Prefers Marcus Aurelius quotations",
                status = MentorMemoryStatus.APPROVED,
                userApproved = true
            )
        )
        journalRepository.saveEntry(
            JournalEntry(
                id = "entry1",
                type = EntryType.FREE_FORM,
                promptId = null,
                title = "Reflection on Discipline",
                body = "I practiced self-control today during a stressful meeting.",
                quoteId = null,
                exerciseId = null,
                virtues = emptyList(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        val result = retriever.retrieveContext("self-control", maxEntries = 2)
        assertEquals(1, result.approvedMemories.size)
        assertTrue(result.formattedContextString.contains("Marcus Aurelius"))
    }

    private class FakeJournalRepository : JournalRepository {
        private val entries = mutableListOf<JournalEntry>()
        override fun getAllEntries(): Flow<List<JournalEntry>> = flowOf(entries)
        override suspend fun getEntryById(id: String): JournalEntry? = entries.find { it.id == id }
        override suspend fun saveEntry(entry: JournalEntry) { entries.add(entry) }
        override suspend fun deleteEntry(entry: JournalEntry) { entries.remove(entry) }
    }

    private class FakeMemoryRepository : MentorMemoryRepository {
        private val memories = mutableListOf<MentorMemory>()
        override fun getApprovedMemories(): Flow<List<MentorMemory>> = flowOf(memories.filter { it.userApproved })
        override suspend fun saveMemory(memory: MentorMemory) { memories.add(memory) }
        override suspend fun deleteMemory(id: String) { memories.removeAll { it.id == id } }
        override suspend fun clearAll() { memories.clear() }
    }
}
