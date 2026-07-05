# Quality Metrics — stoic-journal

Updated 2026-07-05 (autoloop run 0001). Source of truth: `.autoloop/runs/NNNN/gates_*.json` + `baseline.json`.

| Metric | Source | Baseline (run 0001) | Latest | Direction |
|---|---|---|---|---|
| Required gates passing (build, unit_tests) | gates | being captured | — | must stay ✅ |
| Gate wall-time (s) | gates total_seconds | being captured (cold build incl. dependency downloads — treat as upper bound; warm baseline promoted after first keep) | — | ↓ better, ≤ +30% tolerated |
| Unit test files / tests | testDebugUnitTest output | 3 files (core:domain ×2, feature:journal ×1) | — | ↑ better |
| Instrumentation test files | src/androidTest | 4 (never executed on this host) | — | must run ≥1×/release |
| Lint errors | lintDebug (optional gate) | being captured | — | ↓ better |
| Modules | settings.gradle.kts | 12 (app + 6 feature + 4 core + build-logic) | — | stable |
| Kotlin LOC (est.) | autoloop analyze | ~5,978 across 109 files | — | n/a |

Notes: cold-build seconds are not comparable to warm-build seconds — only compare like-for-like (autoloop promotes baselines on keep, which will be a warm build).
