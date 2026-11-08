#!/usr/bin/env bash
# Tests for app_fn.sh: how it parses adb output, ranks devices, quotes JSON for the device shell,
# spots errors and formats the function list. A fake adb stands in for devices, so nothing real is
# touched. Run: bash .claude/skills/agent-app-functions/scripts/test_app_fn.sh
set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
SCRIPT=$HERE/app_fn.sh
TESTDATA=$HERE/testdata
WORK=$(mktemp -d "${TMPDIR:-/tmp}/app_fn_test.XXXXXX")
trap 'rm -rf "$WORK"' EXIT

P=org.greenstand.android.TreeTracker
DEBUG=$P.debug
DEV=$P.dev
PASSED=0
FAILED=0

pass() { PASSED=$((PASSED + 1)); }
fail() {
  FAILED=$((FAILED + 1))
  printf 'FAIL: %s\n' "$1"
  [ $# -lt 2 ] || printf '%s\n' "$2" | sed 's/^/    /'
}

check() { # name expected actual
  if [ "$2" = "$3" ]; then pass; else fail "$1" "expected: [$2]
actual:   [$3]"; fi
}

check_contains() { # name needle haystack
  case "$3" in
    *"$2"*) pass ;;
    *) fail "$1" "expected to contain: [$2]
actual: [$3]" ;;
  esac
}

check_not_contains() { # name needle haystack
  case "$3" in
    *"$2"*) fail "$1" "expected not to contain: [$2]
actual: [$3]" ;;
    *) pass ;;
  esac
}

# --- Fake adb ----------------------------------------------------------------------------------
# FAKE_DEVICES: "serial:state:api:package,package ..." ("-" for no packages)
# FAKE_CRLF=1 ends shell output lines with \r\n, as some devices do
# FAKE_NOISE=1 prints the daemon start-up lines before the device list
# FAKE_EXEC_OUTPUT / FAKE_EXEC_STATUS: what execute-app-function prints and exits with
# FAKE_LIST_FILE: what list-app-functions prints
# FAKE_LOG: every shell command is logged as "<serial> <command>", parameters as "PARAMS <json>"
mkdir -p "$WORK/bin"
cat >"$WORK/bin/adb" <<'FAKE'
#!/usr/bin/env bash
serial=${ANDROID_SERIAL:-}
if [ "${1:-}" = -s ]; then serial=$2; shift 2; fi

out() {
  if [ "${FAKE_CRLF:-0}" = 1 ]; then printf '%s\r\n' "$@"; else printf '%s\n' "$@"; fi
}

if [ "${1:-}" = devices ]; then
  if [ "${FAKE_NOISE:-0}" = 1 ]; then
    echo "* daemon not running; starting now at tcp:5037"
    echo "* daemon started successfully"
  fi
  echo "List of devices attached"
  for d in ${FAKE_DEVICES:-}; do
    IFS=: read -r s state _ _ <<<"$d"
    printf '%s\t%s\n' "$s" "$state"
  done
  echo
  exit 0
fi

[ "${1:-}" = shell ] || { echo "fake adb: unsupported: $*" >&2; exit 2; }
shift

if [ -z "$serial" ]; then
  count=$(echo ${FAKE_DEVICES:-} | wc -w | tr -d ' ')
  if [ "$count" -gt 1 ]; then echo "adb: more than one device/emulator" >&2; exit 1; fi
  serial=${FAKE_DEVICES%%:*}
fi

device=""
for d in ${FAKE_DEVICES:-}; do
  case "$d" in "$serial":*) device=$d ;; esac
done
[ -n "$device" ] || { echo "adb: device '$serial' not found" >&2; exit 1; }
IFS=: read -r _ _ api packages <<<"$device"

joined="$*"
echo "$serial $joined" >>"${FAKE_LOG:-/dev/null}"

list_packages() { # filter
  [ "$packages" = - ] && return 0
  local IFS=,
  for p in $packages; do
    case "$p" in *"$1"*) out "package:$p" ;; esac
  done
}

case "$joined" in
  "getprop ro.build.version.sdk; pm list packages "*)
    out "$api"
    list_packages "${joined##* }"
    ;;
  "getprop ro.build.version.sdk") out "$api" ;;
  "pm list packages "*) list_packages "${joined##* }" ;;
  "cmd app_function execute-app-function"*)
    eval "set -- $joined" # parse the way the device shell does
    while [ $# -gt 0 ]; do
      if [ "$1" = --parameters ]; then printf 'PARAMS %s\n' "$2" >>"${FAKE_LOG:-/dev/null}"; fi
      shift
    done
    printf '%s\n' "${FAKE_EXEC_OUTPUT:-androidAppfunctionsReturnValue:}"
    exit "${FAKE_EXEC_STATUS:-0}"
    ;;
  "cmd app_function list-app-functions"*) cat "${FAKE_LIST_FILE:-/dev/null}" ;;
  "pm clear "* | "pm grant "* | "am force-stop "* | "monkey "*) ;;
  *) echo "fake adb: unsupported shell command: $joined" >&2; exit 2 ;;
