package com.stoicjournal.feature.ai.mentor

import com.stoicjournal.core.domain.model.AiError

/**
 * A user-facing chat error. [opensSettings] is true when the user can fix it themselves in Settings
 * (missing or rejected API key), so the banner offers a direct "Open Settings" action.
 */
data class MentorChatError(
    val title: String,
    val detail: String,
    val opensSettings: Boolean = false
)

/** Human-readable provider name for UI text. */
fun providerDisplayName(providerId: String): String = when (providerId.lowercase()) {
    "openai" -> "OpenAI"
    "claude", "anthropic" -> "Claude"
    "gemini" -> "Gemini"
    else -> providerId
}

/**
 * Maps a provider error to plain language. Returns null for a user-initiated cancel (not an error).
 * Never includes credentials or message content.
 */
fun AiError.toChatError(providerId: String): MentorChatError? {
    val provider = providerDisplayName(providerId)
    val title = "Message not sent"
    return when (this) {
        AiError.UserCancelled -> null
        AiError.MissingCredentials -> MentorChatError(
            title, "No API key is set for $provider. Add one in Settings → AI provider.", opensSettings = true
        )
        AiError.InvalidCredentials, AiError.AuthenticationExpired, AiError.PermissionDenied -> MentorChatError(
            title, "$provider did not accept the API key. Check it in Settings → AI provider.", opensSettings = true
        )
        AiError.RateLimited -> MentorChatError(title, "$provider is receiving too many requests. Wait a moment and try again.")
        AiError.QuotaExceeded, AiError.BudgetExceeded -> MentorChatError(title, "Your $provider quota or budget is used up.")
        AiError.NetworkUnavailable -> MentorChatError(title, "No internet connection. Check your connection and try again.")
        AiError.RequestTimeout -> MentorChatError(title, "The mentor took too long to answer. Try again.")
        AiError.ProviderUnavailable -> MentorChatError(title, "$provider is temporarily unavailable. Try again later.")
        AiError.ContextLimitExceeded -> MentorChatError(title, "This conversation has become too long. Start a new conversation.")
        AiError.ContentRejected -> MentorChatError(title, "$provider declined to answer this message.")
        AiError.InvalidResponse -> MentorChatError(title, "$provider sent an answer the app could not read. Try again.")
        is AiError.Unknown -> MentorChatError(title, "Something went wrong ($diagnosticCode). Try again.")
    }
}
