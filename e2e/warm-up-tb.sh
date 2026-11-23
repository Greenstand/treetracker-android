#!/usr/bin/env bash
# Runs one short Test Buddy session before the real ones, when tb's helper app is not on the
# emulator yet (a freshly booted CI emulator).
#
# Why: tb installs its helper (dev.testbuddy.agent) at the start of its first session and at once
# enables the helper's accessibility service, which is what makes a Compose app report screen
# changes. Seconds after boot the system is often still registering the new package when the
# setting is written, and it then drops the setting. tb does not check, so that whole tb process
# runs without the service: every tap waits its full second, typed text is not verified, and
# keys get lost or doubled (E10 "Edit profile name", B4 "Add a note on review"; task 118).
# With the helper already installed, tb's next session only writes the setting, and it stays.
set -euo pipefail
if [[ $# != 2 || $1 != -s ]]; then
  echo "Usage: TB=tb bash e2e/warm-up-tb.sh -s <emulator-serial>" >&2
  exit 2
fi
SERIAL=$2
ADB=${ADB:-adb}
TB=${TB:-tb}
HELPER=dev.testbuddy.agent
cd "$(dirname "$0")/.."
installed() { "$ADB" -s "$SERIAL" shell pm path "$HELPER" 2>/dev/null | grep -q '^package:'; }
if installed; then
  exit 0
fi
echo "tb warm-up: a first tb session, so the next one finds tb's helper installed"
"$TB" test e2e/flows/first-screen.yaml -p android --device "$SERIAL" </dev/null ||
  echo "tb warm-up: the first-screen flow failed; the flows after it will show why" >&2
if ! installed; then
  echo "tb warm-up: tb's helper ($HELPER) is still not installed; typing may be unreliable in this run" >&2
fi
