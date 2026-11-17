#!/usr/bin/env bash
# Runs each given flow on its own with the screen recorded, for before/after videos of a fix.
# Writes <out>/<flow>.mp4, the tb report and debug files, and a line per flow in <out>/results.txt.
# A failing flow doesn't stop the run or fail the script: the "before" run of a known-bug flow is
# expected to fail.
# Usage: TB=tb OUT=build/e2e-video bash e2e/record.sh -s emulator-5554 e2e/flows/a.yaml [more...]
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
ADB=${ADB:-adb}
TB=${TB:-tb}
OUT=${OUT:-build/e2e-video}
APP=org.greenstand.android.TreeTracker.debug
if [[ $# -lt 3 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: TB=tb OUT=<dir> bash e2e/record.sh -s <emulator-serial> <flow.yaml>..." >&2
  exit 2
fi
SERIAL=$2
shift 2
mkdir -p "$OUT"
: > "$OUT/results.txt"
command -v ffmpeg >/dev/null || echo "ffmpeg not found: videos longer than 170s keep only the first part" >&2

# Offline, like run.sh: online, the app syncs messages and the dummy CI keys make that crash.
"$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode enable
"$ADB" -s "$SERIAL" shell svc wifi disable

for FLOW in "$@"; do
  NAME=$(basename "$FLOW" .yaml)
  "$ADB" -s "$SERIAL" shell am force-stop "$APP" || true
  "$ADB" -s "$SERIAL" shell 'rm -f /sdcard/rec-stop /sdcard/rec-*.mp4'
  # screenrecord stops at 3 minutes, so record in parts until told to stop.
  "$ADB" -s "$SERIAL" shell 'i=1; while [ ! -f /sdcard/rec-stop ]; do screenrecord --time-limit 170 --bit-rate 4000000 /sdcard/rec-$i.mp4; i=$((i+1)); done' &
  REC=$!
  sleep 2
  if "$TB" test "$ROOT/$FLOW" -p android --device "$SERIAL" --format junit \
      --output "$OUT/$NAME.xml" --debug-output "$OUT/$NAME" </dev/null; then
    RESULT=passed
  else
    RESULT=failed
  fi
  sleep 2
  "$ADB" -s "$SERIAL" shell touch /sdcard/rec-stop
  "$ADB" -s "$SERIAL" shell pkill -INT screenrecord || true
  wait "$REC" 2>/dev/null || true
  sleep 2
  PARTS=()
  for i in 1 2 3 4 5 6; do
    "$ADB" -s "$SERIAL" pull "/sdcard/rec-$i.mp4" "$OUT/$NAME-part-$i.mp4" >/dev/null 2>&1 \
      && PARTS+=("$OUT/$NAME-part-$i.mp4")
  done
  if (( ${#PARTS[@]} == 1 )) || ! command -v ffmpeg >/dev/null; then
    [[ ${#PARTS[@]} -gt 0 ]] && mv "${PARTS[0]}" "$OUT/$NAME.mp4"
  elif (( ${#PARTS[@]} > 1 )); then
    printf "file '%s'\n" "${PARTS[@]##*/}" > "$OUT/$NAME-parts.txt"
    (cd "$OUT" && ffmpeg -loglevel error -f concat -safe 0 -i "$NAME-parts.txt" -c copy "$NAME.mp4")
  fi
  rm -f "$OUT/$NAME"-part-*.mp4 "$OUT/$NAME-parts.txt"
  echo "$NAME $RESULT" | tee -a "$OUT/results.txt"
done
exit 0
