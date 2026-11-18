#!/usr/bin/env bash
# Runs the org-configured flows (tag org-config) on one disposable emulator. Each flow needs its own
# org in the database, so the device is reset and seeded with seed-org.sh before every flow.
# Usage: TB=tb bash e2e/run-org.sh -s emulator-5584
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TB=${TB:-tb}
OUT=${OUT:-build/e2e}
mkdir -p "$OUT"
if [[ $# -ne 2 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: bash e2e/run-org.sh -s <emulator-serial>" >&2
  exit 2
fi
SERIAL=$2
FAILED=0
while read -r VARIANT FLOW; do
  # Children must not read the loop's stdin.
  bash "$ROOT/e2e/seed-org.sh" -s "$SERIAL" "$VARIANT" </dev/null || { FAILED=1; continue; }
  "$TB" test "$ROOT/e2e/flows/$FLOW" -p android --device "$SERIAL" --format junit \
    --output "$OUT/${FLOW%.yaml}.xml" --debug-output "$OUT/${FLOW%.yaml}" </dev/null || FAILED=1
done <<'EOF'
session-note org-session-note.yaml
tree-height org-tree-height.yaml
force-note org-force-note.yaml
wallet-select org-wallet-select.yaml
session-note org-process-death-session-note.yaml
EOF
exit $FAILED
