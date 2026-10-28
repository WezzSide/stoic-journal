# QA12 — Security & Privacy Audit

Method: independent read-only static review of commit `568cbc7` (credential crypto, logging, network, exported components, PendingIntents, notifications, app lock, AI data boundary, release hygiene), followed by remediation on `qa/final-qa-001` and on-device checks (sections 9–10). No secret values were printed or stored in this evidence package.

## 1. Credential security

| Control | Finding | Status |
|---|---|---|
| Key generation | AES-256, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, `setRandomizedEncryptionRequired(true)` (`AndroidKeystoreManager.kt:32-40`) | PASS |
| IV / tag / AAD | Keystore-generated random 12-byte IV per encryption (`cipher.iv`), 128-bit tag, AAD binds format version + providerId + credentialType (`AesGcmCredentialEncryption.kt`) | PASS |
| Envelope storage | `DataStore<CredentialPreferences>` in `context.noBackupFilesDir/sj_credentials.pb`; `android:allowBackup="false"` | PASS |
| Plaintext fallback | none; no logging of key material anywhere (grep of all `Log.*`) | PASS |
| Missing/invalidated key | `getExistingKey` called **outside** try → `UnrecoverableKeyException`/`KeyStoreException` would crash the caller (`decrypt`, then `getApiKey()` in providers) | **D-11 fixed**: mapped to `KeyInvalidated` / `MissingKeystoreKey`, surfaced as `MissingCredentials` to the UI |
| Credential deletion | `deleteCredential` existed but had no UI | **fixed**: "Verwijder sleutel" button per provider |
| Key entry UI | API-key field rendered in cleartext, no password keyboard, not cleared after save | **D-10 fixed**: `PasswordVisualTransformation`, `KeyboardType.Password`, `autoCorrect=false`, cleared on save |
| Hardening (optional) | no `setUnlockedDeviceRequired` / StrongBox | OPEN (P3 recommendation) |
| Backup rule XMLs | `backup_rules.xml` / `data_extraction_rules.xml` exist but are not referenced by the manifest (moot: `allowBackup=false` + noBackup dir) | OPEN (P3 hygiene) |

## 2. Sensitive data exposure

