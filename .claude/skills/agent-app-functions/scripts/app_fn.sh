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
Usage: app_fn.sh [-s <serial>] <command> [json]

  createAccount ['<json>']  Create an account. The first one is the admin, which skips onboarding.
  captureTrees '<json>'     Save trees, e.g. '{"count": 20}'.
  list                      Show the functions and their parameters.
  reset                     Clear app data and re-grant runtime permissions (a clean install).
  launch                    Force-stop and relaunch the app so it shows new data.
  devices                   Show connected devices and which one the script would use.

Device: -s <serial> or ANDROID_SERIAL chooses one. Otherwise the script picks among Android 17+
(API 37) devices with a debug or dev build installed: emulators first, then the newest Android.
The choice is stable, so repeated calls land on the same device. reset never picks a physical
device on its own, since it clears app data: pass -s for that.
Env: TT_PACKAGE overrides the package (default: the installed debug build, else dev).
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

# Prints the package to use from `pm list packages` output: TT_PACKAGE, else debug, else dev
pick_package() {
  local installed=$1 candidate
  for candidate in ${TT_PACKAGE:-$APP_ID.debug $APP_ID.dev}; do
    if echo "$installed" | grep -qx "package:$candidate"; then
      echo "$candidate"
      return 0
    fi
  done
  return 0
}

# One line per connected device: "<serial> <api> <package, or - when not installed>"
device_table() {
  local serial info sdk package
  for serial in $(adb_ devices | awk 'NR > 1 && $2 == "device" { print $1 }'); do
    info=$(adb_ -s "$serial" shell "getprop ro.build.version.sdk; pm list packages $APP_ID" 2>/dev/null | tr -d '\r') || true
    sdk=$(echo "$info" | awk 'NR == 1')
    package=$(pick_package "$info")
    echo "$serial ${sdk:-0} ${package:--}"
  done
}

# Serials that can run the functions (API 37+ with the build installed), best first: emulators,
# then newest API, then by serial. Every command ranks the same way, so separate calls (reset,
# createAccount, launch) land on the same device.
rank_devices() {
  echo "$TABLE" |
    awk -v min="$MIN_SDK" '$3 != "-" && $2 + 0 >= min { print ($1 ~ /^emulator-/ ? 0 : 1), -$2, $1 }' |
    sort -k1,1n -k2,2n -k3,3 |
    awk '{ print $3 }'
}

print_table() {
  local picked=${1:-} serial sdk package marker
  printf "  %-20s %-4s %s\n" SERIAL API PACKAGE
  echo "$TABLE" | while read -r serial sdk package; do
    marker=""
    [ "$serial" != "$picked" ] || marker="   <- used"
    printf "  %-20s %-4s %s%s\n" "$serial" "$sdk" "$package" "$marker"
  done
}

# Sets ANDROID_SERIAL (unless -s or the environment already did) and PACKAGE.
#   $1: "sdk" when the command calls the functions, so the device must be API 37+
#   $2: "emulator-only" when the command clears data, so it must not auto-pick a physical device
choose_device() {
  local requirement=$1 auto_pick=${2:-any} picked
  if [ -z "${ANDROID_SERIAL:-}" ]; then
    TABLE=$(device_table)
    [ -n "$TABLE" ] || die "no device connected."
    picked=$(rank_devices | awk 'NR == 1')
    if [ -z "$picked" ]; then
      echo "app_fn: no connected device has a debug or dev build on Android 17+ (API $MIN_SDK):" >&2
      print_table >&2
      echo "Install one with: ANDROID_SERIAL=<serial> ./gradlew :app:installDebug" >&2
      exit 1
    fi
    case "$picked" in
      emulator-*) ;;
      *)
        [ "$auto_pick" != emulator-only ] ||
          die "$picked is a physical device and this clears its app data. Pass -s $picked to confirm."
        ;;
    esac
    export ANDROID_SERIAL=$picked
    if [ "$(echo "$TABLE" | wc -l | tr -d ' ')" -gt 1 ]; then
      echo "app_fn: using $picked. Pass -s <serial> to use another (see: app_fn.sh devices)." >&2
    fi
  fi
  [ "$requirement" != sdk ] || check_sdk
  PACKAGE=$(resolve_package)
}

check_sdk() {
  local sdk
  sdk=$(adb_ shell getprop ro.build.version.sdk | tr -d '\r')
  [ "$sdk" -ge "$MIN_SDK" ] ||
    die "AppFunctions need Android 17 (API $MIN_SDK) to call over adb; this device is API $sdk. Use an Android 17 emulator, or drive the UI instead."
}

resolve_package() {
  local package
  package=$(pick_package "$(adb_ shell pm list packages "$APP_ID" | tr -d '\r')")
  [ -n "$package" ] ||
    die "no debug or dev build on $ANDROID_SERIAL. Install one with: ANDROID_SERIAL=$ANDROID_SERIAL ./gradlew :app:installDebug"
  echo "$package"
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

while [ $# -gt 0 ]; do
  case "$1" in
    -s)
      [ $# -ge 2 ] || die "-s needs a device serial."
      export ANDROID_SERIAL=$2
      shift 2
      ;;
    *) break ;;
  esac
done
[ $# -ge 1 ] || {
  usage
  exit 1
}
ADB=$(find_adb)
command=$1
default_json='{}'
TABLE=""

case "$command" in
  -h | --help | help)
    usage
    ;;
  createAccount | captureTrees)
    choose_device sdk
    call_function "$command" "${2:-$default_json}"
    ;;
  list)
    choose_device sdk
    list_functions
    ;;
  reset)
    choose_device any-sdk emulator-only
    adb_ shell pm clear "$PACKAGE" >/dev/null
    for permission in $PERMISSIONS; do
      adb_ shell pm grant "$PACKAGE" "android.permission.$permission" 2>/dev/null || true
    done
    echo "Cleared $PACKAGE and granted its runtime permissions."
    ;;
  launch)
    choose_device any-sdk
    adb_ shell am force-stop "$PACKAGE"
    adb_ shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    echo "Launched $PACKAGE."
    ;;
  devices)
    TABLE=$(device_table)
    [ -n "$TABLE" ] || die "no device connected."
    picked=${ANDROID_SERIAL:-$(rank_devices | awk 'NR == 1')}
    print_table "$picked"
    [ -n "$picked" ] || echo "No device can run the functions: they need a debug or dev build on API $MIN_SDK+."
    ;;
  *)
    usage
    exit 1
    ;;
esac
