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
    saves them in a session from `SessionTracker.createSession` and closes it under
    `NonCancellable`. That session never becomes the current one, so the UI's capture flow and
    the agent can't end each other's sessions. If a session is running, the trees join it. It
    also runs the splash screen's device config and org setup, since the app may never have been
    opened.
  - `AgentResults`: the `@AppFunctionSerializable` return types.
- The `dev` build type also compiles `src/debug/java`, since it is the recommended local build.
- `SessionTracker.createSession(userId, organizationName, sessionNote)` saves a session without
  making it current, and `closeSession(sessionId)` ends one specific session. `startSession()`
  and `endSession()` now use them, with no behavior change.
- `.claude/skills/agent-app-functions/`: a Claude skill that tells agents when and how to use the
  functions. Its `scripts/app_fn.sh` picks a device when several are connected (Android 17+
  emulators with the build installed; physical devices only when named, and `reset` on one only
  with `-s`). It finds the debug or dev package, escapes the JSON for the device shell, and adds
  `reset` (clean install), `launch` (relaunch) and `devices` commands.

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

Unit tests: `AgentAccountCreatorTest` and `AgentTreeCapturerTest` (in `src/testDebug`) and new
`SessionTrackerTest` cases.

## Review follow-ups

A subagent review found these, now fixed:

- A cancelled `captureTrees` left its session open, because closing it ran in the cancelled
  coroutine. Closing now runs under `NonCancellable`, with a test that fails without it.
- `captureTrees` ended whatever session was current, so it could end a session the UI started
  during a long save, and the next photo would crash. Agent sessions are now never current (see
  `createSession`/`closeSession` above).
- `app_fn.sh` could auto-pick a physical phone, and an exported `ANDROID_SERIAL` skipped the
  `reset` guard. Auto-pick is now emulators only, and `reset` on a phone needs `-s`.
- Email validation was stricter than sign-up's (which only checks for an `@`), and `captureTrees`
  didn't match an email wallet in another case. Both now behave like sign-up.
- Trees placed near a pole or the antimeridian could get invalid coordinates. They are now
  clamped and wrapped.
- The "no account" test was reaching the wrong branch, and the location test didn't check the
  tree count. Both are fixed, and there are new tests for setup order, cancellation and email
  case.
- The script now escapes apostrophes instead of refusing them, since names like O'Brien are valid.

## Second review pass: parsing tests

- Argument checking moved into `AgentInput`, which has no side effects. `AgentInputTest` is
  table-driven (JUnit `Parameterized`), with about 125 cases: names (Unicode, apostrophes,
  hyphens, length limits, digits and symbols), phones, emails, phone and email together, wallet
  lookups, tree counts, coordinates (bounds, NaN, infinity) and wrapping (fixed cases plus a
  10,000-point randomized sweep). Names, phones and emails are now trimmed.
- `app_fn.sh` keeps its commands in `main`, so `test_app_fn.sh` can load its functions. Its 111
  checks use a fake `adb` and cover:
  - reading `adb devices` and `getprop`/`pm` output (CRLF, daemon noise, unauthorized and offline
    devices, test APKs, similar package names);
  - device ranking;
  - JSON quoting, as a round trip through `sh` (apostrophes, `$()`, backticks, globs, quote
    break-outs);
  - error detection;
  - the `list` formatter, against real device output.
  Breaking the escaping, the error matching, the emulator-only rule or the `reset` guard each
  makes it fail.
- Fixed while writing these: any output containing "Exception", such as an account named
  "Exception", was reported as a failure. Now only the device's error lines count. The `list`
  output also drops empty descriptions instead of printing blank lines.

## Second review follow-ups

A second subagent review found no code blockers. These are now fixed:

- `closeSession`'s end time and the start-then-end path of `SessionTracker` weren't tested. They
  are now, and removing the end time makes two tests fail.
- `captureTrees` creates its session under `NonCancellable` too, so a cancellation during the
  insert can't leave a session with no id to close. There is a test that holds the insert at a
  gate while the call is cancelled.
- `app_fn.sh`:
  - `TT_PACKAGE` must end in `.debug` or `.dev`, so `reset` can't clear a release or beta app.
  - Package names are matched literally (`grep -F`).
  - `reset` won't choose between several qualifying emulators.
  - `-s ""` is rejected.
  - 14 new checks cover these rules.
- Docs and KDoc: the name rules (letters, spaces, hyphens and apostrophes), `wallet`'s default when
  a capture session is open, and a note that the functions log the same Firebase Analytics events
  as the UI. The `list` fixture was re-captured from a device.

Not changed: `runAgentFunction` reports any `IllegalArgumentException` as an invalid argument.
Only the error code differs from an unknown error, and the message is the same. Testing the
wrapper needs `Bundle` (so Robolectric), which isn't worth it for a debug tool.

## Final review: simultaneous createAccount calls

`createAccount` checked "does this wallet exist" and "is there an admin yet", then saved the
account. Simultaneous calls all passed the checks before any of them saved. The wallet column
isn't unique, and `insertUser` replaces on conflict, so nothing below stopped it. On an Android
17 emulator, six simultaneous calls on a clean install made **six admins**, and the same phone sent
twice was saved twice.

The check-then-save now runs under a `Mutex` in `AgentAccountCreator`'s companion object. It has
to be static, because the AppFunctions library builds a new instance for each call. With the fix,
three runs of the same device test each gave 1 admin, and the duplicate phone was saved once (the
second call gets "already exists"). Two new tests use a fake repository that suspends on every
call, so calls interleave. Both failed before the fix.

