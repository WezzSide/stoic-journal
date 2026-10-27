package com.stoicjournal.core.data.security.crypto

import android.util.Base64
import com.stoicjournal.core.data.security.model.EncryptedCredentialEnvelope
import com.stoicjournal.core.domain.model.AiError
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
    }

    private fun buildAad(providerId: String, credentialType: String): ByteArray {
        return "com.stoicjournal:version=$CURRENT_FORMAT_VERSION:provider=$providerId:credential=$credentialType"
            .toByteArray(Charsets.UTF_8)
    }

    override fun encrypt(providerId: String, credentialType: String, plaintext: String): Result<EncryptedCredentialEnvelope> {
        return try {
            val secretKey = keystoreManager.getOrGenerateKey(KEY_ALIAS_V1)
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
            Result.failure(Exception("Encryption failed", e))
        }
    }

    override fun decrypt(envelope: EncryptedCredentialEnvelope): Result<String> {
        if (envelope.formatVersion != CURRENT_FORMAT_VERSION) {
            return Result.failure(Exception("Unsupported envelope version"))
        }
        if (!keystoreManager.hasKey(envelope.keyAlias)) {
            return Result.failure(Exception("Missing keystore key"))
        }
        
        return try {
            val secretKey = keystoreManager.getOrGenerateKey(envelope.keyAlias)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val nonce = Base64.decode(envelope.nonce, Base64.NO_WRAP)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, nonce)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.updateAAD(buildAad(envelope.providerId, envelope.credentialType))
            
            val ciphertext = Base64.decode(envelope.ciphertext, Base64.NO_WRAP)
            val plaintext = cipher.doFinal(ciphertext)
            
            Result.success(String(plaintext, Charsets.UTF_8))
        } catch (e: AEADBadTagException) {
            Result.failure(Exception("Authentication tag mismatch", e))
        } catch (e: Exception) {
            Result.failure(Exception("Decryption failed", e))
        }
    }
}
