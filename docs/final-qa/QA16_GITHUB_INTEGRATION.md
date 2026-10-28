# QA16 — GitHub & Branch Integration

## 1. Repository state at mission start (2026-10-28)

| Item | Value |
|---|---|
| Remote | `origin` = `https://github.com/B0LK13/stoic-journal.git` → redirects to **`WezzSide/stoic-journal`** (owner rename); `gh` is authenticated as `WezzSide` |
| Working tree | clean except untracked `docs/final-qa/` (prior session's drafts) |
| Local `feature/stoic-mentor-multiprovider` = `origin/feature/stoic-mentor-multiprovider` | `568cbc7071e85b587a5ce084404e029778797fda` |
| Mission "verified initial SHA" `712fe71…` | ancestor of HEAD (HEAD adds one docs-only commit) |
| Local `main` | `7f890e69e0c023e2df9aba51a3d46364b1540b12` — **stale**, equals the merge-base of the feature branch |
| `origin/main` | `ccb9420175c75815d1795211a585336725dd1cec` — 8 commits ahead of local `main` (Gemini 3.1 Pro AI reflection, Phase 9 search/export, Phase 10 Play-Store prep, committed release/debug APKs, storage reports) |
| Merge-base(feature, origin/main) | `7f890e6` |
| PR #1 | `https://github.com/WezzSide/stoic-journal/pull/1` — **head `sync/2026-10-27-stoic-journal-icon` → base `main`**, state OPEN, `mergeable: CONFLICTING`, `mergeStateStatus: DIRTY`. `origin/sync/2026-10-27-stoic-journal-icon` points at the **same commit** `568cbc7` as the feature branch, so PR #1 effectively is the feature branch. |
| Feature vs origin/main | 248 files / +12,890 −355 on the feature side; 16 files conflict (`git merge-tree --write-tree origin/main 568cbc7`) |

## 2. Conflict analysis (16 files)

| File | Nature | Resolution principle |
|---|---|---|
| `settings.gradle.kts` | foojay plugin 0.10.0 (main) vs 1.0.0 (feature) | keep 1.0.0 (newer, builds) |
| `gradle/gradle-daemon-jvm.properties` | JetBrains JDK 21 (main) vs Microsoft JDK 17 (feature) | keep **17** — project targets JVM 17 and kapt |
| `gradle/libs.versions.toml` | main adds `composeCompiler`, glance; feature adds okhttp, secrets (now removed) | union; keep feature's AGP 8.13.2/Kotlin 2.0.21 line; drop `composeCompiler` (Kotlin 2 Compose plugin) |
| `app/build.gradle.kts` | signing-config expression | keep the QA fail-safe version (no default passwords) |
| `core/data/build.gradle.kts` | feature adds Room schema export + okhttp | keep feature |
| `core/data/.../StoicJournalDatabase.kt` | main: version **2**, `exportSchema=false`, 6 entities; feature: version **6**, 11 entities | keep feature (superset) |
| `core/data/.../DataModule.kt` | main: **`fallbackToDestructiveMigration()`**; feature: explicit migrations | keep feature — destructive fallback is forbidden by the mandatory gates |
| `core/data/.../GeminiAiRepositoryImpl.kt` | main reads **`BuildConfig.GEMINI_API_KEY`** (compiled-in secret, flagged CRITICAL in main's own `KNOWN_BLOCKERS.md`); feature uses an unreachable `"dummy_key"` | keep feature (no compiled-in key); the screen remains unreachable (D-22) |
| `app/.../navigation/Screen.kt`, `StoicJournalNavHost.kt` | feature adds Mentor routes; main routes Today's AI button to the legacy `AiReflection` | keep feature (Mentor) |
| `core/notifications/src/main/AndroidManifest.xml` | main declares `DailyQuoteWidgetReceiver` (+ widget info); feature declares the two journal receivers | **union** — restores the widget (fixes D-23) |
| `feature/today/.../TodayScreen.kt`, `feature/history/.../HistoryScreen.kt` | main adds streak-insight text and history search/filter chips + loading state; feature adds On-This-Day card and swipe-to-delete | **union where both compile**; main's additions depend on `TodayUiState.streakInsight` / `HistoryUiState.selectedFilter` which exist only on main → brought over together |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | adaptive icon with monochrome layer (main) vs colour/mipmap foreground (feature, the "icon" sync branch) | keep feature's icon assets (what the sync branch was for) unless drawables are missing; verified by building |
| `README.md` | ToC/structure text | union, prefer feature's security section |

## 3. Integration candidate

Branch `integration/mentor-into-main` (worktree `.claude/worktrees/integration`), built from `qa/final-qa-001` + `origin/main`:

| Commit | Content |
|---|---|
| `1ce8187` | merge of `origin/main` (ccb9420) with the 17 conflicts resolved per §2 (union where both sides add; feature side where main would regress safety) |
| `6ef6de9` | merge of the later QA commit `1d5708a` (D-34 Upsert fix) |
| `91d12e1` | drops the `secrets` plugin alias that the main merge re-introduced in the root build file |

What the candidate adds over the QA branch (20 files, +348/−68): History search bar + filter chips + loading state and `HistoryList` refactor (VirtueStatsCard kept), streak insight text on Today (`GetStreakInsightsUseCase` now live), adaptive launcher icon drawables, `DailyQuoteWidgetReceiver` declared (fixes D-23), Play-Store metadata/privacy docs, main's README sections. Explicitly **not** taken from main: `fallbackToDestructiveMigration()`, `BuildConfig.GEMINI_API_KEY`, the legacy AI-reflection navigation, AGP 8.5/Kotlin 2.0.0 downgrade, JDK 21 daemon.

Inherited from `origin/main` and flagged for the owner: two committed binaries `apks/stoic-journal-debug.apk` (19.9 MB) and `apks/stoic-journal-release.apk` (13.9 MB). Committing APKs bloats the repository and the release APK's signature/content cannot be tied to source; recommend removing them from `main` (and from history if desired) and using CI artifacts / GitHub Releases instead. Not removed by the agent (owner decision).

| `489cd78` | removes the plain-text release signing credentials that `origin/main` committed in `gradle.properties` (D-37) |

Verification of the candidate: `./gradlew :app:assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, **88/88 unit tests** (`evidence/logs/18_integration_build.log`); release artefacts built from the candidate with environment-supplied credentials (QA17). Instrumentation was executed on the QA branch build; the candidate differs only in main's additive UI/metadata (History search, streak insight, icons, widget manifest) which is covered by its own compile + the unchanged test suite, not by a separate device run (documented limitation).

## 3b. Pushed branches and pull requests

| Branch | Remote head | PR |
|---|---|---|
| `qa/final-qa-001` (feature branch + remediation + evidence) | `43d558f` (+ this doc commit) | **#3** https://github.com/WezzSide/stoic-journal/pull/3 — draft, base `feature/stoic-mentor-multiprovider` |
| `integration/mentor-into-main` (qa + origin/main, conflicts resolved, D-37 removed) | `b3fa012` (+ merge of this doc commit) | **#2** https://github.com/WezzSide/stoic-journal/pull/2 — draft, base `main`, **mergeable** (no conflicts) |
| PR #1 (`sync/2026-10-27-stoic-journal-icon` → `main`) | unchanged, still CONFLICTING | recommend closing in favour of #2 |

## 4. Git restrictions honoured
No force-push, no history rewrite, no branch deletion, no merge into `main`, no release publication. All QA commits are on `qa/final-qa-001`; the integration candidate lives on its own review branch.
