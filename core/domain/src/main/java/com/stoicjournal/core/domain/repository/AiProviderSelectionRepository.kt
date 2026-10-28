package com.stoicjournal.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists which AI provider the user has explicitly chosen to receive Stoic Mentor
 * conversations. `null` means the user has never made a choice; callers must then fall
 * back to a safe default and must not silently pick a provider the user has not consented to.
 */
interface AiProviderSelectionRepository {
    val activeProviderId: Flow<String?>
    suspend fun setActiveProviderId(providerId: String)
}
