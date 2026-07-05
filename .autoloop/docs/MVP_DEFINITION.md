# MVP Definition — stoic-journal

Updated 2026-07-05 (autoloop run 0001). Source: `docs/prd.md` §2 + README "MVP v1.0".

## MVP statement
> A user can build a private daily Stoic reflection habit: complete a guided entry in under 2 minutes, browse and favorite primary-source quotes, run classical exercises, and get a daily reminder — entirely offline.

## MVP feature cut
| Feature | In MVP? | Rationale |
|---|---|---|
| Onboarding, Today, Journal (3 modes), Library, Exercises, History, Reminder, Settings | ✅ | PRD Phase 1 — all implemented as feature modules |
| Widget, summaries, export, search | ❌ | Roadmap Phase 2 (v1.1) |
| Reading collections, lesson cards | ❌ | Phase 3 (v1.2) |
| Cloud sync, audio, Wear, community | ❌ | Phase 4 (v2.0) |

## MVP quality bar
- `assembleDebug` + `testDebugUnitTest` gates green on a standard workstation (not only the original Termux/aarch64 host)
- CI (`.github/workflows/build.yml`) green on main
- Release build signed (keystore local-only; `*.keystore` correctly gitignored)
- No open Critical/High risks in RISK_REGISTER.md

## Definition of Done for MVP release
- [x] All required gates green (run 0001; baseline promoted 2026-07-05)
- [ ] Instrumentation tests executed on an emulator at least once per release (4 androidTest files exist; not in CI)
- [ ] PRD success criteria verified on device (incl. < 2-min guided entry)
- [ ] Release readiness score ≥ 85
- [ ] Screenshots captured for README/store listing (currently placeholders)
- [ ] Release Manager sign-off recorded in EVIDENCE_LOG.md
