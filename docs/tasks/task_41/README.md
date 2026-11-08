# Task 41 - Fix unit test failures in the test coverage PR

## Goal

Make `testReleaseUnitTest` and `testDebugUnitTest` pass on the test coverage branch (PR #1374).

## Fixes

The main fixes (MockK `Unit` matchers in `SyncDataUseCaseTest`, test JVM heap, and 320dp
qualifiers for text-field dialog UI tests) landed on the PR branch directly. This task adds:

- `ImageUtilsBitmapTest` "orientImage on a missing file does not throw" also checks that
  nothing was written. Robolectric's native `BitmapFactory.decodeFile` creates an empty file
  at the path, so the check is on the file's length, not on it being absent.
- `.gitignore`: ignore `.kotlin/` compiler session files.
