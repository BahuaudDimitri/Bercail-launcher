#!/usr/bin/env bash
# Measures the energy of Brume on the plugged Pixel 9 with its power rails (Perfetto, android.power).
# Compares Brume still and Brume moving, full screen, from the gallery of the debug build.
#
# Before: install the debug build (./gradlew :app:installDebug), unlock the phone, turn adaptive brightness off,
# set the brightness to the middle, then leave the phone alone: any touch skews the measure.
# Usage: bash scripts/measure-brume.sh [runs] [seconds]
# Needs trace_processor_shell (Perfetto): set TRACE_PROCESSOR to its path.
set -euo pipefail
export MSYS_NO_PATHCONV=1

RUNS="${1:-2}"
SECONDS_PER_MODE="${2:-60}"
MODES=(still playing)
PKG="io.github.bahuauddimitri.bercail.debug"
ACTIVITY="io.github.bahuauddimitri.bercail.GalleryActivity"
ADB="${ADB:-adb}"
TP="${TRACE_PROCESSOR:?Set TRACE_PROCESSOR to the path of trace_processor_shell}"
OUT="build/measures/brume-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

cat > "$OUT/rails.sql" <<'SQL'
select t.name as rail,
       (max(c.value) - min(c.value)) / ((max(c.ts) - min(c.ts)) / 1e9) / 1000.0 as mw
from counter c join counter_track t on c.track_id = t.id
where t.name like 'power.rails.%' or t.name like 'power.%_uws'
group by t.name;
SQL

echo "mode,run,rail,mw" > "$OUT/rails.csv"
for run in $(seq 1 "$RUNS"); do
  for mode in "${MODES[@]}"; do
    echo "Run $run - Brume $mode"
    "$ADB" shell am start -S -n "$PKG/$ACTIVITY" --es brume "$mode" > /dev/null
    sleep 10
    "$ADB" shell dumpsys gfxinfo "$PKG" reset > /dev/null
    printf 'buffers { size_kb: 16384 fill_policy: RING_BUFFER }\ndata_sources { config { name: "android.power" android_power_config { battery_poll_ms: 1000 collect_power_rails: true } } }\nduration_ms: %d\n' \
      "$((SECONDS_PER_MODE * 1000))" | "$ADB" shell perfetto --txt -c - -o "/data/misc/perfetto-traces/brume-$mode-$run.pftrace" 2> /dev/null
    "$ADB" pull "/data/misc/perfetto-traces/brume-$mode-$run.pftrace" "$OUT/$mode-$run.pftrace" > /dev/null
    "$ADB" shell rm "/data/misc/perfetto-traces/brume-$mode-$run.pftrace"
    "$ADB" shell dumpsys gfxinfo "$PKG" > "$OUT/$mode-$run-gfxinfo.txt"
    "$ADB" shell dumpsys display | grep -m1 "mActiveRenderFrameRate" > "$OUT/$mode-$run-display.txt" || true
    "$TP" -q "$OUT/rails.sql" "$OUT/$mode-$run.pftrace" 2> /dev/null | tail -n +2 | tr -d '"' \
      | awk -F, -v m="$mode" -v r="$run" '{ print m "," r "," $1 "," $2 }' >> "$OUT/rails.csv"
    "$ADB" shell input keyevent KEYCODE_HOME
    sleep 5
  done
done

echo
echo "Average power of the whole phone per mode (mW):"
awk -F, 'NR > 1 { total[$1 "," $2] += $4 }
  END {
    for (k in total) { split(k, p, ","); sum[p[1]] += total[k]; n[p[1]]++ }
    for (m in sum) printf "  Brume %-8s %7.1f mW\n", m, sum[m] / n[m]
  }' "$OUT/rails.csv" | sort
echo "Screen refresh rate and janky frames:"
for f in "$OUT"/*-display.txt; do echo "  $(basename "$f" -display.txt): $(cat "$f" | tr -s ' ')"; done
grep -H "Janky frames:" "$OUT"/*-gfxinfo.txt | sed 's|.*/||; s|-gfxinfo.txt:| |' | sed 's/^/  /'
echo "Details: $OUT/rails.csv"
