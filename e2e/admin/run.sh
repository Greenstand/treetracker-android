#!/usr/bin/env bash
# Runs e2e/admin/capture-upload.yaml: capture a tree on the emulator, upload it, and find it on
# the dev admin panel in headless Chrome.
#
#   MAESTRO_ADMIN_USER=... MAESTRO_ADMIN_PASSWORD=... TB=tb bash e2e/admin/run.sh -s emulator-5554
#
# The admin login is read from the environment only: tb passes MAESTRO_* variables to flows, so
# it never goes on a command line. tb prints and saves what it types, so its console output goes
# through scrub.py, and so does every file it writes under OUT, before anything else reads them.
# The emulator needs the dev build installed and network access; this script turns on Wi-Fi,
# turns off airplane mode and sets a GPS fix.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
if [[ $# != 2 || $1 != -s ]]; then
  echo "Usage: MAESTRO_ADMIN_USER=... MAESTRO_ADMIN_PASSWORD=... TB=tb bash e2e/admin/run.sh -s <serial>" >&2
  exit 2
fi
SERIAL=$2
ADB=${ADB:-adb}
TB=${TB:-tb}
OUT=${OUT:-build/e2e-admin}
APP_ID=org.greenstand.android.TreeTracker.dev

for v in MAESTRO_ADMIN_USER MAESTRO_ADMIN_PASSWORD; do
  if [[ -z ${!v:-} ]]; then
    echo "$v is not set" >&2
    exit 2
  fi
done
# scrub.py cannot mask a value shorter than 3 characters without mangling the rest of the output.
if (( ${#MAESTRO_ADMIN_USER} < 3 || ${#MAESTRO_ADMIN_PASSWORD} < 8 )); then
  echo "The admin user name or password is too short to be masked in tb's output" >&2
  exit 2
fi
export SCRUB_VARS=MAESTRO_ADMIN_USER,MAESTRO_ADMIN_PASSWORD
SCRUB="$ROOT/e2e/admin/scrub.py"

if ! "$ADB" -s "$SERIAL" shell pm path "$APP_ID" >/dev/null 2>&1; then
  echo "$APP_ID is not installed on $SERIAL (./gradlew :app:installDev)" >&2
  exit 2
fi

# The note the phone stamps on the tree and the admin panel must show. Not secret.
NOTE="tb-e2e-${GITHUB_RUN_ID:-local}-$(date +%s)"
echo "Capture note: $NOTE"

rm -rf "$OUT"
mkdir -p "$OUT"
# Runs on every exit, including a failed or interrupted tb run. A file the scrub could not clean
# is deleted and fails the run.
cleanup() {
  "$ADB" -s "$SERIAL" shell am force-stop "$APP_ID" || true
  if ! python3 "$SCRUB" "$OUT"; then
    echo "Removed tb output that still held the admin login" >&2
    exit 1
  fi
}
trap cleanup EXIT

"$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode disable || true
"$ADB" -s "$SERIAL" shell svc wifi enable || true
"$ADB" -s "$SERIAL" emu geo fix 37.3 -3.5

status=0
"$TB" test e2e/admin/capture-upload.yaml --device phone="$SERIAL" --headless -e "NOTE=$NOTE" \
  --format junit --output "$OUT/admin-upload.xml" --debug-output "$OUT/debug" 2>&1 \
  | python3 "$SCRUB" --stdin || status=$?
exit "$status"
