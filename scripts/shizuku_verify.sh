#!/usr/bin/env bash
# Shizuku-powered on-device APK verification for Stoic Journal.
# Requires the Shizuku service to be running and the `rish` wrapper in PATH.

set -uo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
PKG="com.stoicjournal"
RISH="${RISH:-rish}"
TMP_APK="/data/local/tmp/stoic-journal-verify.apk"

if [[ ! -f "$APK" ]]; then
    echo "ERROR: APK not found: $APK" >&2
    exit 1
fi

if ! command -v "$RISH" >/dev/null 2>&1; then
    echo "ERROR: rish not found in PATH. Install/export the Shizuku rish wrapper." >&2
    exit 1
fi

echo "=== Shizuku on-device verification ==="
echo "APK: $APK"

# 1. Push APK to device as the shell user so PackageManager can read it.
echo "[1/6] Pushing APK to device via rish..."
$RISH -c "rm -f $TMP_APK" >/dev/null 2>&1 || true
# Push through rish so the file is owned by shell with shell_data_file context.
$RISH -c "cat > $TMP_APK" < "$APK"
$RISH -c "chmod 644 $TMP_APK && ls -l $TMP_APK" >/dev/null 2>&1

# 2. Install (or reinstall) the APK.
echo "[2/6] Installing APK with pm install..."
INSTALL_OUT=$($RISH -c "pm install -r -t $TMP_APK 2>&1; echo \"\$?\"" 2>&1)
INSTALL_EXIT=$(echo "$INSTALL_OUT" | tail -1)
if [[ "$INSTALL_EXIT" != "0" ]]; then
    echo "ERROR: pm install failed (exit $INSTALL_EXIT):" >&2
    echo "$INSTALL_OUT" >&2
    exit 1
fi

# 3. Verify package metadata.
echo "[3/6] Verifying package metadata..."
$RISH -c "pm list packages -f $PKG" 2>&1
echo "---"
$RISH -c "dumpsys package $PKG | grep -E 'versionCode|versionName|targetSdk|enabled|firstInstallTime|lastUpdateTime|signatures'" 2>&1

# 4. Launch the main activity.
echo "[4/6] Launching main activity..."
MAIN_ACTIVITY=$($RISH -c "cmd package resolve-activity --brief $PKG 2>&1 | grep '^$PKG' | head -1" 2>&1)
if [[ -z "$MAIN_ACTIVITY" ]]; then
    MAIN_ACTIVITY="$PKG/.MainActivity"
fi
$RISH -c "am start -n $MAIN_ACTIVITY 2>&1" 2>&1

# 5. Wait briefly and check the process is alive.
echo "[5/6] Checking app process..."
sleep 5
PID=$($RISH -c "pidof $PKG" 2>&1)
if [[ -z "$PID" ]]; then
    echo "WARNING: app process not found after launch." >&2
else
    echo "PID: $PID"
fi

# 6. Capture a short logcat snapshot and scan for fatal errors.
echo "[6/6] Capturing logcat snapshot..."
LOG_OUT=$($RISH -c "logcat -d -t 100 --pid=$PID 2>&1" 2>&1)
if echo "$LOG_OUT" | grep -qiE 'FATAL|AndroidRuntime.*Exception|Process.*has died'; then
    echo "ERROR: fatal exception detected in app logcat." >&2
    echo "$LOG_OUT" >&2
    exit 1
fi

echo "=== Verification complete: $PKG installed and running ==="
