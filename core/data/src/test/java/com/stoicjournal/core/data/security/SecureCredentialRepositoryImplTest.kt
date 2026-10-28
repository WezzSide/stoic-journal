package com.stoicjournal.core.data.security

import androidx.datastore.core.DataStore
import com.stoicjournal.core.data.security.crypto.CredentialEncryption
import com.stoicjournal.core.data.security.model.EncryptedCredentialEnvelope
import com.stoicjournal.core.data.security.storage.CredentialPreferences
import com.stoicjournal.core.data.security.storage.SecureCredentialRepositoryImpl
import com.stoicjournal.core.domain.model.CredentialSecurityError
import com.stoicjournal.core.domain.model.ProviderCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecureCredentialRepositoryImplTest {

    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var fakeEncryption: FakeCredentialEncryption
    private lateinit var repository: SecureCredentialRepositoryImpl

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        fakeEncryption = FakeCredentialEncryption()
        repository = SecureCredentialRepositoryImpl(fakeDataStore, fakeEncryption)
    }

    @Test
    fun `save and get ApiKey credential successfully`() = runBlocking {
        val providerId = "openai"
        val credential = ProviderCredential.ApiKey("sk-test-key")

        repository.saveCredential(providerId, credential)

        assertTrue(repository.hasCredential(providerId))
        val result = repository.getCredential(providerId)
        assertTrue(result.isSuccess)
        assertEquals(credential, result.getOrNull())
    }

    @Test
    fun `save and get OAuthToken credential successfully`() = runBlocking {
        val providerId = "chatgpt_oauth"
        val credential = ProviderCredential.OAuthToken("access-123", "refresh-456", 99999L)

        repository.saveCredential(providerId, credential)

        val result = repository.getCredential(providerId)
        assertTrue(result.isSuccess)
        assertEquals(credential, result.getOrNull())
    }

    @Test
    fun `get missing credential returns failure with MissingCredential error`() = runBlocking {
        val result = repository.getCredential("non_existent")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains(CredentialSecurityError.MissingCredential.toString()) == true)
    }

    @Test
    fun `delete credential removes it from store`() = runBlocking {
        val providerId = "claude"
        repository.saveCredential(providerId, ProviderCredential.ApiKey("sk-ant-test"))
        assertTrue(repository.hasCredential(providerId))

        repository.deleteCredential(providerId)
        assertFalse(repository.hasCredential(providerId))
    }

    @Test
    fun `decryption failure propagates error`() = runBlocking {
        val providerId = "gemini"
        repository.saveCredential(providerId, ProviderCredential.ApiKey("test-key"))
        fakeEncryption.shouldFailDecryption = true

        val result = repository.getCredential(providerId)
        assertTrue(result.isFailure)
    }

    private class FakeDataStore : DataStore<CredentialPreferences> {
        private val state = MutableStateFlow(CredentialPreferences())

        override val data = state

        override suspend fun updateData(transform: suspend (t: CredentialPreferences) -> CredentialPreferences): CredentialPreferences {
            val updated = transform(state.first())
            state.value = updated
            return updated
        }
    }

    private class FakeCredentialEncryption : CredentialEncryption {
        var shouldFailDecryption = false

        override fun encrypt(providerId: String, credentialType: String, plaintext: String): Result<EncryptedCredentialEnvelope> {
            return Result.success(
                EncryptedCredentialEnvelope(
                    formatVersion = 1,
                    keyAlias = "key_v1",
                    providerId = providerId,
                    credentialType = credentialType,
                    algorithm = "AES/GCM",
                    nonce = "fake_nonce",
                    ciphertext = plaintext,
                    createdAt = 0L,
                    updatedAt = 0L
                )
            )
        }

        override fun decrypt(requestedProviderId: String, envelope: EncryptedCredentialEnvelope): Result<String> {
            if (shouldFailDecryption) {
                return Result.failure(IllegalStateException(CredentialSecurityError.DecryptionFailed.toString()))
            }
            if (requestedProviderId != envelope.providerId) {
                return Result.failure(IllegalStateException(CredentialSecurityError.CorruptedCredentialRecord.toString()))
            }
            return Result.success(envelope.ciphertext)
        }
    }
}
