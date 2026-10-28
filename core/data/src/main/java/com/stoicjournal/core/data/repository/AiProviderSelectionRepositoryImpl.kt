package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.repository.AiProviderSelectionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiProviderSelectionDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_provider_selection")

@Singleton
class AiProviderSelectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AiProviderSelectionRepository {

    private object Keys {
        val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
    }

    override val activeProviderId: Flow<String?> =
        context.aiProviderSelectionDataStore.data.map { prefs -> prefs[Keys.ACTIVE_PROVIDER_ID] }

    override suspend fun setActiveProviderId(providerId: String) {
        context.aiProviderSelectionDataStore.edit { it[Keys.ACTIVE_PROVIDER_ID] = providerId }
    }
}
