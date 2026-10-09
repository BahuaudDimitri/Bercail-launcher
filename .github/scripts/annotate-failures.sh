#!/usr/bin/env bash
# Turns failed tests into GitHub annotations: unlike the logs, annotations are public,
# so a failure can be read without signing in.
set -u

reports=$(find . -path '*/build/*' -name 'TEST-*.xml' 2>/dev/null)
if [ -z "$reports" ]; then
    echo "::error title=Aucun résultat de test::No test report: the failure happened before the tests (build, SDK or emulator)."
    exit 0
fi

failures=$(echo "$reports" | tr '\n' '\0' | xargs -0 awk '
    /<testcase / { if (match($0, /name="[^"]*"/)) name = substr($0, RSTART + 6, RLENGTH - 7) }
    /<failure/ {
        line = $0
        if (match(line, /message="[^"]*"/)) msg = substr(line, RSTART + 9, RLENGTH - 10)
        else { sub(/.*<failure[^>]*>/, "", line); msg = line }
        print "::error title=" name "::" substr(msg, 1, 400)
    }
')

if [ -n "$failures" ]; then
    echo "$failures"
else
    echo "::error title=Échec hors tests::Every test passed: the failure comes from another step (build, SDK or emulator)."
fi
