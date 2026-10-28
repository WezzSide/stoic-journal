# SJ_SECURITY_AND_PRIVACY_AUDIT

## Security Boundaries Enforced
- **Keystore Encryption**: User credentials (`OpenAI`, `Anthropic`, `Gemini`) encrypted locally utilizing `AndroidKeyStore` AES-256-GCM encryption with strictly bounded Auth/Auth components (`SecureCredentialRepositoryImpl.kt`).
- **Data Protection**: Privacy-safe back-up default enabled globally via `android:allowBackup="false"` in Manifest.
- **Biometric Enforced Settings**: Supported by Android 11+ BiometricPrompt capabilities. Handled locally via SharedPreferences masking.

## Privacy Check
- **Memory Opt-in**: `MentorMemoryPreferencesRepositoryImpl` dictates all external tracking of past discussions is **DISABLED** by default.
- **Provider Switching**: Models are fully compartmentalised; memory objects persist locally through Room and aren't submitted until user permits `sendHistoricalContextToProvider = true`.
- **Notification Text**: Refactored `JournalDirectReplyReceiver` confirming reflection saves without passing explicit user-written context to standard notification logs.