* **Logs**: the only content-bearing log was `Log.d("GeminiAiRepository", "Raw JSON: $json")` in the unreachable legacy `AiReflection` path → removed (D-22). Remaining logs print static messages / exceptions only. No `HttpLoggingInterceptor` use; the unused `okhttp.logging` dependency was removed (D-16).
* **Exceptions in UI**: HTTP failures are classified to `AiError` codes; response bodies are never embedded in exception messages or snackbars.
* **Notifications**: static copy only; proactive invitation explicitly `VISIBILITY_PUBLIC` with no user/AI text (verified on device, §9). Quick-check-in confirmation shows the chosen mood chip (mild, pre-existing).
* **Screenshots**: optional `FLAG_SECURE` ("privacy screen") honoured in `MainActivity`.
* **BuildConfig leakage**: the `secrets-gradle-plugin` was applied to `core:data` and `feature:ai` with no config → every key in `local.properties` (the developer's checkout contains `GEMINI_API_KEY`) was compiled into debug `BuildConfig` classes. Nothing read them. → **D-16 fixed**: plugin removed from both modules and the root build; `buildConfig` disabled in `core:data`.

## 3. Network

HTTPS-only hard-coded endpoints (`api.openai.com`, `api.anthropic.com`, Gemini SDK); no `usesCleartextTraffic`, no custom network-security-config; 30 s connect/read/write timeouts; `Authorization: Bearer` / `x-api-key` headers set per request and never logged; OpenAI requests send `"store": false`. Test-only base-URL override is `internal` and defaults to the public host.

## 4. Component exposure

| Component | Exported | Assessment |
|---|---|---|
| `MainActivity` | yes (launcher) | Reads only `NotificationRoutes.EXTRA_ROUTE` (string compared against a whitelist); unknown values are ignored and logged without content. Deep link cannot bypass the lock (§7). |
| `BootCompletedReceiver` | yes, protected by `RECEIVE_BOOT_COMPLETED` | Checks action, only reschedules WorkManager. PASS |
| `ProactiveMentorNotificationReceiver` | **no** | Snooze/Skip only mutate the proactive preferences; now wrapped in try/catch. PASS |
| `JournalDirectReplyReceiver`, `JournalQuickCheckInReceiver` | **no** | Create journal entries from notification actions; protection = non-exported + PendingIntent capability; RemoteInput PI is correctly `FLAG_MUTABLE` with explicit component and pre-set extras. Standard Android trust boundary; documented. |
| `HiltTestActivity` | debug only, not exported | PASS |
| `InitializationProvider` | no; WorkManager initializer removed in favour of `Configuration.Provider` | PASS |
| `DailyQuoteWidgetReceiver` | not declared on this branch | see D-23 / integration |

All other PendingIntents use `FLAG_IMMUTABLE` with distinct request codes; `POST_NOTIFICATIONS` is checked before every `notify()`.

## 5. Application lock

* Lock state lives in `MainViewModel`; locks on cold start when PIN/biometric is enabled and re-locks on foreground after `lockTimeoutSeconds`.
* The whole `NavHost` is composed only when `!isLocked`, so no journal/memory/mentor content renders behind the lock. The notification route is held in `MainActivity` state and consumed by the NavHost → **a notification cannot bypass the lock** (verified by `AppLockDeepLinkE2ETest`, §9).
* Weaknesses (OPEN, documented): 4-digit PIN hashed with unsalted SHA-256 in plain DataStore and no attempt throttling (D-24); biometric-only lock can be enabled without a PIN (lock-out risk, D-30); lifecycle observer added in `LaunchedEffect(Unit)` is never removed (P3).

## 6. AI prompt-injection / data boundary

| Check | Before | After |
|---|---|---|
| Journal/memory text clearly separated from system instructions | appended under plain headings | wrapped in `<<<BEGIN/END_USER_AUTHORISED_CONTEXT>>>` with an explicit "treat as data" instruction; terminators inside user text are stripped (D-21, unit-tested) |
| Model output can change save policy / provider / approval / permissions | **No** — parser reads string fields only; save policy is a caller constant; user must review before save; `virtues` forced empty | unchanged (PASS) |
| Journal text sent without consent | `sendHistoricalContextToProvider` existed but was never read; mitigated only by all flags defaulting to false and having no UI | **D-09 fixed**: retriever returns `EMPTY` unless `memoryEnabled && (journalAccess || approvedMemories) && sendHistoricalContextToProvider` (unit + E2E tested) |
| Provider switching requires consent | no switching existed; conversations always went to OpenAI | **D-02 fixed**: explicit confirmation dialog, persisted selection, status text shows the active provider |
| Gemini role spoofing | history flattened into one text prompt (`USER:`/`MENTOR:` prefixes) | **D-20 fixed**: structured `startChat(history)` turns |
| Unapproved / deleted memories | DAO filters `status='APPROVED' AND userApproved=1`; delete is a soft-delete that never touches journal entries | PASS (unit + instrumentation tested) |
| Context budget | ≤5 memories, ≤3 entries × 150 chars | PASS (tested) |
| Hidden memory extraction | none: memories are only created by the user in `MemorySettingsScreen` and must be approved manually | PASS |

## 7. Release hygiene

| Item | Finding | Status |
|---|---|---|
| Release signing | `app/build.gradle.kts` fell back to `storePassword="android"`, `keyAlias="stoicjournal"`, `keyPassword="android"`; the gitignored `stoic-journal-release.keystore` on the developer machine opens with that default password (verified read-only with `keytool -list`) | **D-08**: build no longer has defaults (unsigned + warning when credentials are absent). **Owner action required**: regenerate the keystore with strong passwords, keep it outside worktree-copied paths; if already uploaded to Play, rotate via Play App Signing. |
| `debuggable` | not set in release; `isMinifyEnabled`/`isShrinkResources` true | PASS |
| Test-only components in release | `FakeAiProvider` is in the production registry (needed by instrumentation); now excluded from `getSupportedProviders()` and never selected implicitly (D-25) | mitigated |
| Hard-coded keys | none (the `"dummy_key"` legacy path is unreachable) | PASS |
| Assets | `core/data/src/main/assets` holds only reference JSON | PASS |
| Dependencies | `androidx.biometric 1.2.0-alpha05` (alpha), `com.google.ai.client.generativeai 0.9.0` (legacy SDK line) | INFO (QA03 §dependency audit) |
| Log stripping | no `-assumenosideeffects` rule for `android.util.Log` | OPEN (P3) |

## 7b. Credentials committed on `origin/main` (D-37, found during integration)

`origin/main` commit `b567d7a` ("Final Release: Verified release APK…") added `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` to the tracked `gradle.properties`; the repository is **public** (`gh repo view` → visibility PUBLIC). The alias (`stoic-alias`) does not match the keystore on the developer machine (`stoicjournal`), so these credentials belong to the keystore that signed the committed `apks/stoic-journal-release.apk`. The keystore file itself was never committed (checked across all refs). Agent action: removed the three lines from the integration candidate (`489cd78`); values are not reproduced anywhere in this evidence package. **Owner action: treat that keystore as compromised-by-credential-leak — rotate it (new upload key via Play App Signing if applicable), purge the values from history if desired (not done: history rewrite is prohibited by the mission), and rotate anything else that shared those passwords.**

## 8. Backup / import

JSON export is plaintext (UI copy says "lokaal"); import trusts the file's `id` with `REPLACE` → a stale or crafted backup silently overwrites entries (D-31, OPEN, documented).

## 9. Device observations

* Proactive notification content on device: static title/body, no journal text (asserted in `ProactiveReminderE2ETest`).
* Lock gate: notification deep link cannot bypass the PIN (`AppLockDeepLinkE2ETest`, 4 consecutive passes).
* Credential store: `no_backup/sj_credentials.pb` (423 bytes) created by the owner's own key entry; agent inspected only provider id/type/timestamp metadata, never the envelope value. `allowBackup=false` confirmed in the merged manifest.
* **Incident (disclosed)**: two tests selected the `claude` provider to exercise the missing-key path while the owner's key was present → live Anthropic calls with synthetic text. No journal content was involved; tests were corrected (QA04 §4). The owner may want to check their Anthropic usage log for 3–4 small `claude-opus-5-5` requests around 17:14–17:36 local time.
* Logcat evidence committed with this package was filtered to the app's own lines; no other application's output or any personal content is included.

## 10. Verdict
See `SJ_FINAL_ACCEPTANCE_REPORT.md` → Privacy & Security Status.
