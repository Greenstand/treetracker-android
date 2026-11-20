# Running flows on CI from a cloud session

Claude Code cloud sessions have no Android emulator. The **Flow runner** workflow runs any Test
Buddy flows (`e2e/flows/`) on CI's emulator for any branch or PR, and brings back a screenshot
after each step, the step log and logcat, for the session (or a person) to look at. It replaces
writing a one-off CI script per PR.

| File | What it does |
| --- | --- |
| `.github/workflows/flow-runner.yml` | The reusable workflow: builds the debug app, boots the emulator, runs the flows, publishes the results. `workflow_call` and `workflow_dispatch`. |
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
      uses: ./.github/workflows/flow-runner.yml
      with:
        flows: settings-logout tag:signup
        pr: ${{ github.event.pull_request.number }}
  ```

A PR that changes the runner itself runs `first-screen` and `settings-logout`, with videos, as a
smoke test.

A video on the results branch can go in a PR description for the "Check for screenshot & video"
check: link it at the results commit (`https://github.com/<repo>/blob/<commit>/videos/<flow>.mp4`),
since the branch moves on with the next run.

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
videos/<flow>.mp4                                 with --video
```

Steps that only check the screen (`assertVisible` and the like) get no screenshot, since the screen
is the one before them. `--screenshots end` keeps only the last screen, `none` takes none. A
screenshot takes a moment, so a flow that depends on tight timing can behave differently with
`steps`; use `end` for it.

The job fails when a flow fails, after uploading everything.

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
```

## What only a maintainer can do

- The `run-flows` label is created the first time someone adds it (`cloud-flows.sh pr` does).
- If a ruleset blocks pushes to `flow-results/*` branches, results still reach the artifact, and
  the PR comment, but a cloud session can't fetch them: allow `github-actions[bot]` there.
