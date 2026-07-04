package com.stoicjournal.feature.journal.draft

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounced auto-save helper for in-progress journal drafts.
 *
 * The helper owns the debounce delay, the in-flight save job, and the
 * on-exit flush. ViewModels supply the draft key, a function that returns
 * the current [JournalDraft], and a suspend save callback.
 */
class JournalDraftSaver(
    private val viewModelScope: CoroutineScope,
    private val entryType: EntryType,
    private val key: String,
    private val produceDraft: () -> JournalDraft,
    private val saveDraft: suspend (EntryType, String, JournalDraft) -> Unit
) {
    private var saveJob: Job? = null
    private var autoSaveDisabled: Boolean = false

    fun scheduleSave() {
        if (autoSaveDisabled) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            saveDraft(entryType, key, produceDraft())
        }
    }

    /**
     * Cancels any pending auto-save and prevents future saves, including
     * [flushOnExit]. Safe to call multiple times.
     */
    fun disableAutoSave() {
        autoSaveDisabled = true
        saveJob?.cancel()
        saveJob = null
    }

    fun flushOnExit(scope: CoroutineScope) {
        if (autoSaveDisabled) return
        val pendingSave = saveJob?.isCompleted == false
        saveJob?.cancel()
        if (pendingSave) {
            scope.launch {
                saveDraft(entryType, key, produceDraft())
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}