esac
FAKE
chmod +x "$WORK/bin/adb"

# Runs app_fn.sh against the fake adb, with a fresh log and no inherited device choice. Set
# DEVICES (and optionally SERIAL_ENV, CRLF, NOISE, EXEC_OUTPUT, EXEC_STATUS, LIST_FILE) first.
# Sets OUT (stdout and stderr) and STATUS.
DEVICES=""
SERIAL_ENV=""
CRLF=0
NOISE=0
EXEC_OUTPUT="androidAppfunctionsReturnValue:"
EXEC_STATUS=0
LIST_FILE=/dev/null
TT=""
run() {
  : >"$WORK/log"
  OUT=$(env -u ANDROID_SERIAL -u TT_PACKAGE PATH="$WORK/bin:$PATH" FAKE_LOG="$WORK/log" \
    FAKE_DEVICES="$DEVICES" FAKE_CRLF="$CRLF" FAKE_NOISE="$NOISE" \
    FAKE_EXEC_OUTPUT="$EXEC_OUTPUT" FAKE_EXEC_STATUS="$EXEC_STATUS" FAKE_LIST_FILE="$LIST_FILE" \
    ${SERIAL_ENV:+ANDROID_SERIAL=$SERIAL_ENV} ${TT:+TT_PACKAGE=$TT} bash "$SCRIPT" "$@" 2>&1)
  STATUS=$?
}

logged_params() { sed -n 's/^PARAMS //p' "$WORK/log"; }

# --- Unit tests: functions sourced from app_fn.sh ---------------------------------------------
# shellcheck source=app_fn.sh
source "$SCRIPT"
set +e
set -uo pipefail
ADB=true

echo "pick_package"
check "debug build" "$DEBUG" "$(pick_package "package:$DEBUG")"
check "dev build" "$DEV" "$(pick_package "package:$DEV")"
check "debug wins over dev" "$DEBUG" "$(pick_package "package:$DEV
package:$DEBUG")"
check "test APK alone is not the app" "" "$(pick_package "package:$DEBUG.test")"
check "test APK next to the app" "$DEBUG" "$(pick_package "package:$DEBUG.test
package:$DEBUG")"
check "similar name is not the app" "" "$(pick_package "package:${DEBUG}ger")"
check "release build has no functions" "" "$(pick_package "package:$P")"
check "beta build has no functions" "" "$(pick_package "package:$P.test")"
check "nothing installed" "" "$(pick_package "")"
check "TT_PACKAGE installed" "com.example.app.debug" "$(TT_PACKAGE=com.example.app.debug pick_package "package:com.example.app.debug")"
check "TT_PACKAGE not installed" "" "$(TT_PACKAGE=com.example.app.debug pick_package "package:$DEBUG")"
check "TT_PACKAGE dev wins over installed debug" "$DEV" "$(TT_PACKAGE=$DEV pick_package "package:$DEBUG
package:$DEV")"
check "dots match only dots" "" "$(pick_package "package:orgXgreenstandXandroidXTreeTracker.debug")"

echo "rank_devices"
rank() { TABLE=$1 rank_devices | tr '\n' ' ' | sed 's/ $//'; }
check "one ready emulator" "emulator-5554" "$(rank "emulator-5554 37 $DEBUG")"
check "dev build counts" "emulator-5554" "$(rank "emulator-5554 37 $DEV")"
check "API 36 emulator is skipped" "" "$(rank "emulator-5554 36 $DEBUG")"
check "emulator without the build is skipped" "" "$(rank "emulator-5554 37 -")"
check "physical device is never picked" "" "$(rank "R58N12345 37 $DEBUG")"
check "network device is never picked" "" "$(rank "192.168.1.20:5555 37 $DEBUG")"
check "unknown API is skipped" "" "$(rank "emulator-5554 0 $DEBUG")"
check "newest API first" "emulator-5556 emulator-5554" "$(rank "emulator-5554 37 $DEBUG
emulator-5556 38 $DEBUG")"
check "API compared as numbers" "emulator-5556 emulator-5554" "$(rank "emulator-5554 37 $DEBUG
emulator-5556 100 $DEBUG")"
check "tie broken by serial" "emulator-5554 emulator-5556" "$(rank "emulator-5556 37 $DEBUG
emulator-5554 37 $DEBUG")"
check "mixed table" "emulator-5560 emulator-5554 emulator-5558" "$(rank "R58N12345 38 $DEBUG
emulator-5558 37 $DEBUG
emulator-5556 36 $DEBUG
emulator-5560 38 $DEV
emulator-5554 37 $DEBUG
emulator-5562 39 -")"
check "empty table" "" "$(rank "")"

