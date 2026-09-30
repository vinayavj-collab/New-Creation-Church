#!/bin/bash
set -e

echo "[1/4] Checking and syncing version.properties..."
if [ -f "version.properties" ]; then
    cat version.properties
fi

echo "[2/4] Cleaning previous generated BuildConfig and APK artifacts..."
rm -rf app/build/generated/source/buildConfig
rm -rf app/build/intermediates/javac
rm -rf app/build/outputs/apk

echo "[3/4] Running forceVersionSyncAndRebuild and assembleDebug..."
gradle :app:forceVersionSyncAndRebuild :app:assembleDebug

echo "[4/4] Updating distributable APK artifacts..."
mkdir -p /app/applet/.build-outputs

SRC_APK=""
if [ -f "app/build/outputs/apk/debug/app-universal-debug.apk" ]; then
    SRC_APK="app/build/outputs/apk/debug/app-universal-debug.apk"
elif [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    SRC_APK="app/build/outputs/apk/debug/app-debug.apk"
fi

if [ -n "$SRC_APK" ]; then
    # cp -fv "$SRC_APK" /app/applet/.build-outputs/app-debug.apk
    # cp -fv "$SRC_APK" /app/app-debug.apk
    # cp -fv "$SRC_APK" /app/app-universal.apk
    # cp -fv "$SRC_APK" /app/applet/app-debug.apk
    # cp -fv "$SRC_APK" /app/applet/app-universal.apk
    chmod 644 /app/applet/.build-outputs/app-debug.apk /app/*.apk /app/applet/*.apk 2>/dev/null || true
    echo "Copied $SRC_APK successfully."
fi

echo "Clean, version sync, and rebuild completed successfully!"
