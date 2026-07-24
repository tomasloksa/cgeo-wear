#!/usr/bin/env bash
# Stream logs from the phone bridge and the watch at the same time, colour-tagged
# per device. Auto-detects which connected device is the watch (via the 'watch'
# build characteristic) and which is the phone.
#   ./run-logs.sh          filtered to this project's tags on both devices
#   ./run-logs.sh --all     everything (unfiltered) from both devices
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

PHONE_TAGS=(NavigateActivity:D BridgeService:D)
WATCH_TAGS=(DataLayerNav:D NavListenerSvc:D HeadingProvider:D)

CYAN=$'\033[36m'
GREEN=$'\033[32m'
RESET=$'\033[0m'

ALL=false
[ "${1:-}" = "--all" ] && ALL=true

online_devices() { "$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'; }

is_watch() {
    "$ADB" -s "$1" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r' | grep -q watch
}

WATCH=""
PHONE=""
for serial in $(online_devices); do
    if is_watch "$serial"; then WATCH="$serial"; else PHONE="$serial"; fi
done

stream() {
    local serial="$1" label="$2" color="$3"
    shift 3
    "$ADB" -s "$serial" logcat -c 2>/dev/null || true
    if $ALL; then
        "$ADB" -s "$serial" logcat -v time
    else
        "$ADB" -s "$serial" logcat -s "$@"
    fi | sed -u "s/^/${color}[${label}]${RESET} /"
}

trap 'kill 0' EXIT INT TERM

any=false
if [ -n "$PHONE" ]; then
    echo "${CYAN}PHONE${RESET}: $PHONE"
    stream "$PHONE" PHONE "$CYAN" "${PHONE_TAGS[@]}" &
    any=true
else
    echo "No phone connected." >&2
fi

if [ -n "$WATCH" ]; then
    echo "${GREEN}WATCH${RESET}: $WATCH"
    stream "$WATCH" WATCH "$GREEN" "${WATCH_TAGS[@]}" &
    any=true
else
    echo "No watch connected." >&2
fi

if ! $any; then
    echo "No devices to log." >&2
    exit 1
fi

echo "Streaming — Ctrl-C to stop."
wait
