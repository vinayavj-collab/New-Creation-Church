#!/bin/bash
set -e

echo "Building ABI split APKs (arm64-v8a, armeabi-v7a, universal)..."
gradle :app:assembleRelease -PenableAbiSplits=true

echo "Copying ABI APKs to directory..."
mkdir -p /app/applet/.build-outputs

if [ -d "app/build/outputs/apk/release" ]; then
    cp -fv app/build/outputs/apk/release/*.apk /app/ 2>/dev/null || true
    cp -fv app/build/outputs/apk/release/*.apk /app/applet/ 2>/dev/null || true
    cp -fv app/build/outputs/apk/release/*.apk /app/applet/.build-outputs/ 2>/dev/null || true
fi

echo "ABI APKs successfully created and updated in directory!"
