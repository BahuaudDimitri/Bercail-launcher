#!/usr/bin/env bash
# A device job is green only if its tests ran. When the APK cannot be installed (an emulator short on disk,
# for instance), Gradle logs the error, reports zero tests and still succeeds: this refuses that green.
set -u

status=0
for module in $(find . -type d -path '*/src/androidTest' -not -path '*/build/*' | sed 's|/src/androidTest$||; s|^\./||'); do
    ran=0
    for report in $(find "$module/build/outputs/androidTest-results" -name 'TEST-*.xml' 2>/dev/null); do
        count=$(grep -oE '<testsuite [^>]*' "$report" | grep -oE ' tests="[0-9]+"' | grep -oE '[0-9]+' | head -1)
        ran=$(( ran + ${count:-0} ))
    done
    if [ "$ran" -eq 0 ]; then
        echo "::error title=Aucun test exécuté::$module: no instrumented test ran on this device. Was the APK installed?"
        status=1
    else
        echo "$module: $ran instrumented tests ran."
    fi
done
exit $status
