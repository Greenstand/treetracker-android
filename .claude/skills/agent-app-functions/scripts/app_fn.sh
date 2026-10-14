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

Device: -s <serial> or ANDROID_SERIAL chooses one. Otherwise the script picks an emulator on
Android 17+ (API 37) with a debug or dev build installed, the newest Android first. The choice is
stable, so repeated calls land on the same device. Physical devices are only used when named, and
reset on one needs -s (not just ANDROID_SERIAL), since it clears the app's data. With a named
device, reset and launch work on any Android version.
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

# Emulators that can run the functions (API 37+ with the build installed), best first: newest
# API, then by serial. Physical devices are never picked automatically. Every command ranks the
# same way, so separate calls (reset, createAccount, launch) land on the same device.
rank_devices() {
  echo "$TABLE" |
    awk -v min="$MIN_SDK" '$1 ~ /^emulator-/ && $3 != "-" && $2 + 0 >= min { print -$2, $1 }' |
    sort -k1,1n -k2,2 |
    awk '{ print $2 }'
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
#   $2: "clears-data" when the command wipes app data, so a physical device must be named with -s
choose_device() {
  local requirement=$1 effect=${2:-} picked
  if [ -z "${ANDROID_SERIAL:-}" ]; then
    TABLE=$(device_table)
    [ -n "$TABLE" ] || die "no device connected."
    picked=$(rank_devices | awk 'NR == 1')
    if [ -z "$picked" ]; then
      echo "app_fn: no emulator has a debug or dev build on Android 17+ (API $MIN_SDK):" >&2
      print_table >&2
      echo "Install one with: ANDROID_SERIAL=<serial> ./gradlew :app:installDebug" >&2
      echo "Physical devices are only used when named with -s <serial>." >&2
      exit 1
    fi
    export ANDROID_SERIAL=$picked
    if [ "$(echo "$TABLE" | wc -l | tr -d ' ')" -gt 1 ]; then
      echo "app_fn: using $picked. Pass -s <serial> to use another (see: app_fn.sh devices)." >&2
    fi
  elif [ "$effect" = clears-data ] && [ "$SERIAL_FROM_FLAG" != yes ]; then
    case "$ANDROID_SERIAL" in
      emulator-*) ;;
      *) die "ANDROID_SERIAL is $ANDROID_SERIAL, a physical device, and reset clears its app data. Pass -s $ANDROID_SERIAL to confirm." ;;
    esac
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

# adb joins its arguments and the device shell parses them again, so the JSON has to arrive as
# one single-quoted word: wrapped in single quotes, with each ' in it written as '\''
quote_for_device_shell() {
  printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

# The shell command reports some failures only as text, so check its output as well as its status
is_error_output() {
  printf '%s\n' "$1" | grep -qE '^(Error executing app function|Exception):'
}

call_function() {
  local name=$1 json=$2 output status=0
  output=$(adb_ shell cmd app_function execute-app-function \
    --package "$PACKAGE" \
    --function "$FUNCTIONS_CLASS#$name" \
    --parameters "$(quote_for_device_shell "$json")" \
    --brief-yaml 2>&1) || status=$?
  echo "$output"
  if is_error_output "$output"; then
    exit 1
  fi
  [ "$status" -eq 0 ] || exit "$status"
}

list_functions() {
  local raw
  raw=$(adb_ shell cmd app_function list-app-functions --package "$PACKAGE")
  if command -v python3 >/dev/null 2>&1; then
    echo "$raw" | format_function_list
  else
    echo "$raw"
  fi
}

# Turns list-app-functions JSON (on stdin) into one block per function with its parameters
format_function_list() {
  python3 -c '
import json, sys
data = json.load(sys.stdin)
for entries in data.values():
    for entry in entries:
        for meta in entry.values():
            if not isinstance(meta, dict) or "functionId" not in meta:
                continue
            print(meta["functionId"][0].split("#")[-1])
            description = " ".join(meta.get("description", [""])[0].split())
            if description:
                print("  " + description)
            for param in meta.get("parameters", []):
                line = "  - " + param["name"][0]
                line += " (required)" if param["isRequired"][0] else " (optional)"
                param_description = " ".join(param.get("description", [""])[0].split())
                if param_description:
                    line += ": " + param_description
                print(line)
            print()
'
}

main() {
  SERIAL_FROM_FLAG=no
  while [ $# -gt 0 ]; do
    case "$1" in
      -s)
        [ $# -ge 2 ] || die "-s needs a device serial."
        export ANDROID_SERIAL=$2
        SERIAL_FROM_FLAG=yes
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
  local command=$1 default_json='{}' picked
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
      choose_device any-sdk clears-data
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
      [ -n "$picked" ] ||
        echo "No emulator can run the functions (they need a debug or dev build on API $MIN_SDK+). Name a physical device with -s."
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

# Run only when executed, so the tests can source the functions
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  main "$@"
fi
