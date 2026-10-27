package com.stoicjournal.core.domain.model

sealed interface ProviderCredential {
    data class ApiKey(val key: String) : ProviderCredential
    data class OAuthToken(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAt: Long?
    ) : ProviderCredential
}
