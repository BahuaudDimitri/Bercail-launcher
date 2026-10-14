#!/usr/bin/env bash
# Runs the instrumented tests on the emulator that android-emulator-runner just booted. Right after boot, the
# package service of an Android 17 emulator may not answer yet ("Failure calling service package"): Gradle would
# then fail to install the APK, report zero tests and still pass. So the package service is awaited first.
set -eu

for _ in $(seq 1 60); do
    if adb shell pm path android >/dev/null 2>&1; then
        break
    fi
    echo "The package service is not ready yet: waiting."
    sleep 5
done
adb shell pm path android >/dev/null

./gradlew connectedDebugAndroidTest
