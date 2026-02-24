# Environment Setup

## Tooling

- IDE: Android Studio (Jetpack Compose-compatible version)
- Android API: API 21+

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
