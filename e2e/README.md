# End-to-end flows

Flows for [Test Buddy](https://github.com/Elforama/testbuddy) (`tb`), which reads Maestro-style
YAML and drives the installed debug app on an emulator or device.

- `flows/first-screen.yaml`: a fresh install opens on the language picker.
- `flows/dev-options.yaml`: I1, change a developer parameter and verify persistence
  after navigating back and restarting the app.
- `flows/debug-overlays.yaml`: I2, toggle both overlays during capture and a real
  offline sync attempt, using the developer switches and floating menu.

CI runs them in the "Android end-to-end" check (`.github/workflows/android-e2e.yml`). A failed
step makes `tb` exit non-zero, which fails the check.

Run locally with `adb` and `tb` on PATH and a **disposable emulator** running.
The runner clears this emulator's debug app, grants runtime permissions, seeds
the fixtures, and restores airplane mode and Wi-Fi when it exits. Other devices
are not used. Use dummy keys as CI does; the seeded sync test runs offline.

```bash
ANDROID_SERIAL=emulator-5584 ./gradlew :app:installDebug
TB=tb bash e2e/run.sh -s emulator-5584
```

JUnit reports and failure artifacts are written to `build/e2e/` (`OUT` overrides
the destination). CI uses the same runner on API 35 with emulated cameras.

## Seed first, then open a screen

`setup-device.sh` reuses the
[`agent-app-functions` skill](../.claude/skills/agent-app-functions/SKILL.md):
`reset`, `createAccount` and `captureTrees`. It creates Ada Admin, phone
`5551230001`, and two trees near latitude -3.5, longitude 37.3. On API 37+ it
calls AppFunctions; on API 35/36 a debug-only setup link calls the very same
`AgentAccountCreator` and `AgentTreeCapturer` helpers. No signup or camera UI
is needed to prepare tests.

```bash
# Clears the named emulator's debug app, seeds it, then opens a screen.
bash e2e/setup-device.sh -s emulator-5584 dev-options

# Re-run only the seeded tests; omit clearState so fixtures survive.
tb test e2e/flows/ -p android --device emulator-5584 --include-tags dev-tools
```

The `treetracker-e2e://` links exist only in debug/dev builds:

| Link | Result |
| --- | --- |
| `treetracker-e2e://dashboard` | Dashboard with seeded counts |
| `treetracker-e2e://dev-options` | Dev options, with Dashboard under it for Back |
| `treetracker-e2e://capture` | Capture with a real session for the admin, with Dashboard under it |
| `treetracker-e2e://setup` | API 35/36 fixture fallback; used by the script after reset |

The screen links render the real app Host and initialize the org, device config
and capture session before rendering a screen. They do not emulate screen
behavior. Flows still exercise the long-press logo, navigation back, parameter
persistence, overlay controls and real sync worker themselves. The sync assertion
checks the worker's durable error diagnostic, not a successful upload; offline test data
stays on the emulator. The runner stops the app to cancel outstanding work before
restoring network settings.

For navigation/onboarding tests, continue using the real entry points. The
first-screen flow runs before seeding because it deliberately clears app data.

## Ids for flows

Screens tag the controls a flow needs with names from `TestTags`
(`app/src/main/java/org/greenstand/android/TreeTracker/view/TestTags.kt`), and each composition
root sets `Modifier.exposeTestTags()` so the tags become resource ids. Use them in flows:

```yaml
- tapOn: {id: "TrackButton"}
- tapOn: {id: "UserButton_5551230001"}
- extendedWaitUntil: {visible: {id: "CaptureButton"}, timeout: 15000}
```

`tb` only shows an id when it is unique on screen, so repeated items carry their own suffix
(`LanguageOption_ENGLISH`, `TreeButton_0`) and dialog buttons have their own names
(`DialogPositiveButton`). When you add a screen or a dialog window, tag its controls in
`TestTags` and check them with `tb look`.

## Screen tour

`tour/tour.sh` walks most screens and saves what `tb look` sees on each one (look text,
screenshot, hierarchy) and ends with the number of failed steps. Run it on an API 35 emulator
started with `-camera-back emulated -camera-front emulated`:

```bash
adb emu geo fix 37.3 -3.5
./gradlew :app:installDebug
OUT=build/tour TB=tb bash e2e/tour/tour.sh
```

The `tb screen tour` workflow (`.github/workflows/tb-screen-tour.yml`) runs it on CI for this
branch and for master and uploads the results with a video. It only runs by hand, from the
Actions tab.
