# Local Storage Analysis Report

**Generated:** 2026-07-05  
**Host:** PRoot-Distro Ubuntu on Android (aarch64)  
**Filesystem:** `/dev/block/dm-65`

---

## Executive Summary

| Metric | Value |
|--------|-------|
| Total capacity | 465 GB |
| Used | 457 GB (99%) |
| Available | 7.8 GB |
| Total inodes | 11,880,957 |
| Used inodes | 9,851,564 (83%) |
| Free inodes | 2,029,393 |

**Critical finding:** The device is critically low on free space. The single
largest consumer is `/sdcard/Download/termux-backups` at **242 GB** (52% of
total storage). Cleaning old backups is the fastest way to reclaim space.

---

## Device-Wide Storage Breakdown

| Location | Size | % of Total | Notes |
|----------|------|------------|-------|
| `/sdcard/Download/termux-backups` | 242 GB | 52.0% | Seven Termux backup archives |
| `/root/.gradle` | 15 GB | 3.2% | Gradle caches and wrapper dists |
| `/root/.cache` | 12 GB | 2.6% | Pip, npm, and other tool caches |
| `/root/.npm` | 7.7 GB | 1.7% | Node package cache |
| `/tmp` | 5.2 GB | 1.1% | Pip installs, wk-gen-test, build temps |
| `/root/projects` | 5.4 GB | 1.2% | All local project checkouts |
| `/product` | 4.6 GB | 1.0% | Android product partition |
| `/root/Android` | 3.0 GB | 0.6% | Secondary Android SDK (NDK-heavy) |
| `/root/.local` | 3.2 GB | 0.7% | Local tool/data installs |
| `/root/go` | 2.5 GB | 0.5% | Go modules and tooling |
| `/root/.cargo` | 1.5 GB | 0.3% | Rust crates and tooling |
| `/root/.bun` | 1.1 GB | 0.2% | Bun runtime/package cache |
| `/root/.venv` | 776 MB | 0.2% | Python virtual environments |
| `/root/bin` | 247 MB | <0.1% | User binaries |
| `/root/.kimi-code` | 91 MB | <0.1% | Session data |
| `/system` | 820 MB | 0.2% | Android system partition |
| `/vendor` | 139 MB | <0.1% | Android vendor partition |
| `/data/local/tmp` | 148 MB | <0.1% | Temp APKs, rish dex |
| `/root/personal-api-hub` | 1.2 GB | 0.3% | Personal API hub projects |
| **Stoic Journal project** | **507 MB** | **0.11%** | See breakdown below |
| **Unaccounted / other** | **~147 GB** | **~32%** | `/data/data`, `/data/misc`, system overhead, small dirs |

---

## `/sdcard/Download/termux-backups` Detail

| File | Size | Date |
|------|------|------|
| `termux-backup-20260702.tar.gz` | 45 GB | Jul 2 |
| `termux-backup-20260628.tar.gz` | 42 GB | Jun 28 |
| `termux-backup-20260630.tar.gz` | 39 GB | Jun 30 |
| `termux-backup-20260627.tar.gz` | 36 GB | Jun 27 |
| `termux-backup-20260626.tar.gz` | 35 GB | Jun 26 |
| `termux-backup-20260701.tar.gz` | 27 GB | Jul 1 |
| `termux-backup-20260625.tar.gz` | 20 GB | Jun 25 |
| **Total** | **242 GB** | |

Keeping only the two most recent backups would free approximately **198 GB**.

---

## Stoic Journal Project Breakdown

| Path/Category | Size | Notes |
|---------------|------|-------|
| `app/build/` | 326 MB | Main app build outputs and intermediates |
| `feature/*/` | 88 MB | Feature module build intermediates |
| `core/*/` | 44 MB | Core module build intermediates |
| `.gradle/` | 39 MB | Project-level Gradle cache |
| `.git/` | 9.1 MB | Git repository history |
| `build-logic/` | 1.2 MB | Convention plugins (source + build) |
| `docs/` | 215 KB | Documentation |
| `scripts/` | 7.5 KB | Verification scripts |
| `metadata/` | 27 KB | Play Store icon |
| **Total** | **507 MB** | |

### App Build Outputs

| APK | Size | Type |
|-----|------|------|
| `app/build/outputs/apk/debug/app-debug.apk` | 17 MB | Debug, signed with Android Debug cert |
| `app/build/outputs/apk/release/app-release.apk` | 12 MB | Release, signed with Stoic Journal cert |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | 997 KB | Instrumentation test APK |

### Largest Items Inside `app/build/`

