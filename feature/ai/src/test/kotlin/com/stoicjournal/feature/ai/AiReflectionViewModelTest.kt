package com.stoicjournal.feature.ai

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.AiRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.usecase.GetRecentJournalEntriesUseCase
import com.stoicjournal.core.domain.usecase.GetStoicReflectionUseCase
import com.stoicjournal.core.domain.usecase.SaveAiReflectionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiReflectionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AiReflectionViewModel

    private val fakeAiResponse = AiReflectionResponse(
        insight = "Insight",
        interpretation = "Interpretation",
        reflectiveQuestion = "Question",
        actionableStep = "Step",
        theme = AiReflectionTheme.FOCUS
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val fakeAiRepository = object : AiRepository {
            override suspend fun getReflection(request: AiReflectionRequest) = Result.success(fakeAiResponse)
            override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> = flowOf(fakeAiResponse)
        }

        val fakeJournalRepository = object : JournalRepository {
            override fun getAllEntries(): Flow<List<JournalEntry>> = flowOf(emptyList())
            override suspend fun getEntryById(id: String): JournalEntry? = null
            override suspend fun saveEntry(entry: JournalEntry) {}
            override suspend fun deleteEntry(entry: JournalEntry) {}
        }

        val getRecentJournalEntriesUseCase = GetRecentJournalEntriesUseCase(fakeJournalRepository)
        val getStoicReflectionUseCase = GetStoicReflectionUseCase(fakeAiRepository, getRecentJournalEntriesUseCase)
        val saveAiReflectionUseCase = SaveAiReflectionUseCase(fakeJournalRepository)

        viewModel = AiReflectionViewModel(getStoicReflectionUseCase, saveAiReflectionUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(AiReflectionUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `getReflection updates state to Success on valid input`() = runTest {
        viewModel.getReflection("My thought", AiReflectionTheme.FOCUS)
        
        assertTrue(viewModel.uiState.value is AiReflectionUiState.Success)
        val successState = viewModel.uiState.value as AiReflectionUiState.Success
        assertEquals("Insight", successState.reflection.insight)
    }

    @Test
    fun `getReflection updates state to Error on empty input`() = runTest {
        viewModel.getReflection("", AiReflectionTheme.FOCUS)
        
        assertTrue(viewModel.uiState.value is AiReflectionUiState.Error)
        assertEquals("Please enter your thoughts.", (viewModel.uiState.value as AiReflectionUiState.Error).message)
    }
}
