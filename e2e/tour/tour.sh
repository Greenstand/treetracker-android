#!/usr/bin/env bash
# Walks the debug app screen by screen with tb and saves what `tb look` sees on each one
# (look text, screenshot, raw hierarchy). Each step taps by test tag first and falls back to
# a screen position, so the same tour runs before and after the screens have tags.
# Steps never stop the tour: a failed step is logged and the walk goes on.
set -u
OUT=${OUT:-build/tour}
TB=${TB:-bin/tb}
APP=org.greenstand.android.TreeTracker.debug
mkdir -p "$OUT"
n=0

log() { echo "$*" | tee -a "$OUT/tour.log"; }

snap() {
  n=$((n + 1))
  local f
  f=$(printf '%s/%02d-%s' "$OUT" "$n" "$1")
  "$TB" look > "$f.txt" 2>&1
  "$TB" shot "$f.png" > /dev/null 2>&1
  "$TB" hierarchy --json > "$f.json" 2>&1
  log "=== $n $1"
  cat "$f.txt" | tee -a "$OUT/tour.log"
}

# run 'cmd' ...: tb do, logged; returns tb's status.
run() {
  log "--- tb do $*"
  "$TB" do "$@" >> "$OUT/tour.log" 2>&1
  local rc=$?
  [ $rc -eq 0 ] || log "    (failed: $rc)"
  return $rc
}

# tap ID [POINT|TEXT]: by id, else by the fallback (a "x%,y%" point or a text).
tap() {
  local id=$1 fb=${2:-}
  run "tapOn: {id: \"$id\", retryTapIfNoChange: false}" && return 0
  [ -z "$fb" ] && return 1
  if [[ $fb == *%* ]]; then
    run "tapOn: {point: \"$fb\"}"
  else
    run "tapOn: \"$fb\""
  fi
}

# type ID TEXT [FALLBACK]: focus a field, then type.
type_in() {
  tap "$1" "${3:-}" && run "inputText: \"$2\""
}

source "$(dirname "$0")/steps.sh"
log "tour done: $n screens"
