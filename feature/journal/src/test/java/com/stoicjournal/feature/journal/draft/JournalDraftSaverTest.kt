package com.stoicjournal.feature.journal.draft

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JournalDraftSaverTest {

    @Test
    fun scheduleSave_debouncesMultipleRapidChanges_andSavesLastDraft() = runTestWithScheduler {
        val repo = FakeJournalDraftRepository()
        var draft = JournalDraft(body = "")
        val saver = JournalDraftSaver(
            viewModelScope = this,
            entryType = EntryType.FREE_FORM,
            key = "freeform",
            produceDraft = { draft },
            saveDraft = repo::saveDraft
        )

        draft = JournalDraft(body = "a")
        saver.scheduleSave()
        draft = JournalDraft(body = "ab")
        saver.scheduleSave()
        draft = JournalDraft(body = "abc")
        saver.scheduleSave()

        advanceTimeBy(100)
        runCurrent()
        assertNull(repo.getSavedDraft(EntryType.FREE_FORM, "freeform"))

        advanceTimeBy(300)
        runCurrent()

        assertEquals(
            JournalDraft(body = "abc"),
            repo.getSavedDraft(EntryType.FREE_FORM, "freeform")
        )
    }

    @Test
    fun flushOnExit_savesCurrentDraftWhenCalled() = runTestWithScheduler {
        val repo = FakeJournalDraftRepository()
        val draft = JournalDraft(body = "flush me")
        val saver = JournalDraftSaver(
            viewModelScope = this,
            entryType = EntryType.GUIDED,
            key = "prompt-1",
            produceDraft = { draft },
            saveDraft = repo::saveDraft
        )

        saver.scheduleSave()
        advanceTimeBy(100)
        runCurrent()
        assertNull(repo.getSavedDraft(EntryType.GUIDED, "prompt-1"))

        val exitScope = TestScope(StandardTestDispatcher())
        saver.flushOnExit(exitScope)
        exitScope.advanceUntilIdle()

        assertEquals(
            JournalDraft(body = "flush me"),
            repo.getSavedDraft(EntryType.GUIDED, "prompt-1")
        )
    }

    @Test
    fun disableAutoSave_preventsFlushOnExitFromSaving() = runTestWithScheduler {
        val repo = FakeJournalDraftRepository()
        val draft = JournalDraft(body = "discarded")
        val saver = JournalDraftSaver(
            viewModelScope = this,
            entryType = EntryType.EXERCISE,
            key = "exercise-1",
            produceDraft = { draft },
            saveDraft = repo::saveDraft
        )

        saver.scheduleSave()
        saver.disableAutoSave()

        val exitScope = TestScope(StandardTestDispatcher())
        saver.flushOnExit(exitScope)
        exitScope.advanceUntilIdle()

        assertNull(repo.getSavedDraft(EntryType.EXERCISE, "exercise-1"))
    }

    @Test
    fun scheduleSave_swallowsSaveDraftExceptions() = runTestWithScheduler {
        val repo = FakeJournalDraftRepository(throwOnSave = RuntimeException("boom"))
        val saver = JournalDraftSaver(
            viewModelScope = this,
            entryType = EntryType.FREE_FORM,
            key = "freeform",
            produceDraft = { JournalDraft(body = "text") },
            saveDraft = repo::saveDraft
        )

        saver.scheduleSave()
        advanceTimeBy(500)
        runCurrent()

        // Test completes without propagating the exception.
    }

    @Test
    fun flushOnExit_swallowsSaveDraftExceptions() = runTestWithScheduler {
        val repo = FakeJournalDraftRepository(throwOnSave = RuntimeException("boom"))
        val saver = JournalDraftSaver(
            viewModelScope = this,
            entryType = EntryType.FREE_FORM,
            key = "freeform",
            produceDraft = { JournalDraft(body = "text") },
            saveDraft = repo::saveDraft
        )

        saver.scheduleSave()
        advanceTimeBy(100)
        runCurrent()

        val exitScope = TestScope(StandardTestDispatcher())
        saver.flushOnExit(exitScope)
        exitScope.advanceUntilIdle()

        // Test completes without propagating the exception.
    }

    private fun runTestWithScheduler(block: suspend TestScope.() -> Unit) {
        return TestScope(StandardTestDispatcher()).runTest { block() }
    }

    private class FakeJournalDraftRepository(
        private val throwOnSave: Exception? = null
    ) : JournalDraftRepository {
        private val drafts = mutableMapOf<Pair<EntryType, String>, JournalDraft>()

        override fun getDraft(type: EntryType, key: String): Flow<JournalDraft?> =
            flowOf(drafts[type to key])

        override suspend fun saveDraft(type: EntryType, key: String, draft: JournalDraft) {
            throwOnSave?.let { throw it }
            drafts[type to key] = draft
        }

        override suspend fun clearDraft(type: EntryType, key: String) {
            drafts.remove(type to key)
        }

        fun getSavedDraft(type: EntryType, key: String): JournalDraft? = drafts[type to key]
    }
}
