# Releases

## Version codes

CI builds (the Firebase beta and Play Store workflows) set `versionCode` to the number of commits
on the branch being built (`git rev-list --count HEAD`). Every merge to `master` adds one, so codes
always go up, and CI never commits a version bump (`master` only accepts pull requests). Building
the same commit twice gives the same code. The `versionCode` in `app/build.gradle` is only used for
local builds.

To raise `versionName`, edit it in `app/build.gradle` in a normal PR.

## Legacy release notes

This section preserves legacy release notes from the prior wiki.

### 2.1 (2023-08-22)

- Status: Current Store Build (at time of original note)

### 2.0.0 (2022-03-15)

- Status: Current Store Build (at time of original note)
- Bugs: none listed
- Blockers: none listed
- Changes:
  - Complete UI overhaul using Jetpack Compose
  - Increase upload speed
  - Added messaging functionality

### 1.3.0 (date unknown)

- Status: Old
- Bugs: none listed
- Blockers: infrastructure
- Changes:
  - Increased image upload speed
  - Allowed debug fake trees without GPS signal
  - Bulk user uploads
  - Debug trees visible on map
  - Fixed hidden signup button on smaller screens

### 1.2.7 (2020-02-26)

- Status: Old
- Changes:
  - More stable image capture
  - Focus warning UI
