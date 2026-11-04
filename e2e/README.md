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

`tour/tour.sh` walks most screens and saves what `tb look` sees on each one; the temporary
`tb-screen-tour` workflow runs it on an emulator.
