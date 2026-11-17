#!/usr/bin/env bash
# Use an explicitly named disposable emulator; setup-device.sh clears the debug app.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
if [[ $# != 2 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: TB=tb bash e2e/run.sh -s <emulator-serial>" >&2
  exit 2
fi
SERIAL=$2
ADB=${ADB:-adb}
TB=${TB:-tb}
OUT=${OUT:-build/e2e}
mkdir -p "$OUT"
AIRPLANE=$("$ADB" -s "$SERIAL" shell settings get global airplane_mode_on | tr -d '\r')
WIFI=$("$ADB" -s "$SERIAL" shell settings get global wifi_on | tr -d '\r')
cleanup() {
  "$ADB" -s "$SERIAL" shell am force-stop org.greenstand.android.TreeTracker.debug || true
  if [[ $AIRPLANE == 1 ]]; then
    "$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode enable || true
  else
    "$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode disable || true
  fi
  if [[ $WIFI == 1 ]]; then
    "$ADB" -s "$SERIAL" shell svc wifi enable || true
  else
    "$ADB" -s "$SERIAL" shell svc wifi disable || true
  fi
}
trap cleanup EXIT
# known-bug flows assert the fixed behaviour and fail until their bug is fixed; run them with
# `tb test e2e/flows/ --include-tags known-bug`. org-config flows need a seeded org (run-org.sh).
"$TB" test e2e/flows/ -p android --device "$SERIAL" --exclude-tags dev-tools,org-config,known-bug \
  --format junit --output "$OUT/first-screen.xml" --debug-output "$OUT/first-screen"
TB="$TB" OUT="$OUT" bash e2e/run-org.sh -s "$SERIAL"
bash e2e/setup-device.sh -s "$SERIAL"
"$TB" test e2e/flows/ -p android --device "$SERIAL" --include-tags dev-tools \
  --format junit --output "$OUT/dev-tools.xml" --debug-output "$OUT/dev-tools"
