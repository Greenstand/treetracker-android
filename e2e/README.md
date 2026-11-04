# End-to-end flows

Flows for [Test Buddy](https://github.com/Elforama/testbuddy) (`tb`), which reads Maestro-style
YAML and drives the installed debug app on an emulator or device.

- `flows/first-screen.yaml`: a fresh install opens on the language picker.

CI runs them in the "Android end-to-end" check (`.github/workflows/android-e2e.yml`). A failed
step makes `tb` exit non-zero, which fails the check.

Run locally, with an emulator running and `tb` built from the Test Buddy repo:

```bash
./gradlew :app:installDebug
tb test e2e/flows/ -p android
```

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
