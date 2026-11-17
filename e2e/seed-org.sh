#!/usr/bin/env bash
# Resets the named emulator's debug app, seeds Ada Admin (see setup-device.sh), then adds an org
# with a custom capture config to the Room database and makes it the current org. The app is left
# stopped on the Dashboard route's data, so a flow starts with a plain `launchApp`.
#
# The org config normally arrives from Firebase Remote Config (OrgConfigProvider), which is not
# reachable from an offline emulator, and the org link only carries id and name. Offline, a link
# builds a minimal org, so org-configured variants are written straight to the database.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
ADB=${ADB:-adb}
APP=org.greenstand.android.TreeTracker.debug
if [[ $# -ne 3 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: bash e2e/seed-org.sh -s <emulator-serial> <session-note|tree-height|force-note|wallet-select>" >&2
  exit 2
fi
SERIAL=$2
VARIANT=$3
USER_SELECT='{"route":"user-select"}'
CAPTURE='{"route":"capture/{profilePicUrl}"}'
REVIEW='{"route":"tree-image-review/{photoPath}"}'
case "$VARIANT" in
  session-note) SETUP="$USER_SELECT,{\"route\":\"session-note\"}"; FLOW="$CAPTURE,$REVIEW" ;;
  tree-height) SETUP="$USER_SELECT"; FLOW="$CAPTURE,$REVIEW,{\"route\":\"tree-height-selection\"}" ;;
  force-note) SETUP="$USER_SELECT"; FLOW="$CAPTURE,{\"route\":\"tree-image-review/{photoPath}\",\"features\":[\"forceNote\"]}" ;;
  wallet-select) SETUP="$USER_SELECT,{\"route\":\"wallet-select\"}"; FLOW="$CAPTURE,$REVIEW" ;;
  *) echo "Unknown variant: $VARIANT" >&2; exit 2 ;;
esac
ID="e2e-$VARIANT"

bash "$ROOT/e2e/setup-device.sh" -s "$SERIAL" dashboard
"$ADB" -s "$SERIAL" shell am force-stop "$APP"
SQL=$(mktemp)
cat > "$SQL" <<SQLEOF
INSERT OR REPLACE INTO organization (_id, version, name, wallet_id, capture_setup_flow_json, capture_flow_json)
VALUES ('$ID', 1, 'E2E $VARIANT', '', '[$SETUP]', '[$FLOW]');
SQLEOF
"$ADB" -s "$SERIAL" push "$SQL" /data/local/tmp/e2e-org.sql >/dev/null
rm -f "$SQL"
"$ADB" -s "$SERIAL" shell run-as "$APP" sh -c "'sqlite3 databases/treetracker.v2.db < /data/local/tmp/e2e-org.sql'"
"$ADB" -s "$SERIAL" shell run-as "$APP" sh -c "'sed -i \"s#</map>#<string name=\\\"greenstand/system-settings/current-org\\\">$ID</string></map>#\" shared_prefs/org.greenstand.android.xml'"
"$ADB" -s "$SERIAL" shell rm -f /data/local/tmp/e2e-org.sql
echo "Seeded org '$ID' as the current org on $SERIAL."
