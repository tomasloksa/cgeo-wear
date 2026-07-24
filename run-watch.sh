#!/usr/bin/env bash
# Build, install and launch the cgeo-wear compass on a real Wear OS watch
# over Wi-Fi ADB. First run pairs the watch (one-time); after that it's
# just: ./run-watch.sh
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-17.0.19+10}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

bold() { printf '\033[1m%s\033[0m\n' "$*"; }

online_devices() { "$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'; }

is_watch() {
    "$ADB" -s "$1" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r' | grep -q watch
}

# A watch = a connected device that reports the 'watch' characteristic, so a
# phone on the same adb server is never selected.
watch_serial() {
    local serial
    for serial in $(online_devices); do
        if is_watch "$serial"; then
            echo "$serial"
            return
        fi
    done
}

SERIAL="$(watch_serial)"

if [ -z "$SERIAL" ]; then
    bold "No watch connected yet — one-time setup:"
    echo
    echo "On the watch (must be on the SAME Wi-Fi as this computer):"
    echo "  1. Settings > System > About > tap 'Build number' 7x  (enables developer options)"
    echo "  2. Settings > Developer options > enable 'ADB debugging'"
    echo "  3. Enable 'Wireless debugging' > tap 'Pair new device'"
    echo "     -> the watch shows a PAIRING code + IP:PORT"
    echo
    read -rp "Pairing IP:PORT shown on the watch: " PAIR_ADDR
    read -rp "6-digit pairing code: " PAIR_CODE
    "$ADB" pair "$PAIR_ADDR" "$PAIR_CODE"
    echo
    echo "Now go BACK one screen on the watch, to 'Wireless debugging' itself."
    echo "It shows a second IP:PORT (usually a different port than the pairing one)."
    read -rp "Wireless debugging IP:PORT: " CONNECT_ADDR
    "$ADB" connect "$CONNECT_ADDR"
    SERIAL="$(watch_serial)"
    if [ -z "$SERIAL" ]; then
        echo "Watch did not connect — check both devices are on the same Wi-Fi and retry." >&2
        exit 1
    fi
fi

bold "Watch connected: $SERIAL"
MODEL="$("$ADB" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
echo "Device: $MODEL"

bold "Building and installing…"
ANDROID_SERIAL="$SERIAL" ./gradlew :wear:installDebug

bold "Launching compass…"
"$ADB" -s "$SERIAL" shell am start -n io.github.tomasloksa.cgeowear/.MainActivity

bold "Done — the compass should be on the watch now."
echo "(Next time just run ./run-watch.sh again — pairing is remembered until"
echo " the watch's Wireless debugging is toggled off or its IP changes; if it"
echo " reconnects on a new port, the script will walk you through connect again.)"
