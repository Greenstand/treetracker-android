#!/usr/bin/env bash
# Runs the sync flows (tag sync) on one disposable emulator. Each flow changes the data it syncs, so
# the device is reset and seeded (an account and two unsynced trees) with setup-device.sh before
# every flow. The flows swap S3 for FakeObjectStorage themselves through their treetracker-e2e:// link.
# Usage: TB=tb bash e2e/run-sync.sh -s emulator-5584
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TB=${TB:-tb}
OUT=${OUT:-build/e2e}
mkdir -p "$OUT"
if [[ $# -ne 2 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: bash e2e/run-sync.sh -s <emulator-serial>" >&2
  exit 2
fi
SERIAL=$2
FAILED=0
for FLOW in "$ROOT"/e2e/flows/sync-*.yaml; do
  NAME=$(basename "$FLOW" .yaml)
  # Children must not read the loop's stdin.
  bash "$ROOT/e2e/setup-device.sh" -s "$SERIAL" dashboard </dev/null || { FAILED=1; continue; }
  "$TB" test "$FLOW" -p android --device "$SERIAL" --format junit \
    --output "$OUT/$NAME.xml" --debug-output "$OUT/$NAME" </dev/null || FAILED=1
done
exit $FAILED
