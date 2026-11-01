# Task 42 - Expand unit and Compose UI test coverage (round 2)

## Goal

Follow-up to PR #1374. Find code that still has no tests, or only partial tests, and add unit
tests and Robolectric Compose UI tests for it.

## Approach

Split by area so new tests don't overlap:

1. Data layer: DAOs, migrations, converters, entities, API request models.
2. Logic: models, use cases, uploaders, background sync, utils.
3. Capture and map UI: capture, camera, tree height, tree edit, map, overlays, root host,
   permissions.
4. Account and shared UI: sign-up, user select, profile, messages, settings, dev options,
   org and language pickers, shared `view/` components and dialogs.

Each area's new tests are run with a `--tests` filter; no full-suite sweep.

## Added tests

About 310 new test cases. The 46 new or changed test classes run 499 tests, all passing,
with 2 `@Ignore`d because they expose bugs (below).

- Data layer (89): `ManualMigrationsTest` (migrations 3 to 7 on seeded raw databases),
  `ConvertersEdgeCasesTest`, `DaoConflictAndIntegrityTest` (REPLACE and cascade behaviour, empty
  inputs), `MessagesDaoEdgeCasesTest`, `UploadRequestsJsonShapeTest` (exact JSON keys),
  `MessagesResponsesEdgeCasesTest`.
- Logic (40): `ObjectStorageClientTest`, `di/KoinGraphTest` (real Koin graph resolves),
  `TreeTrackerViewModelFactoryTest`, `RoutesSerializationTest`, plus cases in
  `TreeUploaderTest`, `LocationUpdateManagerTest` and `SnackbarControllerTest`.
- Capture and map UI (77): `TreeMarkerCarouselUiTest`, `CaptureCustomLoadingUiTest`,
  `PermissionDialogsUiTest`, `SyncOverlayProgressUiTest`, `root/RootUiTest`, `TreeButtonUiTest`,
  `WalletItemUiTest`, `DashboardComponentsUiTest`, plus cases in permissions, camera, splash and
  tree height tests.
- Account and shared UI (104): `SettingsScreenUiTest`, `AddOrgScreenUiTest`,
  `DevOptionsStoredValuesUiTest`, `IndividualMessageItemUiTest`, `ImagesUiTest`,
  `LanguageButtonRootUiTest`, plus cases in sign-up, user select, profile, messages, org and
  language picker, and shared view tests.

## Bugs found (app code not changed)

1. `SurveyViewModel` stores answers in a fixed `Array(3)`, so a survey with 4+ questions never
   shows question 4. Ignored test: `SurveyUiTest` "a survey with four questions can be answered
   to the end".
2. `getLatestDeviceConfig` sorts `logged_at` as text, and instants have a variable number of
   fractional digits, so within one second an older config can win. Ignored test in
   `DaoConflictAndIntegrityTest`. `getLatestSyncTimeForWallet` has the same text-sort issue.
3. `PermissionRequest` checks fine and coarse location separately, so a denied location shows two
   identical "open settings" dialogs (three if camera is denied too).
4. `DeleteProfileScreen`: OK on the "account deleted" dialog runs `DeleteUser` again, which can
   send a second admin message.
5. `DeleteProfileScreen` and `ProfileSelectScreen` don't pass `isFromSettings = true`, so back
   goes to Dashboard rather than Settings.
6. `TreeTrackerViewModelFactory` has two branches that would crash (`SplashScreenViewModel` has
   no Koin definition; `IndividualMessageListViewModel` needs a user id). Neither is called.
7. `UploadLocationDataUseCase` calls `ObjectStorageClient.instance()` directly instead of taking
   it as a dependency.
8. Minor: chat sends an empty draft on IME Go; `ArrowButton` exposes no disabled state to
   accessibility; `MIGRATION_3_4` would fail on a 1.2.x tree with no note or location.
