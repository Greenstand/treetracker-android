# Task 115 - Annotated videos: lost recordings, and a plan for before/after proof

## Problem

The "Affected flows (annotated video)" check (task 114, #1457) was tried on five bug-fix PRs
(#1434, #1435, #1443, #1444, #1451). The videos were not useful:

1. **Lost recordings.** 3 of 6 recordings were lost. The flow passed, tb logged
   `--record: the recording could not be saved: adb pull /sdcard/tb-recording-1.mp4 ... exit status 1`,
   the job was green and the PR comment showed "—" where the video link goes.
2. **Not proof of a fix.** Only the PR's build is recorded, on the nearest screen the changed file
   maps to. Nothing shows the flow failing without the change.
3. **No inline playback.** The comment links an MP4 on a results branch.

This task fixes 1 (part A) and plans 2 and 3 (parts B to D, [plan.md](plan.md)).

## A. Why the recordings were lost

TO BE FILLED FROM THE CI REPRODUCTION.

## Change (part A)

- `e2e/ci/run-flows.sh`: with `ANNOTATE`, a flow whose recording tb could not save runs again, up
  to `RECORD_TRIES` times in all (default 3). tb removes the device's copy when its one `adb pull`
  fails, so a second pull is not possible: only a new run records again. The lost try keeps its
  log (`console-try<N>.txt`, `logcat-try<N>.txt`, `attempts.txt`).
- A video that was asked for and is still missing is **lost**, and said so: `results.tsv` and
  `summary.json` (`recording`: `saved` / `partial` / `lost`, `recordingError`, `tries`,
  `recordingsLost`), a Video column and a line in `summary.md`, a `::warning::` on the run, and
  exit status 3 from `run-flows.sh` when every flow passed but a recording was lost.
- `flow-runner-job.yml`: the job fails for a lost recording (after uploading everything), with its
  own error message; new output `recordings_lost`.
- `e2e-affected.yml`: the PR comment says "N of M recordings lost" at the top, "recording lost" in
  the flow's row instead of "—", and tb's reason under the table.
- `docs/testing/ci-flows.md`: what a lost recording is and what the job does about it.

## Checks

TO BE FILLED.
