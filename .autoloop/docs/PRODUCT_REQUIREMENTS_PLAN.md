# Product Requirements Plan — stoic-journal

Updated 2026-07-05 by autoloop run 0001. **Canonical PRD lives at `docs/prd.md` (2026-07-04, Draft)** — this file tracks requirements *status against the code*, not a duplicate spec.

## Problem statement
Offline-first Android journaling app for daily Stoic reflection: guided prompts, primary-source quotes (Marcus Aurelius, Epictetus, Seneca), and five classical exercises. Private by default — no data leaves the device in MVP.

## Target users
Stoicism beginners (guided, explanatory) and practicing Stoics (primary sources, structured exercises) — see PRD §3.

## Functional requirements (from PRD §4; status = code observed 2026-07-05)
| # | Requirement | Priority | Status in code |
|---|---|---|---|
| FR-1 | Onboarding w/ familiarity selection + reminder + content seeding | Must | ✅ `feature:onboarding` present |
| FR-2 | Today screen: daily prompt, quote, quick actions, streak | Must | ✅ `feature:today`; `StreakCalculator` + `GetTodayPromptUseCase` unit-tested |
| FR-3 | Journal entries: guided / free-form / exercise | Must | ✅ `feature:journal` (`JournalDraftSaver` tested) |
| FR-4 | Wisdom library: quotes by philosopher/theme, favorites | Must | ✅ `feature:library` |
| FR-5 | History: list, filter, detail, swipe-delete w/ undo | Must | ✅ `feature:history` |
| FR-6 | Daily reminder via WorkManager | Must | ✅ `core:notifications` (Hilt worker; Hilt test setup fixed in 970cb90) |
| FR-7 | Settings: reminder, reset onboarding, attribution | Must | ✅ `feature:settings` |

## Non-functional requirements
| # | Requirement | Target | Status |
|---|---|---|---|
| NFR-1 | Fully offline core functionality | always | ✅ by design (Room/DataStore; no network stack observed) |
| NFR-2 | Guided entry completable < 2 min | PRD success criterion | ⚠ not yet measured — needs on-device UX check |
| NFR-3 | Clean build on a standard workstation | always | 🔄 being established this run (previously built only on Termux/aarch64 with a QEMU AAPT2 workaround, `docs/build-environment.md`) |
| NFR-4 | minSdk 26, target/compileSdk 34 | current | ⚠ API 34 targets Android 14; Play's target-API window will require 35+ — see RISK_REGISTER R-3 |

## Out of scope for MVP (roadmap Phases 2–4)
Widget, summaries, export, search (v1.1) · reading collections, lessons (v1.2) · cloud sync, audio, Wear, community (v2.0).
