# Task 39 - Version code from the commit count

## Goal

Make the Firebase beta deploy work again. Every run since March 2026 failed at its first step,
"Bump version code", before building anything.

## Cause

That step committed `versionCode + 1` to `app/build.gradle` and pushed it straight to `master`.
Since December 2025, a repository ruleset ("Master") requires a pull request for every change to
`master`, with no bypass actors, so the push was rejected. The last version bump that reached
`master` is from 2023. The Play Store workflow used the same step, so it was broken the same way.

## Changes

- `.github/workflows/actions/build-apk`: computes the version code as `git rev-list --count HEAD`
  and builds with `-Ptreetracker.versionCode=<count>`. It fails clearly on a shallow clone, where
  the count would be wrong. The code is exposed as the `version_code` output.
- `.github/workflows/build.yml`: checks out the full history (`fetch-depth: 0`) and passes
  `version_code` through as a workflow output.
- `app/build.gradle`: `versionCode` reads the `treetracker.versionCode` Gradle property and falls
  back to the committed 197 for local builds.
- `deploy-firebase-beta.yml` and `deploy-play-store.yml`: the bump job is removed. The commented-out
  Slack step now reads the build's `version_code`.
- Removed `bump-version.yml`, `bump-version-only.yml` and `actions/bump-version`, which only
  existed to push the bump commit.
- `docs/release/releases.md`: a "Version codes" section.

## Decisions

- **The commit count rather than a run number.** Each workflow has its own run counter, so the beta
  and Play workflows would overlap. The commit count is shared, increases with every squash merge,
  and the same commit always gets the same code. On `master` it's 2031, well above the 197 in the
  last committed bump, so new builds install over older ones.
- **Branch builds** (a beta from a feature branch) get the branch's own count. It can be lower than
  a later `master` build. Android won't install a lower version code over a higher one, so a
  tester going from a newer `master` build to an older branch build has to uninstall first.

## Verification

- `assembleBeta -Ptreetracker.versionCode=2031`: the APK has `versionCode='2031'` (checked with
  `aapt2 dump badging`). `assembleDebug` without the property: `versionCode='197'`.
- All workflow and action YAML parses.
- End to end: "Deploy to Firebase Beta" run
  [#20](https://github.com/Greenstand/treetracker-android/actions/runs/40372339020), from this
  branch to the `debug` group, passed. It computed version code 2032 (`master` plus this commit),
  built in 3m 34s on JDK 25, and Firebase reported "uploaded new release 2.2.0 (2032)" and
  "distributed to testers/groups successfully". Nothing was pushed to any branch.

## Follow-ups seen in the run

GitHub flagged `actions/setup-java@v4` and other Node 20 actions as deprecated (Node 24 is forced
for now). Moving to `setup-java@v5` and current major versions of the other actions is separate work.
