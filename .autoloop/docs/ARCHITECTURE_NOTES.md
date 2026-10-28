# Architecture Notes — stoic-journal

Updated 2026-07-05 (autoloop run 0001). Owned by the Architect role.

## System overview
Multi-module Android app, MVVM + Repository, fully offline:

```
app/                    shell: navigation graph, Compose theme, DI wiring
feature: today, journal, library, history, settings, onboarding
core:    data (Room entities/DAOs/repos/seeders), domain (use cases/models),
         common (utils), notifications (WorkManager reminder + Hilt worker)
build-logic/            convention plugins (includeBuild): AndroidApplication,
                        AndroidLibrary, AndroidFeature, AndroidHilt, AndroidRoom
```

Stack: Kotlin 2.0.0 · AGP 8.5.0 · Gradle 8.7 (wrapper) · Compose BOM 2024.06.00 (Material 3) · Hilt 2.51.1 · Room 2.6.1 · DataStore 1.1.1 · WorkManager 2.9.0 · kotlinx-serialization · JUnit4 + Espresso. Version catalog: `gradle/libs.versions.toml`. compileSdk/targetSdk 34, minSdk 26.

## Key decisions (ADR-lite)
| Date | Decision | Why | Alternatives rejected |
|---|---|---|---|
| pre-2026-07 | Multi-module + convention plugins (nowinandroid-style) | build scaling, per-feature isolation | single-module (simpler but couples features) |
| pre-2026-07 | Offline-first, no network layer in MVP | privacy is the product promise | remote content delivery |
| pre-2026-07 | Room + DataStore, seeded bundled content | offline primary-source library | asset-only content (no favorites/state) |
| 2026-07-05 | Adopt autoloop evidence workflow on branch `autoresearch/mvp-environment-test` | measurable, reversible iteration | ad-hoc changes on main |

## Dependency policy
Version catalog is the single source of truth. New dependencies need a hypothesis §Research justification. Toolchain pins are ~2 years old (AGP 8.5/Kotlin 2.0/BOM 2024.06) — upgrades must come as *separate, single-hypothesis runs* (AGP-9 migration implies the built-in-Kotlin change; do not bundle with feature work).

## Known architectural debt
- Toolchain staleness (see RISK_REGISTER R-2/R-3).
- Unit-test coverage concentrated in `core:domain` + one `feature:journal` class; feature ViewModels untested.
- `docs/build-environment.md` documents a Termux/aarch64 AAPT2 workaround that does not apply to standard hosts — needs a "standard workstation" section so new contributors aren't misled.
