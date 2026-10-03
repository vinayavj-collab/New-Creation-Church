#!/bin/bash
set -e

echo "=== Building R8 Minified + ABI Split Release APKs ==="
export ANDROID_HOME=/opt/android/sdk
export ANDROID_SDK_ROOT=/opt/android/sdk

gradle :app:assembleRelease --no-daemon

echo "=== Verifying generated APKs ==="
RELEASE_DIR="app/build/outputs/apk/release"

if [ -d "$RELEASE_DIR" ]; then
    echo "Release APKs found:"
    ls -lh "$RELEASE_DIR"/*.apk

    # Copy split APKs to root directory with clear names
    cp -fv "$RELEASE_DIR"/app-arm64-v8a-release.apk ./app-arm64-v8a-release.apk 2>/dev/null || true
    cp -fv "$RELEASE_DIR"/app-armeabi-v7a-release.apk ./app-armeabi-v7a-release.apk 2>/dev/null || true
    cp -fv "$RELEASE_DIR"/app-x86_64-release.apk ./app-x86_64-release.apk 2>/dev/null || true
    cp -fv "$RELEASE_DIR"/app-universal-release.apk ./app-universal-release.apk 2>/dev/null || true

    # Create ZIP archive containing all R8 + Split ABI APKs
    ZIP_NAME="NCCK_v72.11.0_R8_Split_APKs.zip"
    rm -f "$ZIP_NAME"
    zip -j "$ZIP_NAME" "$RELEASE_DIR"/*.apk

    echo "=== Packaging Complete ==="
    ls -lh "$ZIP_NAME"
    echo "Created: $ZIP_NAME in Root Directory!"
else
    echo "Error: $RELEASE_DIR directory not found!"
    exit 1
fi
