#!/usr/bin/env bash
# Wipes and seeds only the named debug emulator, then opens the requested screen.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
ADB=${ADB:-adb}
APP=org.greenstand.android.TreeTracker.debug
FN="$ROOT/.claude/skills/agent-app-functions/scripts/app_fn.sh"
if [[ $# -lt 2 || $# -gt 3 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: bash e2e/setup-device.sh -s <emulator-serial> [dashboard|dev-options|capture]" >&2
  exit 2
fi
SERIAL=$2
SCREEN=${3:-dashboard}
case "$SCREEN" in dashboard|dev-options|capture) ;; *) echo "Unknown screen: $SCREEN" >&2; exit 2 ;; esac

API=$("$ADB" -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')
[[ $API =~ ^[0-9]+$ ]] || { echo "Could not read API level from $SERIAL" >&2; exit 1; }
bash "$FN" -s "$SERIAL" devices
# Keep fixture data on the device, including when testing the real sync worker.
"$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode enable
"$ADB" -s "$SERIAL" shell svc wifi disable
bash "$FN" -s "$SERIAL" reset
if (( API >= 37 )); then
  bash "$FN" -s "$SERIAL" createAccount '{"firstName":"Ada","lastName":"Admin","phone":"5551230001"}'
  bash "$FN" -s "$SERIAL" captureTrees '{"count":2,"wallet":"5551230001","latitude":-3.5,"longitude":37.3,"note":"E2E dev tools"}'
else
  # Same AgentAccountCreator/AgentTreeCapturer, without the API 37 shell transport.
  "$ADB" -s "$SERIAL" shell am start -W -a android.intent.action.VIEW -d treetracker-e2e://setup -p "$APP"
  RESULT=""
  for (( attempt=0; attempt<30; attempt++ )); do
    RESULT=$("$ADB" -s "$SERIAL" shell run-as "$APP" cat files/e2e-setup-result.txt 2>/dev/null | tr -d '\r') || true
    [[ -z $RESULT ]] || break
    sleep 1
  done
  [[ $RESULT == ready ]] || { echo "E2E fixture setup failed: ${RESULT:-timed out}" >&2; exit 1; }
fi
"$ADB" -s "$SERIAL" emu geo fix 37.3 -3.5
"$ADB" -s "$SERIAL" shell am force-stop "$APP"
"$ADB" -s "$SERIAL" shell am start -W -a android.intent.action.VIEW -d "treetracker-e2e://$SCREEN" -p "$APP"
echo "Seeded Ada Admin (5551230001), two trees, and opened $SCREEN on $SERIAL."
