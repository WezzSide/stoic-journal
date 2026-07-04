package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.journalDraftDataStore: DataStore<Preferences> by preferencesDataStore(name = "journal_drafts")

@Singleton
class JournalDraftRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : JournalDraftRepository {

    private val dataStore = context.journalDraftDataStore

    override fun getDraft(type: EntryType, key: String): Flow<JournalDraft?> = dataStore.data.map { prefs ->
        val prefix = keyPrefix(type, key)
        val title = prefs[stringPreferencesKey("${prefix}_title")]
        val body = prefs[stringPreferencesKey("${prefix}_body")]
        val reflection = prefs[stringPreferencesKey("${prefix}_reflection")]
        val quoteId = prefs[stringPreferencesKey("${prefix}_quoteId")]

        if (title == null && body == null && reflection == null && quoteId == null) {
            null
        } else {
            JournalDraft(
                title = title ?: "",
                body = body ?: "",
                reflection = reflection ?: "",
                quoteId = quoteId
            )
        }
    }

    override suspend fun saveDraft(type: EntryType, key: String, draft: JournalDraft) {
        dataStore.edit { prefs ->
            val prefix = keyPrefix(type, key)
            prefs[stringPreferencesKey("${prefix}_title")] = draft.title
            prefs[stringPreferencesKey("${prefix}_body")] = draft.body
            prefs[stringPreferencesKey("${prefix}_reflection")] = draft.reflection
            draft.quoteId?.let {
                prefs[stringPreferencesKey("${prefix}_quoteId")] = it
            } ?: prefs.remove(stringPreferencesKey("${prefix}_quoteId"))
        }
    }

    override suspend fun clearDraft(type: EntryType, key: String) {
        dataStore.edit { prefs ->
            val prefix = keyPrefix(type, key)
            prefs.remove(stringPreferencesKey("${prefix}_title"))
            prefs.remove(stringPreferencesKey("${prefix}_body"))
            prefs.remove(stringPreferencesKey("${prefix}_reflection"))
            prefs.remove(stringPreferencesKey("${prefix}_quoteId"))
        }
    }

    private fun keyPrefix(type: EntryType, key: String): String = "draft_${type.name}_$key"
}
