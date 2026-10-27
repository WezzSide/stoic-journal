# Credential Security Design

## Overview
This document specifies the security architecture for handling AI provider credentials (such as API Keys for OpenAI, Anthropic, Gemini, or OAuth tokens for signed-in services) in the Stoic Journal app.

## Threat Model
- **Stolen device**: Protected by Android Keystore (screen lock required to access key in many cases, though we don't bind it directly to biometrics to avoid background refresh interruptions).
- **Malicious app**: Prevented by Android OS sandboxing. The keystore keys are isolated per-app.
- **Backup Leakage**: Prevented. `sj_credentials.pb` is excluded from cloud backups and device-to-device transfers.

## Architecture
1. **Keystore Manager** (`AndroidKeystoreManager`): Securely generates and fetches a 256-bit AES key.
2. **Encryption Engine** (`AesGcmCredentialEncryption`): Uses AES-256-GCM. An initialization vector (nonce) is generated per encryption. It heavily relies on Associated Authenticated Data (AAD) to ensure the ciphertext is inextricably linked to the `providerId` and `credentialType`.
3. **Storage** (`SecureCredentialRepositoryImpl` & `DataStore`): Only the ciphertext, nonce, and alias strings are persisted into a Jetpack DataStore located in `noBackupFilesDir`.
4. **Error Modeling** (`CredentialSecurityError`): Explicitly captures failures such as `MissingKeystoreKey` or `AuthenticationTagMismatch`.

## Future Work
- Rotating Keys (V1 -> V2).
- Secure UI integration (Prompting for Re-authentication).
- Delegated Auth Support (SiWC OAuth).
