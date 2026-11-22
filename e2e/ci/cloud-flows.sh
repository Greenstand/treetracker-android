#!/usr/bin/env bash
# Runs flows on CI's emulator from a session without one (a Claude Code cloud session) and brings
# the screenshots and logs back. Needs gh (signed in) and git; see docs/testing/ci-flows.md.
#
#   bash e2e/ci/cloud-flows.sh run [options] <selector>...   start flow-runner.yml, wait, fetch
#   bash e2e/ci/cloud-flows.sh pr <PR> [selector...]         run on a PR via the run-flows label
#   bash e2e/ci/cloud-flows.sh wait <run id>                 wait for a run to finish
#   bash e2e/ci/cloud-flows.sh fetch [--pr N | --affected N | --branch B] [--out DIR]
#                                    (--affected: the annotated videos e2e-affected.yml made)
#
# Selectors: a path, a flow name, a glob (settings-*) or tag:<tag>.
# run options: --ref BRANCH (default: the current branch, which must be pushed), --pr N (names
#   the results; default: the open PR for the branch), --inline FILE (a flow file that isn't
#   committed), --video, --api N, --screenshots steps|end|none, --out DIR, --no-wait,
#   --validate "FLOW..." [--before SHA] (before and after: each flow, recorded and annotated, on
#   the app at SHA, default the merge base with origin/master, and on the branch's app; the
#   verdicts land in OUT/validate/verdict.md).
# Results land in --out (default build/flow-results): read summary.md, then summary.json, which
# lists each flow's screenshots in step order.
#
# `run` needs flow-runner.yml on master (GitHub only dispatches workflows on the default branch);
# `pr` works on any PR whose branch has flow-runner-pr.yml.
set -euo pipefail
REPO=${FLOW_REPO:-Greenstand/treetracker-android}
OUT=build/flow-results
POLL=${FLOW_POLL_SECONDS:-30}

die() { echo "$*" >&2; exit 2; }
now() { date -u +%Y-%m-%dT%H:%M:%SZ; }
branch() { git rev-parse --abbrev-ref HEAD; }
pr_for_branch() {
  gh api "repos/$REPO/pulls?state=open&head=${REPO%%/*}:$1" --jq '.[0].number // empty'
}

wait_run() {  # $1 run id; prints progress, returns 0 when it finished (pass or fail)
  local id=$1 last="" state
  echo "Waiting for https://github.com/$REPO/actions/runs/$id"
  for _ in $(seq 1 $((90 * 60 / POLL))); do
    state=$(gh api "repos/$REPO/actions/runs/$id" --jq '"\(.status) \(.conclusion // "")"')
    if [[ $state != "$last" ]]; then echo "  $(date +%H:%M:%S) $state"; last=$state; fi
    [[ $state == completed* ]] && return 0
    sleep "$POLL"
  done
  die "Timed out waiting for run $id"
}

fetch() {  # $1 branch, $2 expected run id (optional)
  local b=$1 want=${2:-}
  git fetch -q --depth 1 origin "+refs/heads/$b:refs/flow-results/latest" ||
    die "No results branch $b (a fork PR, or the run stopped before the flows: see the run)"
  rm -rf "$OUT" && mkdir -p "$OUT"
  git archive refs/flow-results/latest | tar -x -C "$OUT"
  local got
  got=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("runId") or "")' "$OUT/summary.json")
  [[ -z $want || $got == "$want" ]] || echo "Note: $b holds run $got, not $want (a newer run replaced it?)" >&2
  echo
  if [[ -f $OUT/validate/verdict.md ]]; then cat "$OUT/validate/verdict.md"; echo; fi
  cat "$OUT/summary.md"
  echo
  echo "Results in $OUT: summary.json, screens/<flow>/*.png, flows/<flow>/ (console.txt, logcat-app.txt, error.txt)"
}

