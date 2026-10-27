package com.stoicjournal.core.data.security.storage

import androidx.datastore.core.DataStore
import com.stoicjournal.core.data.security.crypto.CredentialEncryption
import com.stoicjournal.core.data.security.model.OAuthCredentialPayload
import com.stoicjournal.core.domain.model.CredentialSecurityError
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            is ProviderCredential.OAuthToken -> {
                val payload = OAuthCredentialPayload(
                    accessToken = credential.accessToken,
                    refreshToken = credential.refreshToken,
                    expiresAt = credential.expiresAt
                )
                "OAUTH_TOKEN" to Json.encodeToString(payload)
            }
        }
        
        val result = encryption.encrypt(providerId, type, plaintext)
        val envelope = result.getOrThrow()

        dataStore.updateData { prefs ->
            val updated = prefs.credentials.toMutableMap()
            updated[providerId] = envelope
            prefs.copy(credentials = updated)
        }
    }

    override suspend fun getCredential(providerId: String): Result<ProviderCredential> {
        val prefs = try {
            dataStore.data.first()
        } catch (e: Exception) {
            return Result.failure(IllegalStateException(CredentialSecurityError.StorageUnavailable.toString(), e))
        }

        val envelope = prefs.credentials[providerId] 
            ?: return Result.failure(IllegalStateException(CredentialSecurityError.MissingCredential.toString()))
        
        val result = encryption.decrypt(providerId, envelope)
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }

        val plaintext = result.getOrThrow()

        val credential = when (envelope.credentialType) {
            "API_KEY" -> ProviderCredential.ApiKey(plaintext)
            "OAUTH_TOKEN" -> {
                try {
                    val payload = Json.decodeFromString<OAuthCredentialPayload>(plaintext)
                    ProviderCredential.OAuthToken(
                        accessToken = payload.accessToken,
                        refreshToken = payload.refreshToken,
                        expiresAt = payload.expiresAt
                    )
                } catch (e: Exception) {
                    return Result.failure(IllegalStateException(CredentialSecurityError.CorruptedCredentialRecord.toString(), e))
                }
            }
            else -> return Result.failure(IllegalStateException(CredentialSecurityError.UnsupportedEnvelopeVersion.toString()))
        }

        return Result.success(credential)
    }

    override suspend fun deleteCredential(providerId: String) {
        dataStore.updateData { prefs ->
            val updated = prefs.credentials.toMutableMap()
            updated.remove(providerId)
            prefs.copy(credentials = updated)
        }
    }

    override suspend fun hasCredential(providerId: String): Boolean {
        return try {
            dataStore.data.first().credentials.containsKey(providerId)
        } catch (e: Exception) {
            false
        }
    }
}
