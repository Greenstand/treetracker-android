# Task 39 - CI version codes without committing a bump

## Goal

Make the Firebase beta deploy work again. Every run since March 2026 failed at its first step,
"Bump version code", before building anything.

## Cause

That step committed `versionCode + 1` to `app/build.gradle` and pushed it straight to `master`.
Since December 2025, a repository ruleset ("Master") requires a pull request for every change to
`master`, with no bypass actors, so the push was rejected. The last version bump that reached
`master` is from 2023. The Play Store workflow used the same step, so it was broken the same way.

## Changes

- `app/build.gradle`: `versionCode = baseVersionCode + treetracker.versionCodeOffset`, where
  `baseVersionCode` is 197 and the offset is a Gradle property that defaults to 0.
- `.github/workflows/actions/build-apk`: passes the workflow's run number as the offset for every
  variant except `release`, which keeps the committed number. It exposes the result as the
  `version_code` output and logs it, e.g. `Version code: 218 (base 197 + run number offset 21)`.
- `.github/workflows/build.yml`: passes `version_code` through as a workflow output.
- `deploy-firebase-beta.yml` and `deploy-play-store.yml`: the bump job is removed. The
  commented-out Slack step now reads the build's `version_code`.
- Removed `bump-version.yml`, `bump-version-only.yml` and `actions/bump-version`, which only
  existed to push the bump commit.
- `docs/release/releases.md`: a "Version codes" section.

## Decisions

- **Base + run number, not committed.** CI builds get a number that goes up with each run, and
  nothing is pushed to `master`. Release builds keep the committed `baseVersionCode`, raised in
  the release PR, the way Mihon, NewPipe and Wikipedia do it.
- **An earlier version of this PR used the commit count** (`git rev-list --count HEAD`). That
  goes wrong after squash merges: a branch with 10 commits builds as `master` + 10, then lands on
  `master` as 1 commit, so later `master` builds have lower codes. Local builds (197) also didn't
  match CI builds (around 2031).
- **Run numbers are per workflow.** Only the beta workflow builds non-release variants in CI
  today. If another workflow starts building the same app ID, their numbers can collide.
- **Testers who installed beta 2032** (the test run of the commit-count version, sent to the
  `debug` group) have to uninstall once, because the next beta is around 218 and Android won't
  install a lower version code over a higher one.

## Verification

- `assembleBeta -Ptreetracker.versionCodeOffset=21`: the APK has `versionCode='218'` (checked
  with `aapt2 dump badging`). `assembleDebug` without the property: `versionCode='197'`.
- The action's `grep -oP 'def baseVersionCode = \K\d+'` pattern finds 197 (checked with Perl,
  which uses the same regex syntax).
- All workflow and action YAML parses.
- End to end, with the earlier commit-count version: "Deploy to Firebase Beta" run
  [#20](https://github.com/Greenstand/treetracker-android/actions/runs/40372339020), from this
  branch to the `debug` group, passed. It computed version code 2032 (`master` plus this commit),
  built in 3m 34s on JDK 25, and Firebase reported "uploaded new release 2.2.0 (2032)" and
  "distributed to testers/groups successfully". Nothing was pushed to any branch.

## Follow-ups seen in the run

GitHub flagged `actions/setup-java@v4` and other Node 20 actions as deprecated (Node 24 is forced
for now). Moving to `setup-java@v5` and current major versions of the other actions is separate work.

## When a non-release build's number can go down

Within one workflow, run numbers only go up, so codes only go up. Re-running a run reuses its
number (an equal code still installs). The number can go down when:

- **The workflow file is renamed or moved.** GitHub treats the file as a new workflow, and its run
  number restarts at 1. Raise `baseVersionCode` past the last build in the same PR.
- **Another workflow builds the same app ID.** Each workflow has its own counter. Today only the
  beta workflow builds non-release variants, so the beta app ID is safe. A future "debug build for
  a PR" workflow would only clash with another workflow that also builds `.debug`.
- **An older branch is built after `baseVersionCode` was raised on `master`.** For example, if
  `master` raised the base to 210 and beta run #40 from `master` built 250, then run #41 from a
  branch still on base 197 builds 238.
- **A local build is installed over a CI build of the same app ID.** Local builds use the base
  only. `adb install -r -d` allows the downgrade for debuggable builds.
