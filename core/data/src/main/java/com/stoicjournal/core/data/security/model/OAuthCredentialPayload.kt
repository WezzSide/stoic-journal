package com.stoicjournal.core.data.security.model

import kotlinx.serialization.Serializable

@Serializable
data class OAuthCredentialPayload(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val tokenType: String = "Bearer",
    val scopes: List<String> = emptyList()
)
