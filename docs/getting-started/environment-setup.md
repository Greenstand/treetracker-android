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

## Production release configuration

Production releases are built by the `Deploy to Google Play Store` GitHub
Actions workflow. Repository administrators must configure these Actions
secrets:

- `S3_PRODUCTION_IDENTITY_POOL_ID`
- `PROD_TREETRACKER_CLIENT_ID`
- `PROD_TREETRACKER_CLIENT_SECRET`
- `KEYSTORE_FILE` (the release keystore encoded as base64)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `PLAY_STORE_SERVICE_ACCOUNT`

The workflow creates `treetracker.keys.properties` and decodes the keystore
only for the duration of the build. These generated files must never be
committed to the repository.
