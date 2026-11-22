# Running flows on CI from a cloud session

Claude Code cloud sessions have no Android emulator. The **Flow runner** workflow runs any Test
Buddy flows (`e2e/flows/`) on CI's emulator for any branch or PR, and brings back a screenshot
after each step, the step log and logcat, for the session (or a person) to look at. It replaces
writing a one-off CI script per PR.

| File | What it does |
| --- | --- |
| `.github/workflows/flow-runner-job.yml` | The reusable workflow (`workflow_call`): builds the debug app, boots the emulator, runs the flows, publishes the results. No permissions of its own: the caller grants `contents: write` to push the results branch, or read-only for a fork's PR. |
| `.github/workflows/flow-runner.yml` | Starts that job by hand (`workflow_dispatch`), with a token that can push the results. |
| `.github/workflows/e2e-affected.yml` | On every PR: annotated videos of the flows the PR affects (below). |
| `e2e/ci/affected.py` | Picks those flows from the PR's diff; `affected-overrides.json` holds its hints. |
| `e2e/ci/cut_flow.py` | Copies a flow cut after its first N steps (`name@N` selectors). |
| `.github/workflows/flow-runner-pr.yml` | Starts it on a PR from the `run-flows` label or a `/run-flows` comment, and keeps one summary comment on the PR up to date. |
| `e2e/ci/run-flows.sh` | Runs the flows on an emulator (CI's or yours) and writes the results folder. |
| `e2e/ci/step_shots.py` | Copies a flow with a `takeScreenshot` after each step. |
| `e2e/ci/summarize.py` | Writes `summary.json` and `summary.md`. |
| `e2e/ci/cloud-flows.sh` | For the session: start a run, wait for it, fetch the results. |

## Picking flows

Selectors, space or comma separated, in any of the ways below:

- `settings-logout`: a flow's name (`e2e/flows/settings-logout.yaml`)
- `settings-*`: a glob of names
- `tag:signup`: every flow with that tag
- `e2e/flows/subflows/signup.yaml`: a path
- `capture-first-tree@7`: only the flow's first 7 top-level steps (a name or path, not a glob or tag)
- a flow that isn't committed: `--inline my-flow.yaml` (run) or the `inline_flow` input. It runs
  as `e2e/flows/inline.yaml`, so `runFlow: {file: subflows/signup.yaml}` works.

Org-config flows get their org seeded and dev-tools flows get `setup-device.sh`'s user and trees,
as in `run.sh`. Flows run offline, as in `run.sh`, and with a GPS fix near the seeded trees.

## From a cloud session

On a PR whose branch has `flow-runner-pr.yml` (works before it reaches master):

```bash
bash e2e/ci/cloud-flows.sh pr 1453 settings-profile-edit settings-logout
```

This puts `run-flows: settings-profile-edit settings-logout` in the PR description, adds the
`run-flows` label, waits for the run and fetches the results to `build/flow-results/`. While the
label is on, every push re-runs the same flows.

Once `flow-runner.yml` is on master, any pushed branch, with or without a PR:

```bash
bash e2e/ci/cloud-flows.sh run settings-logout tag:signup           # the current branch
bash e2e/ci/cloud-flows.sh run --ref my-branch --video --api 34 first-screen
bash e2e/ci/cloud-flows.sh run --inline /tmp/try.yaml                # an uncommitted flow
```

`cloud-flows.sh fetch --pr 1453` fetches the latest results again. `wait <run id>` waits for a run.

Then read `build/flow-results/summary.md`, and for a flow to check, its screenshots in
`summary.json` order (Read shows a PNG). A failed flow also has `screens/<flow>/failure.png`, and
in `flows/<flow>/`: `error.txt`, `screen.txt` (what tb saw on screen when it failed),
`console.txt` (each step, ✓ or ✗), `logcat-app.txt` (the app's own logcat lines) and
`crash.txt` when the app crashed.

Why a branch: the session's network can reach GitHub's API and git, but not the storage that run
artifacts download from. So each run also pushes its results (without the full logcat)
to `flow-results/pr-<N>` (or `flow-results/run-<run id>`), one commit replaced on each run.
Branches nobody updated for 14 days are deleted by the next run.

## From GitHub

- **Label**: add `run-flows` to the PR. Flows come from a `run-flows:` line in the description;
  without one, the flows the PR adds or changes; without those, `first-screen`. Options go on the
  same line: `run-flows: tag:settings --video --api 34 --screenshots end`.
- **Comment**: `/run-flows settings-logout --video` (repo members and collaborators; PRs from this
  repo only; only once `flow-runner-pr.yml` is on master).
- **By hand**: Actions > Flow runner > Run workflow.
- **From another workflow**:

  ```yaml
  jobs:
    flows:
      permissions:
        contents: write
      uses: ./.github/workflows/flow-runner-job.yml
      with:
        flows: settings-logout tag:signup
        pr: ${{ github.event.pull_request.number }}
  ```

A PR that changes the runner itself runs `first-screen` and `settings-logout`, with videos, as a
smoke test.

A video on the results branch can go in a PR description for the "Check for screenshot & video"
check: link it at the results commit (`https://github.com/<repo>/blob/<commit>/videos/<flow>.mp4`),
since the branch moves on with the next run.

## Flows a PR affects (annotated videos)

`e2e-affected.yml` runs on every PR next to "Android end-to-end" (which still runs every flow, without
video). It records each flow it picks with `tb test --record --annotate` (Test Buddy v0.6.3): every
step held about a second, a ring where each tap landed and a band under the video naming the step.
The PR gets one comment, updated on each push, with why each flow ran and a link to each video. It
is not a required check.

What runs (`e2e/ci/affected.py`, at most 6 flows):

1. Flows the PR adds or edits, whole.
2. For each other change a flow can show, **one** flow, cut a few steps after it has shown it. A
   Dashboard change does not run the dozens of flows that pass the Dashboard: it runs the one that gets
   there in the fewest steps, and stops it there. A flow already picked is reused when it shows the
   change too.

How a change is traced to a flow, through the `TestTags` ids flows and screens share:

| Change | Shown by |
|---|---|
| A screen file (uses test tags itself, or a tagged component only it uses) | the flow steps that use its ids |
| Other app code (ViewModel, use case, repository) | the screens that use it, up to 3 files away |
| A resource (`res/values*/` entries changed, drawables, ...) | the screens whose code uses `R.<type>.<name>` |
| `TestTags.kt` | the screens using the tags the change touched |
| A sub-flow | the flow that reaches it soonest, cut after it |
| Build files, tests, docs, code no screen reaches | nothing (the full check covers them) |

Navigation hubs (`Host.kt`) are not screens, and the search for screens does not go through them.
Org-config and dev-tools flows count 5 extra steps for their seeding. `known-bug` flows are only run
when the PR edits them.

Screens built only from shared components (user cards, the capture button) can't be told apart by
their ids. `e2e/ci/affected-overrides.json` names the ids that show such a screen, and the step it
comes after (user select: `UserButton_*` after `TrackButton`). Add one when a new screen is like
that, or `ignore` globs for paths that should never pick a flow. The job summary lists changed
screens no flow reaches: candidates for a new flow.

Try it locally:

```bash
python3 e2e/ci/affected.py --base origin/master            # the table on stderr, selectors on stdout
python3 e2e/ci/affected.py --files app/src/main/java/org/greenstand/android/TreeTracker/dashboard/DashboardScreen.kt
python3 -m unittest discover -s e2e/ci -p 'test_*.py'
```

### Before and after: does the PR's change work?

A clip of the PR's app near the changed code does not show that a fix fixes anything. A
**validating flow** does: a flow that fails on the app without the change and passes on the app
with it. The same workflow runs each validating flow twice, recorded and annotated both times: on
the app built from the PR's merge base ("before") and on the PR's app ("after"). The flow files,
sub-flows and seeding scripts are the PR's for both runs; only the app differs.

Which flows (`e2e/ci/validating.py`, at most 3):

1. Named in the PR description, on a line `validates: <flow> [<flow> ...]` (a flow's name;
   `.yaml` and `e2e/flows/` are optional). Use it for a flow that exists already.
2. Flows the PR adds or edits, unless still tagged `known-bug`. A fix PR that drops `known-bug`
   from its flow needs nothing else.

The verdict (`e2e/ci/verdict.py`), in the PR comment with both videos side by side:

| Before (base app) | After (PR's app) | Verdict |
|---|---|---|
| fails | passes | ✅ validated |
| passes | passes | ⚠️ does not demonstrate the change (it passes without it too) |
| fails | fails | ❌ broken |
| passes | fails | ❌ broken (the PR makes it fail) |
| a side did not run: the base app did not build, seeding failed | | ❔ inconclusive, with the reason |

The same table, with both videos, is also written into the PR **description**, between
`<!-- e2e-videos:start -->` and `<!-- e2e-videos:end -->` (the PR template has the markers; without
them the block is added at the end when validating flows ran). Only the text between the markers
is replaced, on every run. An edit by the workflow's token does not start other workflows, so
"Check for screenshot & video" sees the videos the next time a person or an agent saves the
description. How an agent writes such a flow and what it does with each verdict:
[`pr-validating-flow`](../../.claude/skills/pr-validating-flow/SKILL.md).

Only **broken** fails the job. The comment also quotes the error each failing run stopped at:
read it, since a flow can fail on the base app for a reason that is not the bug (a test tag the
PR adds is a fair reason for a new feature; a seeding error is not). Each side runs once, so a
flaky flow can give a wrong verdict. A PR with no validating flow says so in its comment: its
clips only show screens near the change.

What a validating flow cannot show, so don't expect one: a fix with nothing to see on screen (a
leak, stale state behind the UI), a race a driven tap can't hit reliably (double-tap guards), and
states an offline emulator can't reach (a server error). Those need a unit or instrumented test.

Results are in `validate/before/` and `validate/after/` (each laid out as below) with
`validate/verdict.json` and `verdict.md`. Every video also gets a GIF beside it
(`videos/<flow>.gif`), which the PR comment embeds, since a comment plays a GIF inline but only
links an MP4.

Without a PR, for any pushed branch (the "before" app defaults to the merge base with
`origin/master`; Actions > Flow runner > Run workflow has the same two inputs):

```bash
bash e2e/ci/cloud-flows.sh run --validate settings-delete-user-offline [--before <sha>]
cat build/flow-results/validate/verdict.md
```

Locally, with both APKs built:

```bash
TB=tb VALIDATE_FLOWS="settings-delete-user-offline" BEFORE_APK=before.apk AFTER_APK=after.apk \
  bash e2e/ci/run-job.sh -s emulator-5554
```

**PRs from forks** don't run on their own: their code would run on the emulator with this
repository's caches. The "pick flows" job still lists what would run (picked by the base branch's
copy of `affected.py`, not the fork's). After reading the PR's code, a maintainer runs it from
Actions > "Affected flows (annotated video)" > Run workflow, with the PR's number and the full sha
of the commit they read; the run stops if the PR has moved on since. That run has a read-only token, saves no caches
(tb, emulator, Go, Gradle), loads no local action from the PR and pushes no results branch; the
videos are in the artifact, linked from the PR comment. Code on a runner can still reach the
runner's cache token, so only run code you have read. The same button re-runs a PR from this
repository.

Results are as below, in the `flow-results-affected-pr<N>-<run id>` artifact and, for a PR from this
repository, the `flow-results/affected-pr-<N>` branch (`cloud-flows.sh fetch --affected <N>`), with
`videos/<flow>.mp4` and its captions, `videos/<flow>.srt`.

## Results

The same folder in the `flow-results-pr<N>-<run id>` (or `flow-results-<run id>`) artifact, on the
results branch and in `build/flow-results/` locally:

```
summary.json   run details, and per flow: status, seconds, error, screenshots, video, files
summary.md     the table on the run's page and in the PR comment
screens/<flow>/step-03-tapOn-SettingsButton.png   the screen after step 3
screens/<flow>/step-23-end-....png                the screen the flow ended on
screens/<flow>/failure.png                        the screen at a failure
flows/<flow>/  console.txt log.txt error.txt screen.txt hierarchy.json commands.json
               logcat.txt (artifact only) logcat-app.txt crash.txt report.xml setup.txt
               attempts.txt console-try<N>.txt (tries whose recording was lost)
videos/<flow>.mp4                                 with --video, or annotated (ANNOTATE=1)
videos/<flow>.srt                                 annotated: the step captions (tb's 3-minute
                                                  parts joined into one video and one .srt)
videos/<flow>.gif                                 on CI: the video as a GIF, for the PR comment
validate/before/ validate/after/                  validating flows on the base app and the PR's
validate/verdict.json verdict.md                  app (each folder laid out like this one)
```

Steps that only check the screen (`assertVisible` and the like) get no screenshot, since the screen
is the one before them. `--screenshots end` keeps only the last screen, `none` takes none. A
screenshot takes a moment, so a flow that depends on tight timing can behave differently with
`steps`; use `end` for it.

`summary.json` also says what became of each video that was asked for: `recording` is `saved`,
`partial` (tb kept what the device had when stopping the recording failed) or `lost`, with tb's
reason in `recordingError`, and `tries` is how many times the flow ran to get it.

The job fails when a flow fails, or when a video that was asked for was lost, after uploading
everything.

### Lost recordings

On CI's emulator the adb connection sometimes drops at the moment tb copies its recording off the
device (the device's log has `adbd: host-N: connection terminated: write failed`; tb says
`--record: the recording could not be saved: adb pull /sdcard/tb-recording-1.mp4 ... exit status 1`).
The flow's result is not affected, but tb then deletes the device's copy, so the video is gone.
It is the emulator, not tb: a plain `adb pull` of any file fails the same way, on one or two of
the first pulls after the emulator boots and, as far as it was measured (task 115), not after.

`run-flows.sh` does two things about it when a video is asked for. Before the first flow it pulls
a throwaway file until 5 pulls in a row work ("adb pull warm-up: N dropped ..." in the log), so
the early drops are spent there. And a flow whose recording was still lost runs again, up to
`RECORD_TRIES` times in all (default 3), keeping the log of each try that lost its video
(`flows/<flow>/attempts.txt`, `console-try<N>.txt`, `logcat-try<N>.txt`). The result and the
screenshots are the last try's.

A video still missing after that is reported as **lost**, never as a plain pass: a warning on the
run, "N recordings lost" and a Video column in the summary, "recording lost" with tb's reason in
the PR comment, exit status 3 from `run-flows.sh` and a failed job. Re-run the job to record it.

## Adding a flow

1. Write `e2e/flows/<name>.yaml` (Maestro syntax, ids from `TestTags`; see `e2e/README.md`) with
   a `tags:` line, so tag selectors and `run.sh` pick it up.
2. Try it before committing: `bash e2e/ci/cloud-flows.sh run --inline e2e/flows/<name>.yaml`
   (once the runner is on master), or push and `cloud-flows.sh pr <N> <name>`.
3. Check the step screenshots, then commit it. The "Android end-to-end" check runs it with the
   other flows on every PR.

## Locally

With a disposable emulator and `tb` on PATH:

```bash
./gradlew :app:installDebug
TB=tb OUT=build/flow-results bash e2e/ci/run-flows.sh -s emulator-5554 settings-logout
# Annotated video of the first 7 steps (needs ffmpeg):
TB=tb ANNOTATE=1 SCREENSHOTS=end bash e2e/ci/run-flows.sh -s emulator-5554 capture-first-tree@7
```

## What only a maintainer can do

- The `run-flows` label is created the first time someone adds it (`cloud-flows.sh pr` does).
- If a ruleset blocks pushes to `flow-results/*` branches, results still reach the artifact, and
  the PR comment, but a cloud session can't fetch them: allow `github-actions[bot]` there.
