# App Distribution Testing

This page consolidates legacy "Android pre-release testing" and "Tree Tracker test app instructions" wiki pages.

## Access And Install

1. Request testing invitation access from the Android team.
2. Open the Firebase App Distribution invitation on mobile.
3. Install the build (allow unknown sources if required by device settings).

## Publishing A Build

Run the **Deploy to Firebase** workflow (`.github/workflows/deploy-firebase-beta.yml`) from
the Actions tab. Its inputs:

- **Branch**: the branch to build from.
- **Build to deploy**: `beta` (test environment, installs as "Tree Tracker(test)") or `dev`
  (dev environment, installs as "Tree Tracker(dev)"). See [Build Variants](../getting-started/build-variants.md).
- **Tester group**: the Firebase App Distribution group that gets the build.
- **Release notes**.

Each build type is a separate app in Firebase, so a tester sees beta and dev builds as two apps
and can have both installed.

## Core Test Checklist

1. Login supports phone, email, or both (including special characters).
2. Re-login with same identifier should skip redundant profile steps where expected.
3. User switching works.
4. Add a tree works with GPS accuracy gate/convergence behavior.
5. Capture and upload image flows complete.
6. Sync succeeds.
7. Captured data appears in test cloud/admin tooling.

## Test Report

Capture the following for each run:

- Time per capture
- Bugs found
- UX/UI points that were unclear

## Defect Submission

File defects/suggestions in project issues:

- [treetracker-android issues](https://github.com/Greenstand/treetracker-android/issues)
