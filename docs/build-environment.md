# Build Environment Notes

## Standard workstation (verified 2026-07-05)

The default, recommended build host. Verified on Windows 11 (HP ZBook, x86_64):

- JDK 17 (any distribution; AGP 8.5.0 requires 17, tested with MS OpenJDK 17.0.19)
- Android SDK with accepted licenses — `local.properties` pointing at it, e.g.
  `sdk.dir=D\:\\Android\\Sdk` (never commit this file; it is gitignored)
- Gradle comes from the wrapper (8.7 — exactly AGP 8.5.0's minimum; no global install needed)
- Missing SDK packages (platform android-34, build-tools 34.0.0) are auto-provisioned
  by AGP on first build when SDK licenses are accepted

```bash
./gradlew assembleDebug        # (Windows: gradlew.bat) first build downloads deps; ~12 min cold, then cached
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Result on the verified host: `BUILD SUCCESSFUL` (376 tasks), APK at
`app/build/outputs/apk/debug/app-debug.apk`. **No AAPT2 override is needed on
x86_64 hosts** — the workaround below is strictly for the Termux/aarch64 setup
and will break a standard build if copied (the path does not exist).

---

## Termux / aarch64 host (historical — original development environment)

- PRoot-Distro Ubuntu on aarch64 (Termux)
- OpenJDK 17 (aarch64)
- Gradle 8.7
- Android Gradle Plugin 8.5.0
- Android SDK: `/root/personal-api-hub/android-app/android-sdk`
- Build-tools: 34.0.0

## Root Cause: AAPT2 Daemon Startup Failure (Termux/aarch64 only)

`./gradlew :app:assembleDebug` failed with:

```
java.io.IOException: Cannot run program ".../aapt2-8.5.0-11315950-linux/aapt2":
error=2, No such file or directory
```

The AAPT2 binary that AGP downloads from Maven is an **x86_64 ELF** linked to
`/lib64/ld-linux-x86-64.so.2`. On this aarch64 container that loader is missing,
so the kernel reports `No such file or directory`.

## Workaround (Termux/aarch64 only — do NOT apply on standard hosts)

The SDK `build-tools/34.0.0/aapt2` wrapper already uses `qemu-x86_64` to run the
x86_64 `aapt2.real` binary, so it works on this host. Force AGP to use that SDK
AAPT2 instead of the Maven one:

```properties
# ~/.gradle/gradle.properties
android.aapt2FromMavenOverride=/root/personal-api-hub/android-app/android-sdk/build-tools/34.0.0/aapt2
```

Equivalently, pass it on the command line:

```bash
./gradlew :app:assembleDebug \
  -Pandroid.aapt2FromMavenOverride=/root/personal-api-hub/android-app/android-sdk/build-tools/34.0.0/aapt2
```

## Verification

With the override in place:

```bash
./gradlew :app:assembleDebug --no-daemon
./gradlew :app:assembleDebugAndroidTest --no-daemon
```

Produces:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

Static APK checks performed:

- `aapt dump badging app-debug.apk`
- `apksigner verify --print-certs app-debug.apk`

### Release build

A signed release APK is also produced with the `release` signing config:

```bash
./gradlew :app:assembleRelease --no-daemon
```

Output: `app/build/outputs/apk/release/app-release.apk`

Verified with:

- `aapt dump badging app-release.apk`
- `apksigner verify --print-certs app-release.apk`

## CI Fallback

Because the local environment is unusual, `.github/workflows/build.yml` builds
the same targets on an Ubuntu x86_64 runner where the AGP-bundled AAPT2 runs
natively. The workflow uploads the debug APK and build reports as artifacts.

## On-Device Verification (Shizuku)

A Shizuku service was available in the Termux/PRoot environment, so both the
debug and release APKs were installed and verified on-device using the
privileged shell.

Run the automated verification script:

```bash
./scripts/shizuku_verify.sh [path/to/app.apk]
```

The script uses `rish` to:

1. Push the APK to `/data/local/tmp` with shell ownership/SELinux context.
2. Install the APK with `pm install -r -t`.
3. Verify package metadata (`pm list packages`, `dumpsys package`).
4. Launch the main activity (`am start`).
5. Confirm the app process is alive (`pidof`).
6. Capture a logcat snapshot and fail on fatal exceptions.

### Verified on-device results

- Package: `com.stoicjournal`
- Version: `1.0` (versionCode 1)
- minSdk: 26, targetSdk: 34
- Debug APK install: Success
- Release APK install: Success
- Launch: `com.stoicjournal/.MainActivity`
- Process: Running
- Logcat: No fatal exceptions

If no Shizuku service is available, fall back to:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
