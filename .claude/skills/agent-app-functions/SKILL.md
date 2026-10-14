---
name: agent-app-functions
description: Sets up Tree Tracker test state in seconds over adb, using the debug-only AppFunctions instead of the UI. createAccount makes a planter account and skips onboarding and sign-up; captureTrees saves any number of trees and skips the camera, GPS wait and review screens. Use this whenever you run, test, verify, reproduce or demo something in treetracker-android on an emulator or device and need a signed-in user, extra users, or trees and sessions to upload, sync, count or show on the map. For example "test the upload with 50 trees", "check the user select screen with three users", "reproduce the dashboard crash", or verifying your own change in the app. Use it even when nobody mentions app functions, and before tapping through sign-up or the capture flow just to get the app into a state.
---

# Agent app functions: test state without the UI

Debug and dev builds of Tree Tracker expose two
[AppFunctions](https://developer.android.com/ai/appfunctions) that write the same rows the UI
would, in about a second each:

- **`createAccount`** creates an account. The first account on a device becomes its admin, and an
  admin existing is what makes the app skip language select and sign-up on launch. Later calls add
  more accounts.
- **`captureTrees`** saves 1 to 500 trees with the bundled test image, as if the planter had
  tracked them.

Getting the same state through the UI takes minutes: the privacy dialog, phone and name entry and
a selfie, then GPS convergence, the camera and the review screen for every tree. The UI steps are
also the flakiest part of any scripted run. So use these functions whenever the setup isn't the
thing you're testing.

When the sign-up or capture screens are what you're testing (a bug in them, or a change to them),
drive the UI for that part. These functions skip those screens entirely, so they prove nothing
about them.

## Requirements

- **A `debug` or `dev` build** installed. Release, beta and prerelease builds don't contain the
  functions. Install with `ANDROID_SERIAL=<serial> ./gradlew :app:installDebug`. Without
  `ANDROID_SERIAL`, Gradle installs on every connected device.
- **Android 17 (API 37) or newer.** Android 16 has no `cmd app_function` shell command. Check
  with `adb -s <serial> shell getprop ro.build.version.sdk`. To find an Android 17 emulator, look
  for `target=android-37` in `~/.android/avd/*.avd/config.ini` and start it with
  `emulator -avd <name>`. If there isn't one, ask the person before downloading a system image,
  since it's several GB. Otherwise fall back to driving the UI.
- **Use a device you're allowed to wipe.** `reset` clears all app data. Don't run it on someone's
  everyday emulator or phone without asking. Before you `-s` a device for `reset`, run
  `$FN devices` to check what's installed on it.

## Quick start

`scripts/app_fn.sh` wraps the adb calls. It picks the device, finds the installed debug or dev
package and checks the API level. It also handles the quoting that makes a hand-written adb
command fail.

```bash
FN=.claude/skills/agent-app-functions/scripts/app_fn.sh

$FN devices                         # which device the script will use, and why
$FN reset                           # clean install: clear app data, re-grant permissions
$FN createAccount                   # admin account "Test User", random 555 phone
$FN captureTrees '{"count": 20}'    # 20 trees for the admin
$FN launch                          # relaunch: dashboard with 20 trees to upload
```

`$FN list` prints each function with its parameter descriptions. Results print as YAML. On bad
input the script prints the app's message and exits 1.

**Device choice.** With several devices connected, the script picks an emulator on Android 17+
with a debug or dev build installed, the newest Android first, then the lowest serial. The ranking
is stable, so `reset`, `createAccount` and `launch` all land on the same device, and each call
prints which one it used. Physical devices are never picked automatically, so test data doesn't
end up on someone's phone. To use one, name it with `-s <serial>` or `ANDROID_SERIAL`. Because
`reset` clears the app's data, it is stricter: a physical device must be named with `-s`, and when
several emulators qualify it asks you to pick one with `-s` instead of choosing.

## Parameters

**createAccount**: all optional. Pass `{}` or nothing for the defaults.

| Parameter | Default | Rules |
| --- | --- | --- |
| `firstName`, `lastName` | `Test`, `User` | At least 2 characters of letters, spaces, hyphens and apostrophes, like sign-up |
| `phone` | Random unused `555xxxxxxx` | 7 to 15 digits |
| `email` | Not set | Pass phone or email, not both. Lowercased, like sign-up |

It returns `userId`, `wallet` (the phone or email you sign in with), `firstName`, `lastName` and
`isPowerUser`. Creating a wallet that already exists fails.

**captureTrees**

| Parameter | Default | Rules |
| --- | --- | --- |
| `count` | Required | 1 to 500 |
| `wallet` | The admin, or the open session's account | Must be an existing account |
| `latitude`, `longitude` | Device location while the app is open, else 0, 0 | Pass both, with a decimal point (`10.0`, not `10`) |
| `note` | Empty | Saved on every tree |

It returns `sessionId`, `wallet`, `treeCount` and `treeUuids`. Trees land at random points within
about 80 m of the location.

## Behaviour to plan around

- **Relaunch to see the data.** Screens that are already open don't reload, so run `$FN launch`
  after creating accounts or trees.
- **Sessions.** With no capture session running, `captureTrees` saves the trees in a new session
  for the account and ends it, as if the planter had left the capture flow. That session never
  becomes the app's current one, so it can't interfere with a capture flow the UI starts
  meanwhile. It is ended even if the call is cancelled. If the app is on a capture screen, the
  trees join that open session and it stays open. A `wallet` other than that session's fails.
- **Pass a location when the map matters.** Without one, trees go at the device's last location,
  but only while the app is open in the foreground. Android hides location from background apps,
  so after `reset` or before `launch` the trees land at 0, 0. Passing `latitude` and `longitude`
  always works.
- **The trees are real local data.** The next sync uploads them to the build's non-production
  backend. Keep the device offline (airplane mode) if test trees shouldn't leave it. Like the UI,
  the functions also log Firebase Analytics events (tree planted, user created with its phone or
  email), so use made-up phones and emails.
- Trees get step counts of 0 and no rotation matrix or DBH. Selfies and tree photos are all the
  same test image.

## Calling without the script

```bash
adb shell cmd app_function execute-app-function \
  --package org.greenstand.android.TreeTracker.debug \
  --function 'org.greenstand.android.TreeTracker.appfunctions.AgentAppFunctions#captureTrees' \
  --parameters \''{"count": 10}'\' \
  --brief-yaml
```

The `\'` ... `\'` around the JSON matters. The device shell parses the arguments again, and a
plain `'{"count": 10}'`, as in Google's docs, reaches the app with its double quotes stripped,
causing a `JSONException`. Use `org.greenstand.android.TreeTracker.dev` for dev builds.

## Changing the functions

The code is in `app/src/debug/java/org/greenstand/android/TreeTracker/appfunctions/`. Argument
checking lives in `AgentInput`. The full guide is `docs/testing/agent-app-functions.md`. If you add
or change a function, update that guide, this skill and `scripts/app_fn.sh` too. The library is
pinned to `1.0.0-alpha08`, because later versions need AGP 9.1 and compileSdk 37.

Run both test suites after a change. Neither needs a device:

```bash
./gradlew :app:testDebugUnitTest --tests "org.greenstand.android.TreeTracker.appfunctions.*"
bash .claude/skills/agent-app-functions/scripts/test_app_fn.sh
```

The script's tests use a fake `adb`. `scripts/testdata/list-app-functions.json` is real
`list-app-functions` output. If you change a function's KDoc, re-capture it from a device and
regenerate `list-app-functions.expected.txt`.
