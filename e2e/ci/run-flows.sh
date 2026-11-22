#!/usr/bin/env bash
# Runs Test Buddy flows one at a time on a disposable emulator and saves what an agent or person
# needs to check them later: a screenshot after each step, the step log, logcat and, optionally, a
# video. Writes summary.json and summary.md at the top of OUT. Used by
# .github/workflows/flow-runner-job.yml (see docs/testing/ci-flows.md); works locally too.
#
# Flows are picked by selectors: a path (e2e/flows/a.yaml), a name (settings-logout), a glob
# (settings-*) or a tag (tag:settings). Names, globs and tags look in e2e/flows only (not its
# sub-flows), and include known-bug flows that match. A path or name ending in @N runs only the
# flow's first N top-level steps (capture-first-tree@7), as affected.py picks them.
# A failing flow doesn't stop the run. Exit status: 0 all passed, 1 a flow failed, 2 bad input,
# 3 every flow passed but a recording that was asked for was lost.
#
# Usage: TB=tb OUT=build/flow-results bash e2e/ci/run-flows.sh -s emulator-5554 <selector>...
#    or: FLOW_SELECTORS="a b" ... bash e2e/ci/run-flows.sh -s emulator-5554  (no shell globbing)
# Env:
#   SCREENSHOTS=steps|end|none  after each step that can change the screen (default), last only, none
#   RECORD=1                    record a video of each flow (videos/<flow>.mp4)
#   ANNOTATE=1|TIME             record with `tb test --record --annotate[=TIME]` instead: each step
#                               held about a second (or TIME), taps drawn, a caption band naming
#                               the step (videos/<flow>.mp4 and .srt; needs ffmpeg)
#   RECORD_TRIES=N              with ANNOTATE: run a flow again, up to N times in all (default 3),
#                               when tb could not save its recording (it deletes the device's
#                               copy when its one `adb pull` fails, which CI's emulator makes
#                               happen now and then: see docs/testing/ci-flows.md)
#   ONLINE=1                    leave the network on (by default flows run offline, as run.sh does)
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
ADB=${ADB:-adb}
TB=${TB:-tb}
OUT=${OUT:-build/flow-results}
SCREENSHOTS=${SCREENSHOTS:-steps}
RECORD=${RECORD:-0}
ANNOTATE=${ANNOTATE:-}
[[ $ANNOTATE == 0 ]] && ANNOTATE=
# tb records the annotated video itself, so screenrecord stays off.
[[ -n $ANNOTATE ]] && RECORD=0
RECORD_TRIES=${RECORD_TRIES:-3}
[[ $RECORD_TRIES =~ ^[1-9][0-9]*$ ]] || { echo "RECORD_TRIES takes a number from 1" >&2; exit 2; }
ONLINE=${ONLINE:-0}
APP=org.greenstand.android.TreeTracker.debug
if [[ $# == 2 && -n ${FLOW_SELECTORS:-} ]]; then
  set -f
  # shellcheck disable=SC2086 # split on spaces and commas, without expanding globs
  set -- "$1" "$2" ${FLOW_SELECTORS//,/ }
  set +f
fi
if [[ $# -lt 3 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: TB=tb OUT=<dir> bash e2e/ci/run-flows.sh -s <emulator-serial> <flow selector>..." >&2
  exit 2
fi
case "$SCREENSHOTS" in steps|end|none) ;; *) echo "SCREENSHOTS must be steps, end or none" >&2; exit 2 ;; esac
SERIAL=$2
shift 2
cd "$ROOT" || exit 2

# Selectors to flow files, in order, without repeats.
# Tags as affected.py reads them (inline or block list, comments allowed).
has_tag() { python3 e2e/ci/flowfile.py has-tag "$1" "$2"; }
FLOWS=()
# Steps to run per flow, in FLOWS order: empty for the whole flow. (Plain arrays, not
# associative ones, so this runs on macOS's bash 3.2 too.)
CUTS=()
index_of() {  # $1 flow: its index in FLOWS, or nothing
  local i
  for ((i = 0; i < ${#FLOWS[@]}; i++)); do [[ ${FLOWS[$i]} == "$1" ]] && { echo "$i"; return; }; done
}
add() {  # $1 steps (empty: whole), then flows. Whole wins over a cut; of two cuts, the longer.
  local n=$1 f i
  shift
  for f in "$@"; do
    i=$(index_of "$f")
    if [[ -z $i ]]; then
      FLOWS+=("$f")
      CUTS+=("$n")
    elif [[ -z $n ]]; then
      CUTS[$i]=
    elif [[ -n ${CUTS[$i]} ]] && (( n > CUTS[i] )); then
      CUTS[$i]=$n
    fi
  done
}
for SEL in "$@"; do
  N=
  if [[ $SEL == *@* ]]; then
    N=${SEL##*@} SEL=${SEL%@*}
    if [[ ! $N =~ ^[1-9][0-9]*$ ]]; then
      echo "@N takes a step count from 1, without leading zeros: '$SEL@$N'" >&2
      exit 2
    fi
    [[ $SEL == tag:* || $SEL == *[*?[]* ]] && { echo "@N only goes on one flow: '$SEL@$N'" >&2; exit 2; }
  fi
  case "$SEL" in
    tag:*)
      MATCHED=()
      for f in e2e/flows/*.yaml; do has_tag "$f" "${SEL#tag:}" && MATCHED+=("$f"); done ;;
    */*.yaml) MATCHED=("$SEL") ;;
    *)
      shopt -s nullglob
      # A glob in the name is meant to expand.
      # shellcheck disable=SC2206
      MATCHED=(e2e/flows/${SEL%.yaml}.yaml)
      shopt -u nullglob ;;
  esac
  if (( ${#MATCHED[@]} == 0 )) || [[ ! -f ${MATCHED[0]} ]]; then
    echo "No flow matches '$SEL'" >&2
    exit 2
  fi
  add "$N" "${MATCHED[@]}"
done
echo "Flows: ${FLOWS[*]}"

rm -rf "$OUT"
mkdir -p "$OUT"
if [[ $ONLINE != 1 ]]; then
  # Online, the app syncs on launch and the dummy CI keys make that fail (see run.sh).
  "$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode enable
  "$ADB" -s "$SERIAL" shell svc wifi disable
fi
"$ADB" -s "$SERIAL" emu geo fix 37.3 -3.5 >/dev/null || true

# CI's emulator drops its adb connection on one or two of the first `adb pull`s after it boots,
# and then no more (docs/testing/ci-flows.md, "Lost recordings"). tb pulls its recording once and
# deletes the device's copy when that fails, so let those drops happen here, on a file nobody
# needs: pull until 5 in a row work. Running a flow again (RECORD_TRIES) stays as the safety net.
warm_up_pulls() {
  local tmp=/data/local/tmp/flow-warm-up.bin row=0 drops=0 i
  "$ADB" -s "$SERIAL" shell "dd if=/dev/urandom of=$tmp bs=1024 count=1024 2>/dev/null" || return 0
  for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
    if "$ADB" -s "$SERIAL" pull "$tmp" "$OUT/.warm-up.bin" >/dev/null 2>&1; then
      row=$((row + 1))
      (( row >= 5 )) && break
    else
      row=0 drops=$((drops + 1))
      "$ADB" -s "$SERIAL" wait-for-device
    fi
  done
  rm -f "$OUT/.warm-up.bin"
  "$ADB" -s "$SERIAL" shell rm -f "$tmp" || true
  echo "adb pull warm-up: $drops dropped before $row in a row worked"
}
[[ -n $ANNOTATE || $RECORD == 1 ]] && warm_up_pulls

start_recording() {
  REC_DIR=/sdcard/flow-rec
  "$ADB" -s "$SERIAL" shell "rm -rf $REC_DIR; mkdir -p $REC_DIR"
  # screenrecord stops at 3 minutes, so record in parts until told to stop.
  "$ADB" -s "$SERIAL" shell "i=1; while [ ! -f $REC_DIR/stop ]; do screenrecord --time-limit 170 --bit-rate 4000000 $REC_DIR/part-\$i.mp4; i=\$((i+1)); done" &
  REC=$!
  sleep 2
}
# CI's emulator now and then drops its adb connection in the middle of a pull (see
# docs/testing/ci-flows.md, "Lost recordings"): the file is still on the device, so pull again.
pull_video() {  # $1 file on the device, $2 where to save it
  local try
  for try in 1 2 3; do
    "$ADB" -s "$SERIAL" pull "$1" "$2" >/dev/null 2>&1 && return 0
    echo "adb pull $1 failed (try $try)" >&2
    rm -f "$2"
    "$ADB" -s "$SERIAL" wait-for-device
    sleep 1
  done
  return 1
}
stop_recording() {  # $1: flow name
  sleep 1
  "$ADB" -s "$SERIAL" shell touch "$REC_DIR/stop"
  "$ADB" -s "$SERIAL" shell pkill -INT screenrecord || true
  wait "$REC" 2>/dev/null || true
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    "$ADB" -s "$SERIAL" shell pidof screenrecord >/dev/null 2>&1 || break
    sleep 1
  done
  mkdir -p "$OUT/videos"
  local parts=() i
  for i in 1 2 3 4 5 6; do
    "$ADB" -s "$SERIAL" shell "[ -f $REC_DIR/part-$i.mp4 ]" || break
    pull_video "$REC_DIR/part-$i.mp4" "$OUT/videos/$1-part-$i.mp4" && parts+=("$OUT/videos/$1-part-$i.mp4")
  done
  if (( ${#parts[@]} == 1 )) || { (( ${#parts[@]} > 1 )) && ! command -v ffmpeg >/dev/null; }; then
    mv "${parts[0]}" "$OUT/videos/$1.mp4"
  elif (( ${#parts[@]} > 1 )); then
    printf "file '%s'\n" "${parts[@]##*/}" > "$OUT/videos/$1-parts.txt"
    (cd "$OUT/videos" && ffmpeg -loglevel error -f concat -safe 0 -i "$1-parts.txt" -c copy "$1.mp4")
  fi
  rm -f "$OUT/videos/$1"-part-*.mp4 "$OUT/videos/$1-parts.txt"
}

# Runs flow FLOW (NAME, cut to STEPS when set) once: sets STATUS and SECONDS_TAKEN, and leaves
# tb's files in $DIR/tb.
run_once() {
  "$ADB" -s "$SERIAL" shell am force-stop "$APP" || true
  local setup_ok=1 variant
  SETUP_FAILED=0
  # Org-config flows need their org seeded (the variant run-org.sh uses for the flow); dev-tools
  # flows need the seeded user and trees from setup-device.sh.
  variant=$(awk -v f="$NAME.yaml" '$2 == f {print $1}' e2e/run-org.sh)
  if [[ -n $variant ]]; then
    bash e2e/seed-org.sh -s "$SERIAL" "$variant" </dev/null > "$DIR/setup.txt" 2>&1 || setup_ok=0
  elif has_tag "$FLOW" dev-tools; then
    bash e2e/setup-device.sh -s "$SERIAL" </dev/null > "$DIR/setup.txt" 2>&1 || setup_ok=0
  fi

  RUN="$FLOW"
  if [[ -n $STEPS ]]; then
    # Copies go next to the flow, so its runFlow paths still resolve.
    RUN="$(dirname "$FLOW")/.ci-cut-$NAME.yaml"
    python3 e2e/ci/cut_flow.py "$FLOW" "$STEPS" > "$RUN"
  fi
  if [[ $SCREENSHOTS != none ]]; then
    python3 e2e/ci/step_shots.py --mode "$SCREENSHOTS" "$RUN" > "$(dirname "$FLOW")/.ci-$NAME.yaml"
    [[ $RUN != "$FLOW" ]] && rm -f "$RUN"
    RUN="$(dirname "$FLOW")/.ci-$NAME.yaml"
  fi
  local rec_args=()
  if [[ -n $ANNOTATE ]]; then
    rec_args=(--record "--annotate")
    [[ $ANNOTATE != 1 ]] && rec_args=(--record "--annotate=$ANNOTATE")
  fi
  "$ADB" -s "$SERIAL" logcat -b all -c || true
  [[ $RECORD == 1 ]] && start_recording
  local begin
  begin=$(date +%s)
  rm -f "$DIR/error.txt"
  if (( setup_ok == 0 )); then
    STATUS=failed
    SETUP_FAILED=1
    echo "Setup failed: see setup.txt" > "$DIR/error.txt"
  elif "$TB" test "$RUN" -p android --device "$SERIAL" --format junit --output "$DIR/report.xml" \
      --debug-output "$DIR/tb" --flatten-debug-output ${rec_args[@]+"${rec_args[@]}"} </dev/null 2>&1 | tee "$DIR/console.txt"; then
    STATUS=passed
  else
    STATUS=failed
  fi
  SECONDS_TAKEN=$(( $(date +%s) - begin ))
  [[ $RECORD == 1 ]] && stop_recording "$NAME"
  "$ADB" -s "$SERIAL" logcat -d > "$DIR/logcat.txt" 2>/dev/null || true
  [[ $RUN != "$FLOW" ]] && rm -f "$RUN"
  return 0
}
# tb's own words when its recording did not reach the debug folder ("--record: the recording could
# not be saved: ...", "--record: the screen is not recorded: ..."), or nothing.
record_trouble() { grep -a -m1 -o -- '--record: .*' "$DIR/console.txt" 2>/dev/null | tr -d '\r' | tr '\t' ' ' | cut -c1-300; }
tb_recorded() { [[ -n $(find "$DIR/tb" -name 'recording*.mp4' -size +0c 2>/dev/null | head -n 1) ]]; }

FAILED=0
LOST=0
: > "$OUT/results.tsv"
for ((FI = 0; FI < ${#FLOWS[@]}; FI++)); do
  FLOW=${FLOWS[$FI]}
  NAME=$(basename "$FLOW" .yaml)
  DIR="$OUT/flows/$NAME"
  SHOTS="$OUT/screens/$NAME"
  STEPS=${CUTS[$FI]}
  mkdir -p "$DIR" "$SHOTS"
  echo "::group::$NAME"
  TRY=1
  : > "$DIR/attempts.txt"
  while :; do
    run_once
    [[ -z $ANNOTATE || $SETUP_FAILED == 1 ]] && break
    tb_recorded && break
    # No video from tb, and tb has already removed the device's copy: only running the flow
    # again records it again. Each try that lost its video stays on the record.
    WHY=$(record_trouble)
    echo "try $TRY ($STATUS): ${WHY:-no recording}" | tee -a "$DIR/attempts.txt"
    (( TRY >= RECORD_TRIES )) && break
    for f in console logcat; do
      [[ -f $DIR/$f.txt ]] && cp "$DIR/$f.txt" "$DIR/$f-try$TRY.txt"
    done
    TRY=$((TRY + 1))
    echo "Running $NAME again for its recording (try $TRY of $RECORD_TRIES)"
    rm -rf "$DIR/tb" "$DIR/report.xml"
  done
  [[ -s $DIR/attempts.txt ]] || rm -f "$DIR/attempts.txt"
  # The app's own lines (by its uid) are the ones worth reading first; crash.txt only when the
  # app crashed (the emulator's own services crash now and then too).
  APP_UID=$("$ADB" -s "$SERIAL" shell cmd package list packages -U "$APP" | tr -d '\r' | sed -n "s/^package:$APP uid:\([0-9]*\).*/\1/p")
  if [[ -n $APP_UID ]]; then
    "$ADB" -s "$SERIAL" logcat -d --uid="$APP_UID" > "$DIR/logcat-app.txt" 2>/dev/null || true
  fi
  [[ -s $DIR/logcat-app.txt ]] || grep -E 'TreeTracker|greenstand|AndroidRuntime' "$DIR/logcat.txt" > "$DIR/logcat-app.txt" 2>/dev/null || true
  "$ADB" -s "$SERIAL" logcat -d -b crash > "$DIR/crash.txt" 2>/dev/null || true
  grep -q "$APP" "$DIR/crash.txt" 2>/dev/null || rm -f "$DIR/crash.txt"

  # tb writes into a folder named after the flow: the steps' screenshots (takeScreenshot/), its
  # log and, for a failure, the screen it saw then. Move them where the summary points.
  find "$DIR/tb" -path '*/takeScreenshot/*.png' -exec mv {} "$SHOTS/" \; 2>/dev/null
  # tb's own recording (with --annotate): recording.mp4 and its captions, recording.srt, in
  # 3-minute parts (recording-part2.mp4, ...) for a longer flow, joined into one.
  if [[ -n $ANNOTATE ]]; then
    python3 e2e/ci/join_recording.py "$DIR/tb" "$OUT/videos/$NAME" || echo "No recording from tb for $NAME" >&2
  fi
  # Was the video that was asked for saved? "lost" is said out loud: in the summary, the PR
  # comment and the exit status, so a run without its video does not pass for one with it.
  RECORDING= RECORDING_WHY=
  if [[ -n $ANNOTATE || $RECORD == 1 ]]; then
    if [[ -s $OUT/videos/$NAME.mp4 ]]; then
      RECORDING=saved
      # tb kept what the device had when stopping its recording failed.
      RECORDING_WHY=$(record_trouble)
      if [[ $RECORDING_WHY == *"may be incomplete"* ]]; then RECORDING=partial; else RECORDING_WHY=; fi
    else
      RECORDING=lost
      RECORDING_WHY=$(record_trouble)
      [[ $SETUP_FAILED == 1 ]] && RECORDING_WHY="the flow did not run"
      [[ -n $RECORDING_WHY ]] || RECORDING_WHY="no video file was saved"
      LOST=$((LOST + 1))
      echo "::warning::$NAME: the recording was lost ($RECORDING_WHY)"
    fi
  fi
  for f in "$DIR"/tb/*/screenshot*.png; do
    [[ -f $f ]] && mv "$f" "$SHOTS/failure$(basename "$f" .png | sed 's/^screenshot//').png"
  done
  for f in "$DIR"/tb/*/{error.txt,screen*.txt,hierarchy*.json,log.txt,commands.json}; do
    [[ -f $f ]] && mv "$f" "$DIR/"
  done
  find "$DIR/tb" -type d -empty -delete 2>/dev/null
  [[ $STATUS == failed ]] && FAILED=1
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$NAME" "$FLOW" "$STATUS" "$SECONDS_TAKEN" "$STEPS" "$RECORDING" "$TRY" "$RECORDING_WHY" >> "$OUT/results.tsv"
  echo "::endgroup::"
  echo "$NAME $STATUS (${SECONDS_TAKEN}s, $(ls "$SHOTS" | wc -l) screenshots${RECORDING:+, recording $RECORDING})"
done

"$ADB" -s "$SERIAL" shell am force-stop "$APP" || true
if [[ $ONLINE != 1 ]]; then
  "$ADB" -s "$SERIAL" shell cmd connectivity airplane-mode disable || true
  "$ADB" -s "$SERIAL" shell svc wifi enable || true
fi
python3 e2e/ci/summarize.py "$OUT"
(( FAILED )) && exit 1
(( LOST )) && exit 3
exit 0
