package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import kotlinx.coroutines.flow.Flow

/**
 * Persists transient journal-entry drafts so in-progress work is not lost.
 * Implementations are expected to be lightweight (e.g. DataStore).
 */
interface JournalDraftRepository {
    /**
     * Emits the stored draft for the given entry type and key, or null if none exists.
     */
    fun getDraft(type: EntryType, key: String): Flow<JournalDraft?>

    /**
     * Persists [draft] for the given entry type and key.
     */
    suspend fun saveDraft(type: EntryType, key: String, draft: JournalDraft)

    /**
     * Removes the draft for the given entry type and key.
     */
    suspend fun clearDraft(type: EntryType, key: String)
}
