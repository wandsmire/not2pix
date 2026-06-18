#!/bin/bash
# deploy_to_phone.sh
# Builds, installs, and launches Not2Pix on a connected Android device.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BASE_PACKAGE="com.mirwanda.not2pix"
APP_ACTIVITY=".AndroidLauncher"
APK="out/Not2Pix_debug.apk"

echo "==> Building..."
./build_debug.sh

if ! command -v adb &>/dev/null; then
    echo "ERROR: 'adb' not found."
    exit 1
fi

DEVICE_IDS=($(adb devices | awk '/device$/ {print $1}'))

if [ ${#DEVICE_IDS[@]} -eq 0 ]; then
    echo "ERROR: No device connected."
    exit 1
fi

if [ ! -f "$APK" ]; then
    echo "ERROR: $APK not found."
    exit 1
fi

echo "==> Found ${#DEVICE_IDS[@]} device(s): ${DEVICE_IDS[*]}"

for DEVICE_ID in "${DEVICE_IDS[@]}"; do
    echo "    Installing on $DEVICE_ID..."
    adb -s "$DEVICE_ID" install -r "$APK"
    echo "    Launching..."
    adb -s "$DEVICE_ID" shell am start -n "${BASE_PACKAGE}/${BASE_PACKAGE}${APP_ACTIVITY}"
done

echo "==> Done!"
