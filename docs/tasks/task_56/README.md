# Task 56 - Background sync worker in core:data

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

The dashboard starts `TreeSyncWorker`, so the worker has to live below the feature modules.
Move `background/` into `core:data`.

## Changes

- `background/` (`TreeSyncWorker`, `SyncNotificationManager`, `NotificationConstants`) moves to
  `core/data`, same package. Its `R` imports point at `core:resources`.
- `TreeSyncWorker.getForegroundInfo` referenced two things only the app has. Both are now
  resolved at runtime, with the same result:
  - the notification's tap intent was `Intent(context, TreeTrackerActivity::class.java)`; it is
    now `Intent().setClassName(context, NotificationConstants.MAIN_ACTIVITY_CLASS)`, the same
    explicit intent. `MainActivityClassTest` in `app` fails if the activity is renamed or moved.
  - the title was `getString(R.string.app_name)`, a per-build-type `resValue` in `app`; it is now
    the app's label, which the manifest sets to `@string/app_name`.
- New `TreeSyncWorkerForegroundInfoTest` (Robolectric) checks the worker notification's title and
  tap target. `core:data` unit tests now include Android resources.
- Version catalog: `androidx-work-testing`.

That notification is only shown for expedited work before Android 12. During a normal sync on
newer Android the app shows `SyncNotificationManager`'s "Syncing..." notification, which is
unchanged.

## Verification

- All variants assemble; unit tests (490, including the 2 new ones), screenshot tests (no
  changes), ktlint, detekt and spotless pass.
- Emulator (API 37): smoke test has no crashes; tapping Upload starts the sync and shows the
  "Syncing... / Uploading trees" notification.