| Path | Size |
|------|------|
| `app/build/intermediates/incremental/` | 129 MB |
| `app/build/intermediates/dex/` | 98 MB |
| `app/build/intermediates/external_libs_dex/` | 41 MB |
| `app/build/outputs/apk/` | 29 MB |
| `app/build/tmp/` | 8.9 MB |
| `app/build/kotlin/` | 4.8 MB |

---

## `/tmp` Breakdown

| Item | Size | Notes |
|------|------|-------|
| `wk-gen-test/` | 1.7 GB | Unknown generated test data |
| `pip-install-ytji0o_0/` | 1.2 GB | Python package install temp |
| `pip-install-uf5i9gs0/` | 349 MB | Python package install temp |
| `go-build3977936603/` | 282 MB | Go build cache |
| `metro-cache/` | 224 MB | React Native / Metro cache |
| `cargo-install*` dirs | ~408 MB | Rust crate install temps |
| `cmdline-tools-new/` | 159 MB | Android SDK cmdline-tools temp |
| `apk-download/` | 68 MB | APK downloads |
| Others | ~1.0 GB | Misc. temp files |
| **Total** | **5.2 GB** | |

---

## Gradle Cache Detail

`/root/.gradle` totals **15 GB**:

| Subdirectory | Size | Notes |
|--------------|------|-------|
| `caches/` | 14 GB | Main Gradle cache |
| `wrapper/` | 1.1 GB | Gradle distribution zips |
| `daemon/` | 73 MB | Daemon logs/state |
| `native/` | 3.7 MB | Native platform libs |

`/root/.gradle/caches/` breakdown:

| Subdirectory | Size | Notes |
|--------------|------|-------|
| `8.14.3/` | 3.3 GB | Gradle 8.14.3 task cache |
| `modules-2/` | 3.1 GB | Downloaded Maven artifacts |
| `8.11.1/` | 2.8 GB | Gradle 8.11.1 task cache |
| `build-cache-1/` | 1.3 GB | Build cache |
| `transforms-4/` | 1.2 GB | Transformed AARs/jars |
| `8.9/` | 1.2 GB | Gradle 8.9 task cache |
| `8.7/` | 558 MB | Gradle 8.7 task cache |
| `jars-9/` | 788 KB | JAR cache index |
| `journal-1/` | 4.5 MB | Cache journal |

Multiple full Gradle distributions are cached. Removing unused versions (`8.9`,
`8.11.1`, `8.14.3`) could reclaim ~7.3 GB if they are no longer needed.

---

## Inode Usage

| Filesystem | Total Inodes | Used | Free | Use% |
|------------|--------------|------|------|------|
| `/` | 11,880,957 | 9,851,564 | 2,029,393 | 83% |

Inode usage is elevated. The many small files in Gradle caches, Node/npm
packages, Python environments, and backup archives are the likely cause. If
inode exhaustion becomes a problem, cleaning caches and old backups will help
more than deleting large files alone.

---

## Recommendations

1. **Reclaim the most space immediately:** Delete or move old Termux backups in
   `/sdcard/Download/termux-backups/`. Keeping only the latest 1–2 backups frees
   ~198 GB.
2. **Clean temporary caches:** Remove stale `/tmp/pip-install-*`,
   `/tmp/wk-gen-test/`, `/tmp/cargo-install*`, and `/tmp/go-build*` dirs to
   reclaim ~4 GB.
3. **Prune old Gradle versions:** If Gradle 8.9, 8.11.1, and 8.14.3 are no
   longer used, delete their cache directories under `~/.gradle/caches/` to free
   ~7.3 GB.
4. **For Stoic Journal development:** Run `./gradlew clean` between major build
   iterations to drop `app/build/` (~326 MB). It will be rebuilt as needed.
5. **Clear old test APKs:** Remove unused APKs from `/data/local/tmp/` (148 MB
   total, including old project APKs).
6. **Monitor inode usage:** After large cleanups, run `df -i` again. If inode
   use remains above 90%, target cache directories with many small files first.

---

## Commands Used

```bash
# Overall disk usage
df -h /
df -i /

# Project breakdown
du -h --max-depth=1 /root/projects/stoic-journal

# Gradle cache
du -h --max-depth=1 ~/.gradle/caches

# Android SDKs
du -h --max-depth=1 /root/personal-api-hub/android-app/android-sdk
du -h --max-depth=1 /root/Android/Sdk

# /sdcard storage
du -h --max-depth=1 /sdcard
ls -lhS /sdcard/Download/termux-backups

# Temporary storage
du -h --max-depth=1 /tmp
du -sh /data/local/tmp
```
