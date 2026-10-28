package com.stoicjournal.core.data.security.storage

import kotlinx.serialization.Serializable
import com.stoicjournal.core.data.security.model.EncryptedCredentialEnvelope

@Serializable
data class CredentialPreferences(
    val credentials: Map<String, EncryptedCredentialEnvelope> = emptyMap()
)
