# Task 114 - Annotated videos of the flows a PR affects

## Problem

A PR that changes a screen (a button moved, a color changed) still passes every flow, so a
reviewer never sees what changed. Recording every flow on every PR would be slow and mostly
noise: nearly every flow passes the Dashboard.

## Decisions

Three ways to pick the flows were weighed (a hand-kept map of paths to flows; deriving it from the
test tags flows and screens share; recording runtime coverage nightly). Chosen: derived from test
tags, with a small overrides file. Also decided:

- At most 6 flows per PR.
- PRs from forks run only when a maintainer presses Run workflow (workflow_dispatch with the PR
  number), never on their own.
- No fallback flow when a change reaches no screen: "Android end-to-end" covers it.
- Not a required check for now.
- "Android end-to-end" keeps running every flow, on Test Buddy v0.6.0, without `--annotate`.

## Change

- `.github/workflows/e2e-affected.yml` (new): on PRs, `select` (no emulator) runs
  `e2e/ci/affected.py` on the PR's diff; `annotated` runs the picked flows through
  `flow-runner-job.yml` with `annotate: true`; `comment` keeps one PR comment with why each flow
  ran and links to the videos. A fork's PR runs only by workflow_dispatch, in `annotated-fork`:
  read-only token, no results branch, no caches saved.
- `e2e/ci/affected.py` (new): indexes each flow's steps and the ids they use (through sub-flows),
  and which app files use which test tags (own tags, and tags of components only they use). A
  changed file maps to screen files (itself; for other code the files using it, up to 3 away, not
  through navigation hubs; for resources the files using `R.<type>.<name>`; for `TestTags.kt` the
  files using the changed tags; for a sub-flow the flows running it). Each change gets one flow:
  the one that shows it in the fewest steps, cut a few steps after; a flow already picked is reused
  when it shows the change too. Changed flows run whole. Writes the selectors
  (`e2e/flows/x.yaml@N`), a markdown table and JSON.
- `e2e/ci/join_recording.py` (new): joins tb's 3-minute recording parts and their captions.
- `e2e/ci/affected-overrides.json` (new): ids and "after" anchors for screens built only from
  shared components (capture, image review, selfie, user/wallet/profile select), and ignore globs.
- `e2e/ci/cut_flow.py`, `e2e/ci/flowfile.py` (new): cut a flow after N top-level steps; the step
  splitter `step_shots.py` used, now shared (same output on every flow).
- `e2e/ci/run-flows.sh`: `name@N` selectors; `ANNOTATE=1|TIME` runs `tb test --record --annotate`
  and keeps tb's video and captions as `videos/<flow>.mp4` and `.srt`. `summarize.py`: a cut flow's
  steps and the captions file.
- `.github/workflows/flow-runner.yml` now only holds the triggers and a write token; its job moved to
  `flow-runner-job.yml` (no permissions of its own, so a fork's run can get a read-only token),
  which gained `annotate`, `label` (artifact and branch names: `flow-results/affected-pr-<N>`),
  `save_caches` and a `results_sha` output. The checkout no longer leaves a token in `.git`, and
  the tb and emulator caches are saved before the app's code builds.
- Test Buddy v0.6.0 (`8220d90`) in `android-e2e.yml` and `flow-runner-job.yml`.
- `cloud-flows.sh fetch --affected <N>`.
- `docs/testing/ci-flows.md`: "Flows a PR affects".

## Review fixes

A review of the first version found:

- A fork run by hand could still save caches: the build setup is a local composite action, loaded
  from the PR's own checkout, and its `setup-java` saved the Gradle cache after the build. With
  `save_caches: false`, flow-runner-job.yml now sets up the JDK and dummy keys inline, without a
  cache, and turns off setup-go's cache. A fork's flows are picked by the base branch's copy of
  `affected.py` (`--root`), and its tests aren't run.
- Run workflow took only a PR number, so a push after the review would run unread code. It now
  also takes the reviewed commit and stops when the PR's head differs.
- tb splits recordings over 3 minutes into `recording-part2.mp4`, ...; only the first part was
  kept, sometimes with another part's captions. `e2e/ci/join_recording.py` joins the parts with
  ffmpeg and shifts each part's captions.
- The comment could pass GitHub's 65536-character limit on a huge PR: lists are capped at 50 and
  the body at 60000 characters.
- `TestTags.X` matched as a prefix (`SYNC_OVERLAY` in `SYNC_OVERLAY_SWITCH`); now whole words.
- `git diff -z` for paths with unusual characters; deleted, renamed-only and unknown flows, and
  values files with no names in the diff, are listed instead of dropped; any flow in a folder
  under e2e/flows counts as a sub-flow; block-style `tags:` lists are read.

Kept: videos stay on the `flow-results/affected-pr-<N>` branch, so the comment can link them and a
cloud session can fetch them, at the cost of a heavier branch.

## Checks

- `python3 -m unittest discover -s e2e/ci -p 'test_*.py'`: 27 tests (made-up app: screen,
  ViewModel, two hops, resources, TestTags, sub-flow, shared component, override, limit, known-bug;
  and this repo: a Dashboard change picks one cut flow).
- `affected.py` on recent merged PRs: #1441 → 3 flows (session note whole, wallet select cut at
  16, add org cut at 6); #1439 → 2; #1433 → 1; #1452 → `debug-overlays` cut at 6.
- `run-flows.sh` with stand-in `adb` and `tb`: cut copies, a cut and whole pick of one flow run
  whole, tb's video and captions moved, copies removed.
- tb v0.6.0 built locally: `tb check` passes on cut copies of every flow (3 cut points each), and
  `tb test ... --record --annotate` parses.
- actionlint on the changed workflows: clean.
- Not run here: the workflows themselves (no emulator in a cloud session). The first PR run shows
  whether v0.6.0 passes every flow in "Android end-to-end".
