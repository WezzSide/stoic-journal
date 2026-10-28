package com.stoicjournal.core.domain.model

sealed interface AiError {
    data object MissingCredentials : AiError
    data object InvalidCredentials : AiError
    data object AuthenticationExpired : AiError
    data object PermissionDenied : AiError
    data object RateLimited : AiError
    data object QuotaExceeded : AiError
    data object NetworkUnavailable : AiError
    data object RequestTimeout : AiError
    data object ProviderUnavailable : AiError
    data object ContextLimitExceeded : AiError
    data object InvalidResponse : AiError
    data object ContentRejected : AiError
    data object UserCancelled : AiError
    data object BudgetExceeded : AiError
    data class Unknown(val diagnosticCode: String) : AiError
}
