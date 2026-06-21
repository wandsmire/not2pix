#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

echo "Building Not2Pix Release AAB..."

# Bump versionCode in android/build.gradle
CURRENT=$(grep 'versionCode ' android/build.gradle | grep -o '[0-9]\+')
NEXT=$((CURRENT + 1))
sed -i "s/versionCode $CURRENT/versionCode $NEXT/" android/build.gradle
echo "versionCode: $CURRENT -> $NEXT"

chmod +x ./gradlew
./gradlew :android:bundleRelease

mkdir -p out
rm -f out/*.aab

AAB="$(find . ../Not2Pix_build -path '*/outputs/bundle/release/*.aab' 2>/dev/null | head -1)"

if [ -n "$AAB" ]; then
    cp "$AAB" out/Not2Pix_release.aab
    echo "==> out/Not2Pix_release.aab"
fi

echo "==> Build complete!"
