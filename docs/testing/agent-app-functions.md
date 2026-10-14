# Agent App Functions

Debug and dev builds expose [AppFunctions](https://developer.android.com/ai/appfunctions) that let
an agent set up test data over adb instead of driving the UI:

- `createAccount` creates an account without the sign-up screens. The first account on a device
  becomes its admin, which is what skips the first-run setup flow.
- `captureTrees` saves trees with the bundled test image (`assets/testtreeimage.jpg`), skipping
  the camera, GPS convergence and review screens.

Release, beta and prerelease builds do not contain this code or the AppFunctions library.

## Requirements

- A `debug` or `dev` build. Package names are `org.greenstand.android.TreeTracker.debug` and
  `org.greenstand.android.TreeTracker.dev`.
- **Android 17 (API 37) or newer** to call functions over adb. Android 16 has the AppFunctions
  service but no `cmd app_function` shell command. Check with
  `adb shell cmd app_function help`.

The app does not need to be running. The system starts it to run the function.

## Calling a function

Agents should use the `agent-app-functions` skill (`.claude/skills/agent-app-functions/`). Its
`scripts/app_fn.sh` picks a suitable device when several are connected (or takes `-s <serial>`).
It also handles the package, the API check and the quoting, and adds `reset`, `launch` and
`devices` commands. To call adb directly, read on.

The device shell parses the arguments a second time, so wrap the JSON in `\'` ... `\'` as below.
Without that, the device shell strips the JSON's double quotes and the call fails with a
`JSONException`. Avoid apostrophes in values.

```bash
adb shell cmd app_function execute-app-function \
  --package org.greenstand.android.TreeTracker.debug \
  --function 'org.greenstand.android.TreeTracker.appfunctions.AgentAppFunctions#createAccount' \
  --parameters \''{"firstName": "Ada", "lastName": "Admin", "email": "ada@example.com"}'\' \
  --brief-yaml
```

```bash
adb shell cmd app_function execute-app-function \
  --package org.greenstand.android.TreeTracker.debug \
  --function 'org.greenstand.android.TreeTracker.appfunctions.AgentAppFunctions#captureTrees' \
  --parameters \''{"count": 10, "latitude": -3.5, "longitude": 37.3}'\' \
  --brief-yaml
```

Pass `{}` when you want every default. To see the functions and their parameter descriptions, run
`adb shell cmd app_function list-app-functions --package <package>`.

## createAccount

| Parameter | Type | Default |
| --- | --- | --- |
| `firstName` | String | `Test` |
| `lastName` | String | `User` |
| `phone` | String, 7 to 15 digits | A random unused `555xxxxxxx` number |
| `email` | String | Not set. Pass phone or email, not both |

Names follow the sign-up rules: at least 2 characters, letters only. Emails are lowercased, as
sign-up does. The selfie is the test image. Returns `userId`, `wallet`, `firstName`, `lastName`
and `isPowerUser`.

The first account becomes the admin (power user). Later calls add more accounts, as the "add
user" flow does. Creating an account whose wallet already exists fails.

## captureTrees

| Parameter | Type | Default |
| --- | --- | --- |
| `count` | Int, 1 to 500 | Required |
| `wallet` | String | The admin account |
| `latitude`, `longitude` | Double, pass both | The device's location while the app is open, otherwise 0, 0 |
| `note` | String | Empty |

Trees land at random points within about 80 m of the location. Android hides location from apps
in the background, so when the app isn't open and no location is passed, trees go at 0, 0.
Returns `sessionId`, `wallet`, `treeCount` and `treeUuids`.

- **No capture session running** (the usual case): the trees go in a new session for the account,
  which is ended straight away, as if the planter had tracked them and left the capture flow.
- **Session running** (the app is on a capture screen): the trees are added to that session and it
  stays open. Passing a `wallet` other than the session's fails.

## Gotchas

- **Restart the app to see changes.** The screens already open don't reload, so force-stop and
  relaunch:
  `adb shell am force-stop <package> && adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1`.
- **Doubles need a decimal point.** `"latitude": 10` fails to parse. Use `10.0`.
- **`pm clear` also revokes runtime permissions,** so the app asks for camera and location again on
  launch. Re-grant them with `adb shell pm grant <package> android.permission.CAMERA` (and
  `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACTIVITY_RECOGNITION`, `POST_NOTIFICATIONS`).
- Bad input returns `AppFunctionException ... (code 1001)` with a message that says what to fix.

## Clean install to trees in a few seconds

```bash
P=org.greenstand.android.TreeTracker.debug
F=org.greenstand.android.TreeTracker.appfunctions.AgentAppFunctions
adb shell pm clear $P
for perm in CAMERA ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION ACTIVITY_RECOGNITION POST_NOTIFICATIONS; do
  adb shell pm grant $P android.permission.$perm
done
adb shell cmd app_function execute-app-function --package $P --function "$F#createAccount" --parameters \''{}'\'
adb shell cmd app_function execute-app-function --package $P --function "$F#captureTrees" --parameters \''{"count": 20}'\'
adb shell monkey -p $P -c android.intent.category.LAUNCHER 1
```

The app opens on the dashboard with 20 trees waiting to upload.

## Code

- `app/src/debug/java/.../appfunctions/AgentAppFunctions.kt` declares the functions. The
  AppFunctions compiler (KSP) generates the function index and the XML the system reads.
- `AgentAccountCreator` and `AgentTreeCapturer` hold the logic. Their tests are in
  `app/src/testDebug`.
- The library's own manifest declares the service, so there is no manifest entry to maintain.
- The library is pinned to `1.0.0-alpha08`, the last version that builds with AGP 8.x and
  compileSdk 36. From alpha09 it needs AGP 9.1 and compileSdk 37, and later versions change the
  API: `@AppFunction` becomes `@AppFunctionDeclaration` inside an
  `@AppFunctionServiceEntryPoint` service, and `AppFunctionContext` is removed.
