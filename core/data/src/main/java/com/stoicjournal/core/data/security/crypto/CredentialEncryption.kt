package com.stoicjournal.core.data.security.crypto

import com.stoicjournal.core.data.security.model.EncryptedCredentialEnvelope

interface CredentialEncryption {
    fun encrypt(providerId: String, credentialType: String, plaintext: String): Result<EncryptedCredentialEnvelope>
    fun decrypt(requestedProviderId: String, envelope: EncryptedCredentialEnvelope): Result<String>
}
