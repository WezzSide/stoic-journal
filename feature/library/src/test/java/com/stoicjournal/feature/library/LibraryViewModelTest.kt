package com.stoicjournal.feature.library

import com.stoicjournal.core.domain.model.Philosopher
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.ToggleFavoriteQuoteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** QA-04/E2E Wisdom Library: browse, filter by theme / philosopher / favourites, toggle favourite. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val quotes = listOf(
        Quote("q1", "QA control quote", "Epictetus", "epictetus", Theme.CONTROL),
        Quote("q2", "QA adversity quote", "Seneca", "seneca", Theme.ADVERSITY, isFavorite = true),
        Quote("q3", "QA mortality quote", "Marcus Aurelius", "marcus", Theme.MORTALITY),
        Quote("q4", "QA second control quote", "Seneca", "seneca", Theme.CONTROL)
    )
    private val philosophers = listOf(
        Philosopher("epictetus", "Epictetus", "c. 50–135", "QA bio"),
        Philosopher("seneca", "Seneca", "c. 4 BC–65", "QA bio"),
        Philosopher("marcus", "Marcus Aurelius", "121–180", "QA bio")
    )

    private lateinit var quoteRepository: FakeQuoteRepository
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        quoteRepository = FakeQuoteRepository(quotes)
        viewModel = LibraryViewModel(
            quoteRepository,
            FakePhilosopherRepository(philosophers),
            ToggleFavoriteQuoteUseCase(quoteRepository)
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.collectFiltered() =
        backgroundScope.launch(dispatcher) { viewModel.filteredQuotes.collect {} }

    @Test
    fun `library loads all quotes and philosophers`() = runTest(dispatcher) {
        collectFiltered()
        assertEquals(4, viewModel.uiState.value.quotes.size)
        assertEquals(3, viewModel.uiState.value.philosophers.size)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("q1", "q2", "q3", "q4"), viewModel.filteredQuotes.value.map { it.id })
    }

    @Test
    fun `theme filter shows only quotes of that theme`() = runTest(dispatcher) {
        collectFiltered()
        viewModel.selectFilter(LibraryFilter.ByTheme(Theme.CONTROL))
        assertEquals(listOf("q1", "q4"), viewModel.filteredQuotes.value.map { it.id })
    }

    @Test
    fun `philosopher filter shows only that philosopher's quotes`() = runTest(dispatcher) {
        collectFiltered()
        viewModel.selectFilter(LibraryFilter.ByPhilosopher("seneca"))
        assertEquals(listOf("q2", "q4"), viewModel.filteredQuotes.value.map { it.id })
    }

    @Test
    fun `toggling favourite updates the favourites filter`() = runTest(dispatcher) {
        collectFiltered()
        viewModel.selectFilter(LibraryFilter.Favorites)
        assertEquals(listOf("q2"), viewModel.filteredQuotes.value.map { it.id })

        viewModel.toggleFavorite("q3")
        advanceUntilIdle()
        assertEquals(listOf("q2", "q3"), viewModel.filteredQuotes.value.map { it.id })

        viewModel.toggleFavorite("q2")
        advanceUntilIdle()
        assertEquals(listOf("q3"), viewModel.filteredQuotes.value.map { it.id })
    }

    @Test
    fun `favourite failure surfaces a recoverable error`() = runTest(dispatcher) {
        collectFiltered()
        quoteRepository.failToggle = true
        viewModel.toggleFavorite("q1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.error != null)
        viewModel.errorShown()
        assertNull(viewModel.uiState.value.error)
        assertEquals(4, viewModel.filteredQuotes.value.size)
    }

    private class FakeQuoteRepository(initial: List<Quote>) : QuoteRepository {
        private val state = MutableStateFlow(initial)
        var failToggle = false
        override fun getAllQuotes(): Flow<List<Quote>> = state
        override fun getQuotesByPhilosopher(philosopherId: String) =
            state.map { list -> list.filter { it.philosopherId == philosopherId } }
        override fun getQuotesByTheme(theme: Theme) = state.map { list -> list.filter { it.theme == theme } }
        override fun getFavoriteQuotes() = state.map { list -> list.filter { it.isFavorite } }
        override suspend fun getQuoteById(id: String) = state.value.firstOrNull { it.id == id }
        override suspend fun toggleFavorite(quoteId: String) {
            if (failToggle) error("QA simulated write failure")
            state.value = state.value.map { if (it.id == quoteId) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    private class FakePhilosopherRepository(private val list: List<Philosopher>) : PhilosopherRepository {
        override fun getAllPhilosophers(): Flow<List<Philosopher>> = flowOf(list)
        override suspend fun getPhilosopherById(id: String) = list.firstOrNull { it.id == id }
    }
}
