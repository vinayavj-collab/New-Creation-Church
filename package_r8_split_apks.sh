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

    # Copy split APKs to root and apk_downloads and .build-outputs
    mkdir -p apk_downloads .build-outputs
    cp -fv "$RELEASE_DIR"/*.apk ./ 2>/dev/null || true
    cp -fv "$RELEASE_DIR"/*.apk ./apk_downloads/ 2>/dev/null || true
    cp -fv "$RELEASE_DIR"/*.apk ./.build-outputs/ 2>/dev/null || true

    # Create ZIP archive containing all R8 + Split ABI APKs
    V_MAJOR=$(grep "VERSION_MAJOR" version.properties | cut -d'=' -f2)
    V_MINOR=$(grep "VERSION_MINOR" version.properties | cut -d'=' -f2)
    V_PATCH=$(grep "VERSION_PATCH" version.properties | cut -d'=' -f2)
    ZIP_NAME="NCCK_v${V_MAJOR}.${V_MINOR}.${V_PATCH}_R8_Split_APKs.zip"
    rm -f "$ZIP_NAME" "./apk_downloads/$ZIP_NAME" "./.build-outputs/$ZIP_NAME"
    zip -j "$ZIP_NAME" "$RELEASE_DIR"/*.apk
    cp -fv "$ZIP_NAME" ./apk_downloads/ 2>/dev/null || true
    cp -fv "$ZIP_NAME" ./.build-outputs/ 2>/dev/null || true

    echo "=== Packaging Complete ==="
    ls -lh "$ZIP_NAME"
    echo "Created: $ZIP_NAME in Root and apk_downloads/ Directory!"
else
    echo "Error: $RELEASE_DIR directory not found!"
    exit 1
fi
