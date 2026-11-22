#!/usr/bin/env bash
# TEMPORARY (verification only, removed before review): the real tb, but the recording of
# first-screen is thrown away on its first run and settings-logout's on every run, as tb does
# when its adb pull fails.
REAL=$GITHUB_WORKSPACE/bin/tb
OUT=
ARGS=("$@")
while (($#)); do [[ $1 == --debug-output ]] && OUT=$2; shift; done
"$REAL" "${ARGS[@]}"
CODE=$?
lose() {
  find "$OUT" -name 'recording*' -delete
  echo "    log: --record: the recording could not be saved: adb pull /sdcard/tb-recording-1.mp4 (SIMULATED by tmp-lossy-tb.sh): exit status 1: "
}
case "$OUT" in
  */first-screen/tb) [[ -f /tmp/lossy-first ]] || { touch /tmp/lossy-first; lose; } ;;
  */settings-logout/tb) lose ;;
esac
exit $CODE
