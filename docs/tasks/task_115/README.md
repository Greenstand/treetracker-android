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

Not tb's recording, and not timing against screenrecord: **the emulator's adb connection drops in
the middle of the `adb pull`**, and tb then throws the device's copy away.

Evidence:

- `logcat.txt` of the failed run on #1435 (artifact `flow-results-affected-pr1435-44970826171`),
  both flows: screenrecord finishes cleanly (`MPEG4Writer: final fsync() ... file size 1245030`,
  `ScreenRecord: success`), and 88 ms later, as tb starts its pull, the device logs
  `adbd: host-17: connection terminated: write failed`, `host-17: offline`,
  `destroying transport host-17`; a new transport comes up half a second later. tb's console line
  ends in `exit status 1:` with nothing after it, or with `[  6%] /sdcard/tb-recording-1.mp4`:
  adb died after at most one 64 KB chunk and printed no error.
- Reproduced without tb (draft PR #1458, a throwaway workflow on the same emulator setup): a
  plain `adb pull` of a 1.2 MB file from `/sdcard` failed the same way (exit 1, no output) once
  in the first 10 pulls; the host's adb server log has
  `emulator-5554: connection terminated: read failed` at that second. SECOND RUN RESULTS TO ADD.
- So each side sees the other hang up: the connection is cut in between, in the emulator's adb
  bridge. Screenrecord's file size, the 3-minute cap, the stop signal and the pull's timing are
  all fine (the file is complete on the device before the pull starts).

What tb does with it (Test Buddy v0.6.3, the pinned `447f360`, also its newest commit):
`internal/driver/android/recording.go`, `StopRecording`: one `d.adb.Run(ctx, "pull", remote, local)`
(line 279) with no retry, and a deferred `rm -f /sdcard/tb-recording-*.mp4` (line 261) that runs
whether or not the pull worked. `internal/engine/recording.go`, `recordStop` (line 98) then logs
"the recording could not be saved" and the flow still passes. tb is not the cause of the drop,
but it turns a retryable pull into a lost video. Worth reporting upstream: pull again after
`adb wait-for-device`, and keep the device's file when the pull fails.

Because tb has deleted the file by the time it returns, nothing in this repository can pull it
again: the clean workaround here is to run the flow again.

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
