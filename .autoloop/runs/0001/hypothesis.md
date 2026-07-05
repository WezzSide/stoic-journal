# Hypothesis — run 0001 (2026-07-05)

## Hypothesis (one concrete improvement)
> If we document the standard-workstation build path (verified today on Windows 11 + JDK 17 +
> SDK at D:\Android\Sdk) in `docs/build-environment.md` — which currently *only* describes the
> exotic Termux/aarch64 host with a QEMU AAPT2 override — then contributor onboarding risk R-8
> drops and AC-1 ("clean checkout builds on standard workstation") becomes documented, verified
> fact, with zero code risk.

## Research (fill BEFORE changing code)
- Primary evidence: this machine's baseline build — `gradlew.bat assembleDebug` BUILD SUCCESSFUL
  in 12m17s cold (376 tasks) with only `local.properties` pointing at the SDK; AGP auto-provisioned
  platform 34 / build-tools 34.0.0 from the accepted-license SDK.
- Official docs: AGP 8.5 release notes — requires JDK 17, minimum Gradle 8.7 (repo wrapper is
  exactly 8.7, consistent); `local.properties` `sdk.dir` is the canonical SDK pointer and must
  not be committed (repo .gitignore already covers it, lines 3/13).
- Known issues checked: the AAPT2 Maven override in the existing doc is HARMFUL on x86_64 hosts
  (points at a nonexistent aarch64-container path); doc must scope it to Termux hosts only.
- Decision: extend `docs/build-environment.md` with a "Standard workstation (verified)" section
  + scope the Termux section header; no build-file changes in this run.

## Plan
- Files to touch: `docs/build-environment.md` only.
- Expected evidence of success: gates stay green post-change (docs-only, proves no regression);
  R-8 mitigated; AC-1 status flips to ✅ verified+documented in living docs.

## Outcome (fill AFTER decide)
- Verdict:
- Evidence:
- If reverted — why it failed, and the improved next hypothesis:
