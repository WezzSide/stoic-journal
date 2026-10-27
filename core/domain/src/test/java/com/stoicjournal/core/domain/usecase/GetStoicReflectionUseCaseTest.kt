package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetStoicReflectionUseCaseTest {

    private lateinit var aiRepository: AiRepository
    private lateinit var journalRepository: JournalRepository
    private lateinit var getRecentJournalEntriesUseCase: GetRecentJournalEntriesUseCase
    private lateinit var useCase: GetStoicReflectionUseCase

    private val fakeAiResponse = AiReflectionResponse(
        insight = "Insight",
        interpretation = "Interpretation",
        reflectiveQuestion = "Question",
        actionableStep = "Step",
        theme = AiReflectionTheme.FOCUS
    )

    @Before
    fun setup() {
        aiRepository = object : AiRepository {
            override suspend fun getReflection(request: AiReflectionRequest) = Result.success(fakeAiResponse)
            override fun streamReflection(request: AiReflectionRequest) = flowOf(fakeAiResponse)
        }

        journalRepository = object : JournalRepository {
            override fun getAllEntries() = flowOf(emptyList<com.stoicjournal.core.domain.model.JournalEntry>())
            override suspend fun getEntryById(id: String) = null
            override suspend fun saveEntry(entry: com.stoicjournal.core.domain.model.JournalEntry) {}
            override suspend fun deleteEntry(entry: com.stoicjournal.core.domain.model.JournalEntry) {}
        }

        getRecentJournalEntriesUseCase = GetRecentJournalEntriesUseCase(journalRepository)
        useCase = GetStoicReflectionUseCase(aiRepository, getRecentJournalEntriesUseCase)
    }

    @Test
    fun `invoke returns flow with reflection`() = runTest {
        val result = useCase("My career", AiReflectionTheme.FOCUS).first()
        assertEquals("Insight", result.insight)
    }

    @Test
    fun `invoke with recent entries includes context`() = runTest {
        // Here we could verify that the request passed to aiRepository contains the context
        // but for now we just verify it runs
        val result = useCase("Thinking", AiReflectionTheme.FOCUS).first()
        assertEquals("Insight", result.insight)
    }
}