echo "quote_for_device_shell"
# The device shell is POSIX sh. Parsing the quoted word with sh must give back the exact JSON.
roundtrip() { sh -c "printf '%s' $(quote_for_device_shell "$1")"; }
for payload in \
  '{}' \
  '{"count": 20}' \
  '{"firstName": "Mary Ann", "lastName": "Smith"}' \
  "{\"lastName\": \"O'Brien\"}" \
  "{\"note\": \"it's Ada's tree\"}" \
  "{\"note\": \"'''\"}" \
  "'" \
  '{"note": "a  b   c"}' \
  '{"note": "$HOME `id` $(id) ${PATH}"}' \
  '{"note": "back\\slash \"quoted\""}' \
  '{"note": "semi; and && pipe | out > /dev/null < in"}' \
  '{"note": "glob * ? [a]"}' \
  '{"note": "#not a comment"}' \
  '{"firstName": "José", "lastName": "李雷", "note": "😀"}' \
  "{\"note\": \"tab	here\"}" \
  '-n' \
  ''; do
  check "round trip: $payload" "$payload" "$(roundtrip "$payload")"
done
check "newline kept" "line1
line2" "$(roundtrip "line1
line2")"
check "quoted form" "'{\"lastName\": \"O'\\''Brien\"}'" "$(quote_for_device_shell "{\"lastName\": \"O'Brien\"}")"
roundtrip "{\"note\": \"\$(touch $WORK/pwned)'; touch $WORK/pwned2; '\"}" >/dev/null
check "command substitution is not run" "no" "$([ -e "$WORK/pwned" ] && echo yes || echo no)"
check "breaking out of the quotes is not possible" "no" "$([ -e "$WORK/pwned2" ] && echo yes || echo no)"

echo "is_error_output"
errors() { if is_error_output "$1"; then echo error; else echo ok; fi; }
check "function error" error "$(errors "Error executing app function: android.app.appfunctions.AppFunctionException: count must be between 1 and 500, was 0. (code 1001). See logcat for more details.")"
check "JSON parse error" error "$(errors "Exception: org.json.JSONException: Value firstName of type java.lang.String cannot be converted to JSONObject")"
check "error after other lines" error "$(errors "Success
Error executing app function: x")"
check "success" ok "$(errors "androidAppfunctionsReturnValue:
  userId: 1")"
check "name 'Exception' is not an error" ok "$(errors "androidAppfunctionsReturnValue:
  firstName: Exception
  lastName: Handler")"
check "indented error text is a value" ok "$(errors "  note: Error executing app function: nope")"
check "error words mid-line" ok "$(errors "lastName: NullPointerException:")"
check "empty output" ok "$(errors "")"

echo "format_function_list"
if command -v python3 >/dev/null 2>&1; then
  check "real device output" "$(cat "$TESTDATA/list-app-functions.expected.txt")" \
    "$(format_function_list <"$TESTDATA/list-app-functions.json")"
  check "no functions" "" "$(echo '{}' | format_function_list)"
  check "package with no functions" "" "$(echo '{"pkg": []}' | format_function_list)"
  check "function without parameters" "ping
  Checks the app is alive." "$(echo '{"pkg": [{"Meta": {"functionId": ["a.B#ping"], "description": ["Checks the app\n is alive."]}}]}' | format_function_list)"
  check "missing descriptions are left out" "bare
  - x (required)
  - y (optional)" "$(echo '{"pkg": [{"Meta": {"functionId": ["a.B#bare"], "parameters": [{"name": ["x"], "isRequired": [true]}, {"name": ["y"], "isRequired": [false], "description": [""]}]}}]}' | format_function_list)"
  check "functions from several packages" "one
two" "$(echo '{"p1": [{"M": {"functionId": ["a.B#one"]}}], "p2": [{"M": {"functionId": ["c.D#two"]}}]}' | format_function_list | grep -v '^$')"
  check "multi-line parameter description is joined" "f
  - x (optional): first second" "$(echo '{"p": [{"M": {"functionId": ["a#f"], "parameters": [{"name": ["x"], "isRequired": [false], "description": ["first\n   second"]}]}}]}' | format_function_list)"
  check "entries without a function id are skipped" "" "$(echo '{"pkg": [{"Runtime": {"packageName": ["pkg"]}, "Other": "text"}]}' | format_function_list)"
else
  echo "  (skipped: python3 not installed)"
fi

echo "print_table"
TABLE="emulator-5554 37 $DEBUG
R58N12345 34 -"
check "marks the device in use" "  SERIAL               API  PACKAGE
  emulator-5554        37   $DEBUG   <- used
  R58N12345            34   -" "$(print_table emulator-5554)"
check "no mark without a pick" "  SERIAL               API  PACKAGE
  emulator-5554        37   $DEBUG
  R58N12345            34   -" "$(print_table "")"

# --- Command tests: app_fn.sh run against the fake adb ----------------------------------------

echo "reading adb output"
DEVICES="emulator-5554:device:37:$DEBUG,$DEBUG.test"
SERIAL_ENV=""
CRLF=1
NOISE=1
run devices
check "CRLF and daemon noise" "  SERIAL               API  PACKAGE
  emulator-5554        37   $DEBUG   <- used" "$OUT"

DEVICES="emulator-5554:unauthorized:37:$DEBUG emulator-5556:offline:37:$DEBUG emulator-5558:device:37:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run devices
check "unauthorized and offline devices are left out" "  SERIAL               API  PACKAGE
  emulator-5558        37   $DEBUG   <- used" "$OUT"

DEVICES="emulator-5554:device::$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run devices
check_contains "missing API reads as 0" "emulator-5554        0    $DEBUG" "$OUT"
check_contains "and the device is not usable" "No emulator can run the functions" "$OUT"

DEVICES=""
SERIAL_ENV=""
CRLF=0
NOISE=0
run devices
check "no devices" 1 "$STATUS"
check_contains "no devices message" "no device connected" "$OUT"

echo "choosing a device"
DEVICES="emulator-5554:device:37:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run createAccount
check "single device works" 0 "$STATUS"
check_not_contains "no 'using' note for a single device" "app_fn: using" "$OUT"

DEVICES="R58N12345:device:38:$DEBUG emulator-5556:device:36:$DEBUG emulator-5558:device:37:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run createAccount
check "picks the API 37 emulator" "emulator-5558" "$(sed -n 's/ cmd app_function.*//p' "$WORK/log")"
check_contains "says which device it used" "app_fn: using emulator-5558" "$OUT"
run reset
check "reset picks the same device" "emulator-5558 pm clear $DEBUG" "$(grep 'pm clear' "$WORK/log")"
run launch
check_contains "launch picks the same device" "emulator-5558 am force-stop" "$(cat "$WORK/log")"

DEVICES="R58N12345:device:37:$DEBUG emulator-5556:device:36:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run createAccount
check "no emulator: refuses" 1 "$STATUS"
check_contains "no emulator: explains -s" "Physical devices are only used when named with -s" "$OUT"
check "no emulator: nothing ran" "" "$(grep 'cmd app_function' "$WORK/log")"
run -s R58N12345 createAccount
check "-s physical device: runs" 0 "$STATUS"
DEVICES="R58N12345:device:37:$DEBUG emulator-5556:device:36:$DEBUG"
SERIAL_ENV="R58N12345"
CRLF=0
NOISE=0
run createAccount
check "ANDROID_SERIAL physical device: functions run" 0 "$STATUS"
run reset
check "ANDROID_SERIAL physical device: reset refused" 1 "$STATUS"
check_contains "reset refusal names -s" "Pass -s R58N12345 to confirm" "$OUT"
check "reset refused: nothing cleared" "" "$(grep 'pm clear' "$WORK/log")"
DEVICES="R58N12345:device:37:$DEBUG emulator-5556:device:36:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run -s R58N12345 reset
check "-s physical device: reset runs" "R58N12345 pm clear $DEBUG" "$(grep 'pm clear' "$WORK/log")"
run -s emulator-5556 reset
check "-s API 36 emulator: reset runs" 0 "$STATUS"
run -s emulator-5556 createAccount
check "-s API 36 emulator: functions refused" 1 "$STATUS"
check_contains "API message" "this device is API 36" "$OUT"
DEVICES="emulator-5554:device:37:- emulator-5556:device:37:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run -s emulator-5554 createAccount
check "-s device without the build" 1 "$STATUS"
check_contains "install hint" "ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDebug" "$OUT"
DEVICES="emulator-5554:device:37:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run -s emulator-9999 createAccount
check "-s unknown device fails" 1 "$STATUS"

echo "reset safety"
DEVICES="emulator-5554:device:37:$DEBUG emulator-5556:device:37:$DEBUG"
SERIAL_ENV=""
run reset
check "two suitable emulators: reset refuses" 1 "$STATUS"
check_contains "and asks for -s" "several emulators could be reset" "$OUT"
check "and clears nothing" "" "$(grep 'pm clear' "$WORK/log")"
run -s emulator-5556 reset
check "-s picks which emulator to reset" "emulator-5556 pm clear $DEBUG" "$(grep 'pm clear' "$WORK/log")"
run createAccount
check "non-destructive calls still auto-pick" 0 "$STATUS"
DEVICES="emulator-5554:device:37:$P,$DEBUG"
TT=$P
run reset
check "TT_PACKAGE release build: refused" 1 "$STATUS"
check_contains "TT_PACKAGE message" "TT_PACKAGE must be a debug or dev build" "$OUT"
check "TT_PACKAGE release build: nothing cleared" "" "$(grep 'pm clear' "$WORK/log")"
TT=$P.test
run createAccount
check "TT_PACKAGE beta build: refused" 1 "$STATUS"
DEVICES="emulator-5554:device:37:$DEBUG,$DEV"
TT=$DEV
run reset
check "TT_PACKAGE dev build: used" "emulator-5554 pm clear $DEV" "$(grep 'pm clear' "$WORK/log")"
TT=""
run -s "" createAccount
check "-s with an empty serial" 1 "$STATUS"
check_contains "-s empty message" "-s needs a device serial" "$OUT"

echo "arguments"
DEVICES="emulator-5554:device:37:$DEBUG"
SERIAL_ENV=""
CRLF=0
NOISE=0
run
check "no command: usage, exit 1" 1 "$STATUS"
check_contains "no command prints usage" "Usage: app_fn.sh" "$OUT"
run frobnicate
check "unknown command: exit 1" 1 "$STATUS"
run help
check "help: exit 0" 0 "$STATUS"
run -s
check "-s without a serial" 1 "$STATUS"
check_contains "-s message" "-s needs a device serial" "$OUT"
run createAccount
check "createAccount defaults to {}" "{}" "$(logged_params)"
run captureTrees '{"count": 3}'
check "captureTrees passes its JSON" '{"count": 3}' "$(logged_params)"
check_contains "function id" "--function org.greenstand.android.TreeTracker.appfunctions.AgentAppFunctions#captureTrees" "$(cat "$WORK/log")"
check_contains "brief yaml" "--brief-yaml" "$(cat "$WORK/log")"

echo "JSON through adb and the device shell"
for payload in \
  "{\"firstName\": \"Siobhan\", \"lastName\": \"O'Brien\"}" \
  '{"note": "$HOME `id` $(id)"}' \
  '{"note": "a  b; c && d | e"}' \
  '{"firstName": "José", "lastName": "李雷"}' \
  '{"note": "back\\slash \"quoted\""}'; do
  run captureTrees "$payload"
  check "arrives intact: $payload" "$payload" "$(logged_params)"
done

echo "results and errors"
DEVICES="emulator-5554:device:37:$DEBUG"
SERIAL_ENV=""
FAKE_ERROR='Error executing app function: android.app.appfunctions.AppFunctionException: count must be between 1 and 500, was 0. (code 1001). See logcat for more details.'
EXEC_OUTPUT=$FAKE_ERROR EXEC_STATUS=255 run captureTrees '{"count": 0}'
check "function error: exit 1" 1 "$STATUS"
check "function error: message shown" "$FAKE_ERROR" "$OUT"
EXEC_OUTPUT="Exception: org.json.JSONException: bad" EXEC_STATUS=0 run captureTrees 'nope'
check "parse error with exit 0: still exit 1" 1 "$STATUS"
EXEC_OUTPUT="androidAppfunctionsReturnValue:
  firstName: Exception" run createAccount '{"firstName": "Exception"}'
check "a name like 'Exception' is a success" 0 "$STATUS"
EXEC_OUTPUT="something odd" EXEC_STATUS=3 run createAccount
check "unrecognized failure keeps adb's status" 3 "$STATUS"
check_contains "and shows the output" "something odd" "$OUT"

if command -v python3 >/dev/null 2>&1; then
  LIST_FILE=$TESTDATA/list-app-functions.json run list
  check "list command" "$(cat "$TESTDATA/list-app-functions.expected.txt")" "$OUT"
fi

echo
echo "$PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
