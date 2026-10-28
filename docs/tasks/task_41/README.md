# Task 41 - Fix unit test failures in the test coverage PR

## Goal

Make `testReleaseUnitTest` and `testDebugUnitTest` pass on the test coverage branch (PR #1374).

## Fixes

- `SyncDataUseCaseTest`: `coVerify(exactly = 0) { uploadLocationDataUseCase.execute(any()) }`
  failed with `MockKException: left matchers: [any<Unit>()]`. MockK can't match `any()` for a
  `Unit` argument, so the checks pass `Unit` directly.
- `ImageUtilsBitmapTest` "orientImage ignores a missing file": Robolectric's native
  `BitmapFactory.decodeFile` creates an empty file at the path, so the test now checks that
  nothing was written instead of that the file doesn't exist.
- `app/build.gradle`: test JVMs get a 3 GB heap. With Gradle's 512 MB default the Robolectric
  Compose UI tests in `testDebugUnitTest` ran out of memory.
