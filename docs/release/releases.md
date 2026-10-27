# Releases

## Version codes

`gradle.properties` has `treetracker.baseVersionCode` (currently 197) and `treetracker.versionName`. CI never commits a version bump
(`master` only accepts pull requests). Instead, the number is set for each build:

- **Release builds** use `baseVersionCode` as is. To release, raise it in a normal PR.
- **Every other CI build** (beta, dev, debug, prerelease) uses `baseVersionCode` plus the
  workflow's run number. For example, beta run #21 builds 197 + 21 = 218. The number exists only
  in that build.
- **Local builds** use `baseVersionCode`.

Raising `baseVersionCode` for a release also raises every later CI build, so betas stay above the
release. To change the version name, edit `treetracker.versionName` in `gradle.properties` in a normal PR.

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
