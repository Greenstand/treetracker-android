# Environment Setup

## Tooling

- IDE: Android Studio Panda 3 (2025.3.3) Patch 1 or newer, with JDK 25
- Build SDK: Android 17 (API 37), with Android SDK Build-Tools 36.0.0 or newer
- Target SDK: Android 16 (API 36), the minimum required for Google Play updates
- Supported devices: Android 6.0 (API 23) or newer

## Device Setup

- Use a real device or emulator.
- If testing on a real device indoors, a GPS faker can help validate location-gated flows.

## Build Setup

1. Request `treetracker.keys.properties` from the Android team.
2. Place `treetracker.keys.properties` at the repository root.
3. Select a development variant (`dev` is recommended).
4. Connect a device or start an emulator.
5. Run the app from Android Studio or use Gradle commands from [Common Commands](common-commands.md).

See also: [Prerequisites](prerequisites.md), [Build Variants](build-variants.md).
