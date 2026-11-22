# Task 113 - Reusable flow runner for cloud sessions

## Problem

Claude Code cloud sessions edit the app without an emulator, so each PR that needed to see a flow
run got its own CI script (PR #1424's recorder, the tb screen tour, per-PR checks). None could be
pointed at any flow on any PR, and their results came back as artifacts, which a cloud session
can't download (the artifact storage host is outside its network).

## Change

- `.github/workflows/flow-runner.yml`: one reusable workflow (`workflow_call`, `workflow_dispatch`).
  Inputs: flow selectors or an inline flow, ref, PR number, API level, screenshots
  (steps/end/none), video. Builds the debug app and tb (tb cached by its commit), boots the API 35
  emulator from android-e2e.yml's snapshot cache, runs the flows, writes the job summary, uploads
  the `flow-results-pr<N>-<run id>` artifact and pushes the same folder (no full logcat) to the `flow-results/pr-<N>` branch, which a session can `git fetch`.
- `.github/workflows/flow-runner-pr.yml`: on PRs, the `run-flows` label (flows from a `run-flows:`
  line in the description, else the PR's changed flows) and a `/run-flows` comment; one summary
  comment per PR. A PR that changes the runner runs first-screen and settings-logout.
- `e2e/ci/run-flows.sh`: runs each flow alone (seeding org-config and dev-tools flows as `run.sh`
  does), with per-flow logcat, crash buffer and optional video.
- `e2e/ci/step_shots.py`: a copy of the flow with a `takeScreenshot` after every top-level step
  that can change the screen, and at the end. Only column-0 `- ` lines are touched, so comments
  and nested commands stay as written; checked against every flow in `e2e/flows/` (same commands
  once the added screenshots are removed).
- `e2e/ci/summarize.py`: `summary.json` (each flow's status, error and screenshots in step order)
  and `summary.md`.
- `e2e/ci/cloud-flows.sh`: `run`, `pr`, `wait`, `fetch` for a session.
- `docs/testing/ci-flows.md`.

## Checked

- `run-flows.sh` with stub `adb` and `tb`: selectors (names, globs, tags, paths), results layout,
  summary, failure files.
- actionlint and shellcheck clean.
- On CI: the PR's own smoke run (first-screen, settings-logout).
