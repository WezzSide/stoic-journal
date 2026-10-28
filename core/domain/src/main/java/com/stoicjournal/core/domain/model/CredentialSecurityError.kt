package com.stoicjournal.core.domain.model

sealed interface CredentialSecurityError {
    data object MissingCredential : CredentialSecurityError
    data object MissingKeystoreKey : CredentialSecurityError
    data object KeyInvalidated : CredentialSecurityError
    data object DecryptionFailed : CredentialSecurityError
    data object AuthenticationTagMismatch : CredentialSecurityError
    data object CorruptedCredentialRecord : CredentialSecurityError
    data object UnsupportedEnvelopeVersion : CredentialSecurityError
    data object StorageUnavailable : CredentialSecurityError
    data object UserReauthenticationRequired : CredentialSecurityError
}
