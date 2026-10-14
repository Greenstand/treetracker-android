#!/usr/bin/env bash
# Calls Tree Tracker's debug-only AppFunctions over adb, and the reset/launch steps around them.
# See ../SKILL.md.
set -euo pipefail

FUNCTIONS_CLASS=org.greenstand.android.TreeTracker.appfunctions.AgentAppFunctions
APP_ID=org.greenstand.android.TreeTracker
MIN_SDK=37
PERMISSIONS="CAMERA ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION ACTIVITY_RECOGNITION POST_NOTIFICATIONS"

usage() {
  cat <<'EOF'
Usage: app_fn.sh <command> [json]

  createAccount ['<json>']  Create an account. The first one is the admin, which skips onboarding.
  captureTrees '<json>'     Save trees, e.g. '{"count": 20}'.
  list                      Show the functions and their parameters.
  reset                     Clear app data and re-grant runtime permissions (a clean install).
  launch                    Force-stop and relaunch the app so it shows new data.

Env: ANDROID_SERIAL picks the device when several are connected.
     TT_PACKAGE overrides the package (default: the installed debug build, else dev).
EOF
}

die() {
  echo "app_fn: $*" >&2
  exit 1
}

find_adb() {
  if command -v adb >/dev/null 2>&1; then
    command -v adb
  elif [ -n "${ANDROID_HOME:-}" ] && [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
    echo "$ANDROID_HOME/platform-tools/adb"
  elif [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    echo "$HOME/Library/Android/sdk/platform-tools/adb"
  else
    die "adb not found. Add platform-tools to PATH or set ANDROID_HOME."
  fi
}

adb_() { "$ADB" "$@"; }

check_device() {
  if [ -z "${ANDROID_SERIAL:-}" ]; then
    local devices
    devices=$(adb_ devices | awk 'NR > 1 && $2 == "device" { print $1 }')
    [ -n "$devices" ] || die "no device connected."
    if [ "$(echo "$devices" | wc -l | tr -d ' ')" -gt 1 ]; then
      echo "app_fn: several devices are connected. Set ANDROID_SERIAL to one with API $MIN_SDK+:" >&2
      for d in $devices; do
        echo "  $d  API $(adb_ -s "$d" shell getprop ro.build.version.sdk | tr -d '\r')" >&2
      done
      exit 1
    fi
  fi
}

check_sdk() {
  local sdk
  sdk=$(adb_ shell getprop ro.build.version.sdk | tr -d '\r')
  [ "$sdk" -ge "$MIN_SDK" ] ||
    die "AppFunctions need Android 17 (API $MIN_SDK) to call over adb; this device is API $sdk. Use an Android 17 emulator, or drive the UI instead."
}

resolve_package() {
  if [ -n "${TT_PACKAGE:-}" ]; then
    echo "$TT_PACKAGE"
    return
  fi
  local installed
  installed=$(adb_ shell pm list packages "$APP_ID" | tr -d '\r')
  for suffix in debug dev; do
    if echo "$installed" | grep -qx "package:$APP_ID.$suffix"; then
      echo "$APP_ID.$suffix"
      return
    fi
  done
  die "no debug or dev build installed. Install one with: ANDROID_SERIAL=<serial> ./gradlew :app:installDebug"
}

call_function() {
  local name=$1 json=$2 output
  case "$json" in
    *"'"*) die "the JSON can't contain an apostrophe: the device shell would split it." ;;
  esac
  # The device shell parses the arguments again, so the JSON travels inside single quotes
  local status=0
  output=$(adb_ shell cmd app_function execute-app-function \
    --package "$PACKAGE" \
    --function "$FUNCTIONS_CLASS#$name" \
    --parameters "'$json'" \
    --brief-yaml 2>&1) || status=$?
  echo "$output"
  case "$output" in
    *"Error executing"* | *Exception*) exit 1 ;;
  esac
  [ "$status" -eq 0 ] || exit "$status"
}

list_functions() {
  local raw
  raw=$(adb_ shell cmd app_function list-app-functions --package "$PACKAGE")
  if ! command -v python3 >/dev/null 2>&1; then
    echo "$raw"
    return
  fi
  echo "$raw" | python3 -c '
import json, sys
data = json.load(sys.stdin)
for entries in data.values():
    for entry in entries:
        for meta in entry.values():
            if not isinstance(meta, dict) or "functionId" not in meta:
                continue
            print(meta["functionId"][0].split("#")[-1])
            print("  " + " ".join(meta.get("description", [""])[0].split()))
            for param in meta.get("parameters", []):
                name = param["name"][0]
                required = "required" if param["isRequired"][0] else "optional"
                description = param.get("description", [""])[0]
                print(f"  - {name} ({required}): {description}")
            print()
'
}

[ $# -ge 1 ] || {
  usage
  exit 1
}
ADB=$(find_adb)
command=$1
default_json='{}'

case "$command" in
  -h | --help | help)
    usage
    ;;
  createAccount | captureTrees)
    check_device
    check_sdk
    PACKAGE=$(resolve_package)
    call_function "$command" "${2:-$default_json}"
    ;;
  list)
    check_device
    check_sdk
    PACKAGE=$(resolve_package)
    list_functions
    ;;
  reset)
    check_device
    PACKAGE=$(resolve_package)
    adb_ shell pm clear "$PACKAGE" >/dev/null
    for permission in $PERMISSIONS; do
      adb_ shell pm grant "$PACKAGE" "android.permission.$permission" 2>/dev/null || true
    done
    echo "Cleared $PACKAGE and granted its runtime permissions."
    ;;
  launch)
    check_device
    PACKAGE=$(resolve_package)
    adb_ shell am force-stop "$PACKAGE"
    adb_ shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    echo "Launched $PACKAGE."
    ;;
  *)
    usage
    exit 1
    ;;
esac
