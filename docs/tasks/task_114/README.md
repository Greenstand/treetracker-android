# Task 114 - Annotated videos of the flows a PR affects

## Problem

A PR that changes a screen (a button moved, a color changed) still passes every flow, so a
reviewer never sees what changed. Recording every flow on every PR would be slow and mostly
noise: nearly every flow passes the Dashboard.

## Decisions

- Flows are picked from the test tag ids flows and screens share, with a small overrides file
  (weighed against a hand-kept map of paths to flows, and nightly runtime coverage).
- Changed flows run whole; any other change gets one flow, cut soon after it shows the change.
  At most 6 flows per PR.
- PRs from forks run only from Run workflow, with the full sha of the reviewed commit: read-only
  token, no caches saved, no local action or selector code from the fork.
- No fallback flow when a change reaches no screen: "Android end-to-end" covers it.
- Not a required check for now. "Android end-to-end" keeps running every flow, on Test Buddy
  v0.6.0, without `--annotate`.
- Videos stay on the `flow-results/affected-pr-<N>` branch, so the comment can link them and a
  cloud session can fetch them.

## Change

- `.github/workflows/e2e-affected.yml` (new): `select` runs `e2e/ci/affected.py` on the diff,
  `annotated` / `annotated-fork` run the flows through `flow-runner-job.yml` with `annotate: true`,
  `comment` keeps one PR comment with the reasons and video links.
- `e2e/ci/affected.py`, `affected-overrides.json`, `cut_flow.py`, `join_recording.py`,
  `flowfile.py` (new); `run-flows.sh` gained `name@N` and `ANNOTATE`.
- `flow-runner-job.yml` (new) holds the job, with no permissions of its own; `flow-runner.yml` only
  starts it by hand with a write token, and `flow-runner-pr.yml` calls it directly.
- How it works and how to use it: `docs/testing/ci-flows.md`, "Flows a PR affects".

## Checks

- `python3 -m unittest discover -s e2e/ci -p 'test_*.py'` (selector rules on a made-up app and on
  this repo, `run-flows.sh` selectors with stand-in adb and tb, joining videos, summaries).
- Five review rounds (four for correctness and security, one for cleanup); the fixes are in the
  commit messages.
- `affected.py` on recent merged PRs gives sensible picks (#1441: 3 flows; #1452: `debug-overlays`
  cut at 6), and the same picks before and after the cleanup on 729 inputs.
- tb v0.6.0 `tb check` passes on cut copies of every flow; actionlint is clean.
- Not run here (no emulator in a cloud session): the workflows themselves. The first PR shows
  whether Test Buddy v0.6.0 passes every flow in "Android end-to-end".
- Known limit: only `app/src/main` is indexed; once screens move into KMP modules, `affected.py`
  needs to index them too.
