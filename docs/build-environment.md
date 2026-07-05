# Build Environment Notes

## Verified Host

- PRoot-Distro Ubuntu on aarch64 (Termux)
- OpenJDK 17 (aarch64)
- Gradle 8.7
- Android Gradle Plugin 8.5.0
- Android SDK: `/root/personal-api-hub/android-app/android-sdk`
- Build-tools: 34.0.0

## Root Cause: AAPT2 Daemon Startup Failure

`./gradlew :app:assembleDebug` failed with:

```
java.io.IOException: Cannot run program ".../aapt2-8.5.0-11315950-linux/aapt2":
error=2, No such file or directory
```

The AAPT2 binary that AGP downloads from Maven is an **x86_64 ELF** linked to
`/lib64/ld-linux-x86-64.so.2`. On this aarch64 container that loader is missing,
so the kernel reports `No such file or directory`.

## Workaround

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

## CI Fallback

Because the local environment is unusual, `.github/workflows/build.yml` builds
the same targets on an Ubuntu x86_64 runner where the AGP-bundled AAPT2 runs
natively. The workflow uploads the debug APK and build reports as artifacts.

 ## On-Device Verification (Shizuku)

A Shizuku service was available in the Termux/PRoot environment, so the debug
APK was installed and verified on-device using the privileged shell.

Run the automated verification script:

```bash
./scripts/shizuku_verify.sh [path/to/app-debug.apk]
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
- Install: Success
- Launch: `com.stoicjournal/.MainActivity`
- Process: Running
- Logcat: No fatal exceptions

If no Shizuku service is available, fall back to:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
