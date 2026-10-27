package com.stoicjournal.core.data.security.storage

import androidx.datastore.core.DataStore
import com.stoicjournal.core.data.security.crypto.CredentialEncryption
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<CredentialPreferences>,
    private val encryption: CredentialEncryption
) : SecureCredentialRepository {

    override suspend fun saveCredential(providerId: String, credential: ProviderCredential) {
        val (type, plaintext) = when (credential) {
            is ProviderCredential.ApiKey -> "API_KEY" to credential.key
            is ProviderCredential.OAuthToken -> "OAUTH_TOKEN" to "${credential.accessToken}|${credential.refreshToken ?: ""}|${credential.expiresAt ?: ""}"
        }
        
        val result = encryption.encrypt(providerId, type, plaintext)
        val envelope = result.getOrThrow()

        dataStore.updateData { prefs ->
            val updated = prefs.credentials.toMutableMap()
            updated[providerId] = envelope
            prefs.copy(credentials = updated)
        }
    }

    override suspend fun getCredential(providerId: String): ProviderCredential? {
        val prefs = dataStore.data.first()
        val envelope = prefs.credentials[providerId] ?: return null
        
        val result = encryption.decrypt(envelope)
        val plaintext = result.getOrNull() ?: return null

        return when (envelope.credentialType) {
            "API_KEY" -> ProviderCredential.ApiKey(plaintext)
            "OAUTH_TOKEN" -> {
                val parts = plaintext.split("|")
                if (parts.size == 3) {
                    ProviderCredential.OAuthToken(
                        accessToken = parts[0],
                        refreshToken = parts[1].takeIf { it.isNotEmpty() },
                        expiresAt = parts[2].takeIf { it.isNotEmpty() }?.toLongOrNull()
                    )
                } else null
            }
            else -> null
        }
    }

    override suspend fun deleteCredential(providerId: String) {
        dataStore.updateData { prefs ->
            val updated = prefs.credentials.toMutableMap()
            updated.remove(providerId)
            prefs.copy(credentials = updated)
        }
    }

    override suspend fun hasCredential(providerId: String): Boolean {
        return dataStore.data.first().credentials.containsKey(providerId)
    }
}
