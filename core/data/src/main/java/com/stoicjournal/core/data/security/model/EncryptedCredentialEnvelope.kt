package com.stoicjournal.core.data.security.model

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedCredentialEnvelope(
    val formatVersion: Int,
    val keyAlias: String,
    val providerId: String,
    val credentialType: String,
    val algorithm: String,
    val nonce: String,
    val ciphertext: String,
    val createdAt: Long,
    val updatedAt: Long
)