cmd_run() {
  local ref="" pr="" inline="" video=false api=35 shots=steps nowait=false validate="" before="" sel=()
  while (($#)); do
    case $1 in
      --ref) ref=$2; shift ;;
      --pr) pr=$2; shift ;;
      --inline) inline=$2; shift ;;
      --video) video=true ;;
      --validate) validate=$2; shift ;;
      --before) before=$2; shift ;;
      --api) api=$2; shift ;;
      --screenshots) shots=$2; shift ;;
      --out) OUT=$2; shift ;;
      --no-wait) nowait=true ;;
      -*) die "Unknown option $1" ;;
      *) sel+=("$1") ;;
    esac
    shift
  done
  [[ ${#sel[@]} -gt 0 || -n $inline || -n $validate ]] || die "Give flow selectors, --inline FILE or --validate FLOWS"
  if [[ -n $validate ]]; then
    before=$(git rev-parse --verify -q "${before:-$(git merge-base origin/master "${ref:-HEAD}")}^{commit}") ||
      die "--before: a commit whose app is the 'before' (default: the merge base with origin/master)"
  fi
  ref=${ref:-$(branch)}
  git ls-remote --exit-code origin "refs/heads/$ref" >/dev/null || die "Push $ref first: CI runs what is on GitHub"
  pr=${pr:-$(pr_for_branch "$ref")}
  local args=(-X POST "repos/$REPO/actions/workflows/flow-runner.yml/dispatches"
    -f "ref=$ref" -f "inputs[flows]=${sel[*]-}" -f "inputs[pr]=$pr" -f "inputs[api_level]=$api"
    -f "inputs[screenshots]=$shots" -F "inputs[record_video]=$video")
  [[ -n $inline ]] && args+=(-f "inputs[inline_flow]=$(cat "$inline")")
  [[ -n $validate ]] && args+=(-f "inputs[validate_flows]=$validate" -f "inputs[before_ref]=$before")
  local t0 id=""
  t0=$(now)
  # return_run_details makes GitHub answer with the new run's id. Where it isn't taken, dispatch
  # without it and find the run.
  if ! id=$(gh api "${args[@]}" -F return_run_details=true --jq '.workflow_run_id // empty'); then
    gh api "${args[@]}" >/dev/null || die "Could not start the run (is flow-runner.yml on master?)"
    id=""
  fi
  if [[ -z $id ]]; then
    for _ in 1 2 3 4 5 6 7 8 9 10 11 12; do
      sleep 5
      id=$(gh api "repos/$REPO/actions/workflows/flow-runner.yml/runs?event=workflow_dispatch&branch=$ref&per_page=5" \
        --jq "[.workflow_runs[] | select(.created_at >= \"$t0\")][0].id // empty")
      [[ -n $id ]] && break
    done
  fi
  [[ -n $id ]] || die "Could not start or find the run (is flow-runner.yml on master?)"
  echo "Started run $id on $ref${pr:+ (PR $pr)}"
  $nowait && { echo "Later: bash $0 wait $id && bash $0 fetch --branch $(results_branch "$pr" "$id")"; return; }
  wait_run "$id"
  fetch "$(results_branch "$pr" "$id")" "$id"
}
results_branch() { if [[ -n $1 ]]; then echo "flow-results/pr-$1"; else echo "flow-results/run-$2"; fi; }

cmd_pr() {
  local pr=${1:-}
  [[ $pr =~ ^[0-9]+$ ]] || die "Usage: cloud-flows.sh pr <PR number> [selector...]"
  shift
  local t0 sha body labeled changed=false
  t0=$(now)
  sha=$(gh api "repos/$REPO/pulls/$pr" --jq .head.sha)
  labeled=$(gh api "repos/$REPO/issues/$pr/labels" --jq '[.[].name] | index("run-flows") != null')
  if (($#)); then
    # Put the selectors on the description's run-flows line, replacing an older one.
    body=$(gh api "repos/$REPO/pulls/$pr" --jq '.body // ""')
    local new
    new=$(printf '%s\n' "$body" | python3 -c '
import re, sys
body, line = sys.stdin.read().rstrip("\n"), "run-flows: " + " ".join(sys.argv[1:])
pat = re.compile(r"^[ \t>*-]*run-flows:.*$", re.M | re.I)
print(pat.sub(line, body, count=1) if pat.search(body) else (body + "\n\n" + line).lstrip())
' "$@")
    if [[ $new != "$body" ]]; then
      gh api -X PATCH "repos/$REPO/pulls/$pr" -f body="$new" >/dev/null
      changed=true
    fi
  fi
  if [[ $labeled == true && $changed == true ]]; then
    echo "Changed PR $pr's run-flows line; the label is on, so that starts a run"
  else
    # Adding the label starts a run; take it off first when it's already on.
    [[ $labeled == true ]] && gh api -X DELETE "repos/$REPO/issues/$pr/labels/run-flows" >/dev/null
    gh api -X POST "repos/$REPO/issues/$pr/labels" -f "labels[]=run-flows" >/dev/null
    echo "Labeled PR $pr run-flows at ${sha:0:7}"
  fi
  # Wait for the runner's runs on this commit since t0 (an edit before the label makes one too).
  local ids=""
  for _ in $(seq 1 24); do
    sleep 5
    ids=$(gh api "repos/$REPO/actions/runs?event=pull_request&head_sha=$sha&per_page=20" \
      --jq "[.workflow_runs[] | select((.path | startswith(\".github/workflows/flow-runner-pr.yml\")) and .created_at >= \"$t0\") | .id] | join(\" \")")
    [[ -n $ids ]] && { sleep 10; break; }
  done
  ids=$(gh api "repos/$REPO/actions/runs?event=pull_request&head_sha=$sha&per_page=20" \
    --jq "[.workflow_runs[] | select((.path | startswith(\".github/workflows/flow-runner-pr.yml\")) and .created_at >= \"$t0\") | .id] | join(\" \")")
  [[ -n $ids ]] || die "No Flow runner (PR) run started: does PR $pr's branch have .github/workflows/flow-runner-pr.yml?"
  local id
  for id in $ids; do wait_run "$id"; done
  fetch "flow-results/pr-$pr"
}

case ${1:-} in
  run) shift; cmd_run "$@" ;;
  pr) shift; cmd_pr "$@" ;;
  wait) [[ ${2:-} =~ ^[0-9]+$ ]] || die "Usage: cloud-flows.sh wait <run id>"; wait_run "$2" ;;
  fetch)
    shift
    b=""
    while (($#)); do
      case $1 in
        --pr) b="flow-results/pr-$2"; shift ;;
        --affected) b="flow-results/affected-pr-$2"; shift ;;
        --branch) b=$2; shift ;;
        --out) OUT=$2; shift ;;
        *) die "Unknown option $1" ;;
      esac
      shift
    done
    if [[ -z $b ]]; then
      p=$(pr_for_branch "$(branch)")
      [[ -n $p ]] || die "Give --pr N or --branch flow-results/..."
      b="flow-results/pr-$p"
    fi
    fetch "$b" ;;
  *) sed -n '2,20p' "$0"; exit 2 ;;
esac
