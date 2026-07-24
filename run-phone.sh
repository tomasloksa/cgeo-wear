#!/usr/bin/env bash
# Build, install and (optionally) trigger the c:geo wear bridge on a phone over ADB.
# Remembers the phone's serial in .phone-serial so you don't have to pick it each
# time; re-detects automatically if that device is no longer connected.
#   ./run-phone.sh              install + fire a test geo: intent + tail logs
#   ./run-phone.sh --no-trigger install + tail logs, no test intent
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-17.0.19+10}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
SERIAL_FILE=".phone-serial"

APP_ID="io.github.tomasloksa.cgeowear"
NAVIGATE_ACTIVITY="io.github.tomasloksa.cgeowear.bridge.NavigateActivity"
TEST_GEO_URI="geo:49.2308,18.746?q=Test%20Cache"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

online_devices() { "$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'; }

is_connected() { online_devices | grep -qx "$1"; }

is_watch() {
    "$ADB" -s "$1" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r' | grep -q watch
}

detect_phone() {
    local serial
    for serial in $(online_devices); do
        if ! is_watch "$serial"; then
            echo "$serial"
            return
        fi
    done
}

SERIAL=""
if [ -f "$SERIAL_FILE" ]; then
    SERIAL="$(cat "$SERIAL_FILE")"
    if ! is_connected "$SERIAL"; then
        echo "Remembered phone '$SERIAL' is not connected — re-detecting…"
        SERIAL=""
    fi
fi

if [ -z "$SERIAL" ]; then
    SERIAL="$(detect_phone)"
    if [ -z "$SERIAL" ]; then
        echo "No usable phone in adb 'device' state." >&2
        BAD="$("$ADB" devices | awk 'NR>1 && $2!="" && $2!="device" {print}')"
        if [ -n "$BAD" ]; then
            echo >&2
            echo "These devices are connected but not usable:" >&2
            echo "$BAD" >&2
            echo >&2
            echo "  no permissions -> udev/plugdev issue (see README/notes to fix)" >&2
            echo "  unauthorized   -> tap 'Allow USB debugging' on the phone" >&2
            echo "  offline        -> replug, or: $ADB kill-server && $ADB start-server" >&2
        else
            echo "Plug in a phone with USB debugging enabled; check: $ADB devices" >&2
        fi
        exit 1
    fi
    echo "$SERIAL" > "$SERIAL_FILE"
    echo "Remembered phone serial in $SERIAL_FILE"
fi

bold "Phone: $SERIAL"
MODEL="$("$ADB" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
echo "Device: $MODEL"

bold "Building and installing the bridge…"
ANDROID_SERIAL="$SERIAL" ./gradlew :mobile:installDebug