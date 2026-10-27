package com.stoicjournal.core.data.security.crypto

import android.util.Base64
import com.stoicjournal.core.data.security.model.EncryptedCredentialEnvelope
import com.stoicjournal.core.domain.model.CredentialSecurityError
import java.time.Instant
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesGcmCredentialEncryption @Inject constructor(
    private val keystoreManager: AndroidKeystoreManager
) : CredentialEncryption {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BIT = 128
        const val CURRENT_FORMAT_VERSION = 1
        const val KEY_ALIAS_V1 = "sj_ai_credential_key_v1"
        private val ALLOWED_ALIASES = setOf(KEY_ALIAS_V1)
    }

    private fun buildAad(providerId: String, credentialType: String): ByteArray {
        return "com.stoicjournal:version=$CURRENT_FORMAT_VERSION:provider=$providerId:credential=$credentialType"
            .toByteArray(Charsets.UTF_8)
    }

    override fun encrypt(providerId: String, credentialType: String, plaintext: String): Result<EncryptedCredentialEnvelope> {
        return try {
            val secretKey = keystoreManager.createKeyIfAbsent(KEY_ALIAS_V1)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            cipher.updateAAD(buildAad(providerId, credentialType))
            
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val nonce = cipher.iv

            val envelope = EncryptedCredentialEnvelope(
                formatVersion = CURRENT_FORMAT_VERSION,
                keyAlias = KEY_ALIAS_V1,
                providerId = providerId,
                credentialType = credentialType,
                algorithm = TRANSFORMATION,
                nonce = Base64.encodeToString(nonce, Base64.NO_WRAP),
                ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                createdAt = Instant.now().toEpochMilli(),
                updatedAt = Instant.now().toEpochMilli()
            )
            Result.success(envelope)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun decrypt(requestedProviderId: String, envelope: EncryptedCredentialEnvelope): Result<String> {
        if (envelope.providerId != requestedProviderId) {
            return Result.failure(IllegalStateException(CredentialSecurityError.CorruptedCredentialRecord.toString()))
        }
        if (envelope.formatVersion != CURRENT_FORMAT_VERSION) {
            return Result.failure(IllegalStateException(CredentialSecurityError.UnsupportedEnvelopeVersion.toString()))
        }
        if (!ALLOWED_ALIASES.contains(envelope.keyAlias)) {
            return Result.failure(IllegalStateException(CredentialSecurityError.CorruptedCredentialRecord.toString()))
        }

        val secretKey = keystoreManager.getExistingKey(envelope.keyAlias)
            ?: return Result.failure(IllegalStateException(CredentialSecurityError.MissingKeystoreKey.toString()))

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val nonce = Base64.decode(envelope.nonce, Base64.NO_WRAP)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, nonce)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.updateAAD(buildAad(envelope.providerId, envelope.credentialType))
            
            val ciphertext = Base64.decode(envelope.ciphertext, Base64.NO_WRAP)
            val plaintext = cipher.doFinal(ciphertext)
            
            Result.success(String(plaintext, Charsets.UTF_8))
        } catch (e: AEADBadTagException) {
            Result.failure(IllegalStateException(CredentialSecurityError.AuthenticationTagMismatch.toString(), e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(CredentialSecurityError.DecryptionFailed.toString(), e))
        }
    }
}
