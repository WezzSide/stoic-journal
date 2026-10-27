# AI-2: Credential Security Report

## Architecture
The application implements an Android Keystore-backed AES-256-GCM authenticated encryption model to securely persist AI provider credentials. These credentials are saved to a separate Jetpack DataStore (`sj_credentials.pb`), entirely isolated from generic preferences.

## Implemented Components
- `SecureCredentialRepository`: The domain abstraction to securely handle `ProviderCredential` storage, retrieval, deletion, and checks.
- `AesGcmCredentialEncryption`: Handles the AES-GCM logic.
- `AndroidKeystoreManager`: Acts as the interface with the `AndroidKeyStore` system, generating the `sj_ai_credential_key_v1` key safely.
- `EncryptedCredentialEnvelope`: The versioned data structure describing the ciphertext, nonce, and provider linkage.
- `CredentialStorageSerializer`: Parses the encrypted structure to the private `sj_credentials.pb` DataStore.

## Cryptographic Algorithm
- `AES/GCM/NoPadding`
- 256-bit Key length
- 128-bit Authentication Tag length (embedded implicitly by Android's GCM implementation)

## Keystore Configuration
- Automatically generated non-exportable 256-bit AES Key.
- Aliases: `sj_ai_credential_key_v1`
- `KeyProperties.PURPOSE_ENCRYPT` and `KeyProperties.PURPOSE_DECRYPT` only.
- `setRandomizedEncryptionRequired(true)` ensures unique 96-bit nonces automatically.

## Hardware-backed Status
The `AndroidKeyStore` implicitly uses StrongBox/TEE where the OS deems it available. We do not strictly enforce `setIsStrongBoxBacked(true)` to ensure backward compatibility across older devices, adhering to the requirement that device compatibility must take precedence.

## Credential Storage
Credentials are encrypted into an `EncryptedCredentialEnvelope` JSON structure and saved using a discrete `DataStore<CredentialPreferences>`. Associated Authenticated Data (AAD) is used securely to bind the ciphertext to a specific version, provider ID, and credential type. 

## Backup Exclusions
The `sj_credentials.pb` file is explicitly excluded from Auto Backup and Device Transfer rules via:
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

This avoids silent key-loss scenarios in which a user restores encrypted records without the associated non-exportable master key.

## Key Invalidation Behavior
If the Keystore key is missing or an unsupported envelope version is detected, the `decrypt` method yields a `Failure` indicating `MissingKeystoreKey` or `UnsupportedEnvelopeVersion`. The UI layer will eventually prompt reauthentication rather than permanently locking the user.

## Rotation Strategy
The architecture is inherently built to support rotation (e.g. `KEY_ALIAS_V2`). In future milestones, adding a migration logic step to detect `KEY_ALIAS_V1`, decrypt, and re-encrypt under `v2` will seamlessly fulfill rotation. 

## Unit and Instrumentation Test Results
Tests have passed successfully locally on the build pipeline across `:core:data`.
Unit test mapping included verification of `SecureCredentialRepositoryImpl` and `FakeAiProvider`.

## Security Findings
- No secrets are logged.
- The `secrets-gradle-plugin` ensures that standard build environments don't compile hard-coded credentials.
- Safe serialization limits data leakage.

## Known Limitations
Storing credentials locally on the device (BYOK) carries inherent risk if the device is rooted or severely compromised. While the database limits automated extraction through backup exclusion and the Keystore provides strong protection at rest, it is not 100% immune to malicious code actively intercepting memory context during decryption runtime.

## Git Commit
The changes are logged under `feature/stoic-mentor-multiprovider`.
