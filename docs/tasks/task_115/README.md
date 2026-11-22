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
- Reproduced without tb (draft PR #1458, a throwaway workflow on the same emulator setup, two
  runs). A plain `adb pull` of a 1.2 MB file fails the same way (exit 1, no output, or one
  progress line), and the host's adb server logs `emulator-5554: connection terminated: read
  failed` at that second:
  - run 1: the 7th pull after boot failed (from `/sdcard`); the other 19 plain pulls, 6 plain
    screenrecord-and-pull rounds and 8 tb recordings after it all worked;
  - run 2: the first two pulls after boot failed (one from `/sdcard`, one from
    `/data/local/tmp`, so it is not the sdcard's FUSE layer); the next 238 pulls, 240
    `adb exec-out cat` copies and 12 tb recordings all worked.
- The trial PRs fit: each lost video was the first or second pull of its emulator session (both
  flows of #1435, the one flow of #1451).
- So each side sees the other hang up: the connection is cut in between, in the emulator's adb
  bridge, on one or two of the first pulls after boot, and (in every run looked at) not after.
  Why the emulator does that was not found. Screenrecord's file size, the 3-minute cap, the stop
  signal and the pull's timing are all fine: the file is complete on the device before the pull
  starts.

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

- `e2e/ci/run-flows.sh`, when a video is asked for:
  - before the first flow, a throwaway file is pulled until 5 pulls in a row work, so the
    emulator's early drops are spent there and not on a recording;
  - with `ANNOTATE`, a flow whose recording tb still could not save runs again, up to
    `RECORD_TRIES` times in all (default 3). The lost try keeps its log (`console-try<N>.txt`,
    `logcat-try<N>.txt`, `attempts.txt`);
  - with `RECORD=1` (plain screenrecord, where the file is still on the device), the pull itself
    is tried three times.
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

- `python3 -m unittest discover -s e2e/ci -p 'test_*.py'`: 43 tests, with stand-in adb and tb for
  a lost-then-saved recording, a recording lost every time (exit 3, summary, warning), the
  warm-up and `RECORD_TRIES=1`.
- In CI on #1459, with a temporary commit (since reverted) whose tb wrapper threw recordings
  away ([run 44990639135](https://github.com/Greenstand/treetracker-android/actions/runs/44990639135)):
  `first-screen` was saved on its second try, `settings-logout` was reported lost after three
  ("1 of 3 recordings lost" and "recording lost (try 3)" in the PR comment, a Video column in
  the summary, the job failed with "Every flow passed, but a recording was lost"), and
  `signup-back` was saved first time.
- In CI on #1459 with the warm-up, four flows touched by a temporary commit (since reverted),
  among them the two that lost their videos on #1435
  ([run 44991989853](https://github.com/Greenstand/treetracker-android/actions/runs/44991989853)):
  all four videos saved on the first try. The warm-up ran (`adb pull warm-up: 0 dropped before 5
  in a row worked`) but that emulator dropped nothing, so **the warm-up absorbing a real drop
  has not been seen in CI**: it rests on the two debug runs, where every drop was among the
  first pulls. The re-run of a flow covers the case where that pattern does not hold.
- Not seen in CI: the `RECORD=1` pull retry firing (unit-tested only), and a `partial` recording.

## Other things found

- "code analysis" and "screenshot tests" failed on #1459 while this was being checked, on
  downloads, not on code: Maven Central answered Gradle with 429 Too Many Requests, and
  Robolectric could not fetch its Android jar. Several pushes close together (each starts every
  workflow) may have set that off.

- GitHub Models was retired on 30 July 2026, which changes part C ([plan.md](plan.md)).
- "test and assemble" failed once on #1458, which changes no app code:
  `DashboardViewModelSyncTest > enqueued work shows sync preparing` and
  `DashboardViewModelTest > observeTreeCounts reacts to Flow changes`, both
  `IllegalStateException at TestMainDispatcher.kt:72`. It passed on the next push of the same PR. Looks like a
  flaky test on master; not looked into here.
