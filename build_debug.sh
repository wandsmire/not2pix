#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

echo "Building Not2Pix Debug APK..."
chmod +x ./gradlew
./gradlew :android:assembleDebug

mkdir -p out

APK="$(find . -path '*/outputs/apk/debug/*.apk' 2>/dev/null | head -1)"
if [ -z "$APK" ]; then
    APK="$(find ../Not2Pix_build -path '*/outputs/apk/debug/*.apk' 2>/dev/null | head -1)"
fi

if [ -n "$APK" ]; then
    cp "$APK" out/Not2Pix_debug.apk
    echo "==> out/Not2Pix_debug.apk"
fi

echo "==> Build complete!"
