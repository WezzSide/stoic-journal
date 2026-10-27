package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.ProviderCredential

interface SecureCredentialRepository {
    suspend fun saveCredential(providerId: String, credential: ProviderCredential)
    suspend fun getCredential(providerId: String): ProviderCredential?
    suspend fun deleteCredential(providerId: String)
    suspend fun hasCredential(providerId: String): Boolean
}
