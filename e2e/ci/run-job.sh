#!/usr/bin/env bash
# What flow-runner-job.yml runs on the emulator: the chosen flows (run-flows.sh), then, for a PR
# with validating flows, each of those on the base branch's app and on the PR's app, a verdict
# from the two results (verdict.py), and a GIF beside every video, for the PR comment.
# See docs/testing/ci-flows.md, "Before and after".
#
# Usage: TB=tb OUT=build/flow-results bash e2e/ci/run-job.sh -s emulator-5554
# Env, besides run-flows.sh's own (FLOW_SELECTORS, SCREENSHOTS, RECORD, ANNOTATE, ...):
#   VALIDATE_FLOWS   validating flows (selectors); none: only FLOW_SELECTORS run, as before
#   AFTER_APK        the PR's app (default: the debug APK just built); it must be installed
#                    already when FLOW_SELECTORS is given
#   BEFORE_APK       the base branch's app; missing: the verdicts are "inconclusive", with the
#                    reason from BEFORE_MISSING
# Results: OUT as run-flows.sh leaves it, plus OUT/validate/{before,after}/ (each a run-flows.sh
# folder), OUT/validate/verdict.json and verdict.md.
# Exit status: 1 a chosen flow failed or a validating flow is broken, 3 nothing failed but a
# recording was lost, 2 bad input, else 0. A validating flow failing on the base app is the point,
# not a failure.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
ADB=${ADB:-adb}
OUT=${OUT:-build/flow-results}
APP=org.greenstand.android.TreeTracker.debug
AFTER_APK=${AFTER_APK:-app/build/outputs/apk/debug/app-debug.apk}
BEFORE_APK=${BEFORE_APK:-}
SELECTORS=${FLOW_SELECTORS:-}
VALIDATE=${VALIDATE_FLOWS:-}
if [[ $# -ne 2 || $1 != -s || $2 != emulator-* ]]; then
  echo "Usage: TB=tb OUT=<dir> bash e2e/ci/run-job.sh -s <emulator-serial>" >&2
  exit 2
fi
SERIAL=$2
cd "$ROOT" || exit 2
if [[ -z ${SELECTORS//[ ,]/} && -z ${VALIDATE//[ ,]/} ]]; then
  echo "Give FLOW_SELECTORS or VALIDATE_FLOWS" >&2
  exit 2
fi

FAILED=0 LOST=0
note() {  # $1: an exit status of run-flows.sh that counts
  case $1 in 0) ;; 3) LOST=1 ;; *) FAILED=1 ;; esac
}
if [[ -n ${SELECTORS//[ ,]/} ]]; then
  bash e2e/ci/run-flows.sh -s "$SERIAL"
  CODE=$?
  [[ $CODE == 2 ]] && exit 2
  note $CODE
else
  # Only validating flows: an empty run, so the job's summary and results branch still work.
  rm -rf "$OUT" && mkdir -p "$OUT"
  : > "$OUT/results.tsv"
  python3 e2e/ci/summarize.py "$OUT" > /dev/null
fi

install() {  # a clean install: the two apps may not share a database version
  "$ADB" -s "$SERIAL" uninstall "$APP" > /dev/null 2>&1 || true
  "$ADB" -s "$SERIAL" install -r "$1"
}
if [[ -n ${VALIDATE//[ ,]/} ]]; then
  V="$OUT/validate"
  mkdir -p "$V"
  for SIDE in before after; do
    APK=$AFTER_APK
    [[ $SIDE == before ]] && APK=$BEFORE_APK
    echo "::group::Validating flows on the $SIDE app"
    if [[ -z $APK || ! -f $APK ]]; then
      [[ $SIDE == before ]] && echo "${BEFORE_MISSING:-the base app was not built}" > "$V/before-missing.txt"
      echo "No $SIDE app: ${APK:-not built}"
      echo "::endgroup::"
      continue
    fi
    if ! install "$APK"; then
      [[ $SIDE == before ]] && echo "the base app could not be installed" > "$V/before-missing.txt"
      echo "::endgroup::"
      continue
    fi
    # Always recorded and annotated: the two videos are what the PR comment shows.
    OUT="$V/$SIDE" FLOW_SELECTORS="$VALIDATE" ANNOTATE=${ANNOTATE:-1} RECORD=0 SCREENSHOTS=end \
      bash e2e/ci/run-flows.sh -s "$SERIAL"
    CODE=$?
    echo "::endgroup::"
    [[ $CODE == 2 ]] && exit 2
    # A failing flow is judged by verdict.py; only a lost recording counts here.
    [[ $CODE == 3 ]] && LOST=1
    [[ $CODE == 1 && $(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("recordingsLost", 0))' "$V/$SIDE/summary.json" 2>/dev/null) != 0 ]] && LOST=1
  done
  python3 e2e/ci/verdict.py "$V" || FAILED=1
fi

# A GIF beside each video: a PR comment plays those inline, and only links an MP4.
if command -v ffmpeg > /dev/null; then
  while IFS= read -r -d '' MP4; do
    ffmpeg -loglevel error -y -i "$MP4" -filter_complex \
      "[0:v]fps=8,scale=270:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=64[p];[b][p]paletteuse=dither=bayer" \
      "${MP4%.mp4}.gif" < /dev/null || rm -f "${MP4%.mp4}.gif"
    # Over 8 MB a GIF is no use in a comment.
    if [[ -f ${MP4%.mp4}.gif && $(wc -c < "${MP4%.mp4}.gif") -gt 8000000 ]]; then rm -f "${MP4%.mp4}.gif"; fi
  done < <(find "$OUT" -name '*.mp4' -print0)
fi

(( FAILED )) && exit 1
(( LOST )) && exit 3
exit 0
