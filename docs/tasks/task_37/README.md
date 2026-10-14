# Task 37 - Debug AppFunctions for agent testing

## Goal

Let agents testing the app set up state over adb instead of tapping through the UI:

- Create an account, so a clean install skips the first-run setup flow and extra accounts skip the
  add-user flow.
- Capture a number of trees with the bundled test image, without the camera, GPS wait or review
  screens.

Debug builds only. Usage is in [Agent App Functions](../../testing/agent-app-functions.md).

## Changes

- Added `androidx.appfunctions` (`appfunctions`, `appfunctions-service`, and the KSP
  `appfunctions-compiler`) as `debug` and `dev` dependencies only, with
  `appfunctions:aggregateAppFunctions=true` so the compiler generates the app's function index.
- `app/src/debug/java/.../appfunctions/`:
  - `AgentAppFunctions`: the two `@AppFunction`s, `createAccount` and `captureTrees`. They run on
    IO and turn `IllegalArgumentException` into `AppFunctionInvalidArgumentException`, so agents
    get a readable error.
  - `AgentAccountCreator`: creates the user through `UserRepo.createUser` with sign-up's
    validation, the test image as the selfie, and power user set when there is none yet.
  - `AgentTreeCapturer`: saves trees through `CreateTreeUseCase`. With no session running it
    starts one through `SessionTracker`, saves the trees and ends it. If one is running it adds
    the trees to it. It also runs the splash screen's device config and org setup, since the app
    may never have been opened.
  - `AgentResults`: the `@AppFunctionSerializable` return types.
- The `dev` build type also compiles `src/debug/java`, since it is the recommended local build.
- `SessionTracker.startSession(userId, organizationName, sessionNote)`: new overload so a session
  can start without the capture setup scope. `startSession()` now delegates to it, with no
  behavior change.
- `.claude/skills/agent-app-functions/`: a Claude skill that tells agents when and how to use the
  functions. Its `scripts/app_fn.sh` checks the device and API level, finds the debug or dev
  package, quotes the JSON, and adds `reset` (clean install) and `launch` (relaunch) commands.

## Decisions

- **Library version `1.0.0-alpha08`.** alpha09 and later need AGP 9.1 and compileSdk 37. The
  project is on AGP 8.11 and compileSdk 36, and a major AGP upgrade is out of scope. alpha08's
  API (`@AppFunction` + `AppFunctionContext`, service declared by the library) is replaced in
  later versions, so expect a small port when the project moves to AGP 9.
- **Android 17+ to call over adb.** `cmd app_function execute-app-function` is not in Android 16.
- **Release, beta and prerelease are unaffected.** No code, dependency or manifest entry.
  Verified with the merged release manifest.

## Verification

On an Android 17.2 emulator (`Test_Device_A`) with the debug build:

- After `pm clear`, `createAccount` then launching the app opens the dashboard, skipping language
  select and sign-up.
- Called before the app was ever opened, `createAccount` x2 and `captureTrees` save correctly:
  sessions ended, trees on the right wallets, photos on disk, the right counts on the dashboard
  and the user select screen.
- `captureTrees` while on the capture screen adds to the open session, and refuses a different
  wallet.
- Bad input returns clear errors (duplicate wallet, invalid name or phone, both phone and email,
  count out of range, unknown wallet, latitude without longitude).

Unit tests: `AgentAccountCreatorTest` and `AgentTreeCapturerTest` (in `src/testDebug`) and a new
`SessionTrackerTest` case.
