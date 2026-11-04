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
