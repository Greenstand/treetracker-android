# Task 25 - Codebase audit

## Goal

Read-only audit of the whole app (2026-09-30, `master` @ 949d9624): correctness,
data integrity, security/privacy, capture pipeline, UI/navigation, CI/release,
tests, and docs. No production code was changed.

## Method

Six parallel reviewers each took one area (security & build, data & sync,
capture & sensors, UI & navigation, API/messages/orgs, tests/CI/docs) and
reported only findings they had confirmed by reading the code. Every Critical
finding, and most High ones, were then re-read directly in source.

Legend: **✔** re-verified in source during consolidation · **◐** found
independently by two reviewers · unmarked = verified by one reviewer only.
`TT/` = `app/src/main/java/org/greenstand/android/TreeTracker/`.

## Top priorities

Two themes stand out: **field data can silently stop syncing** (C1, C2, C5,
H6, H7), and **the Play release path cannot ship** (C6). Suggested order:

1. C1 + C2 + H6: make tree/user upload fail per record, not per batch, and
   count "remaining" by `uploaded = 0`.
2. C5: catch message-upload failures so they can't crash the UI or abort
   tree sync.
3. C3: restore `AutoMigration(7, 8)` and add a `MigrationTestHelper` test.
4. C4 + H8 + H9: flow-scope and back-stack fixes from the Nav3 migration.
5. C6: release signing config + fix the deploy artifact name.
6. H1 + H2: stop public-read ACLs and PII in Firebase.

---

## Critical

**C1. One bad tree blocks its whole 50-tree batch forever, and sync still reports success.** ✔
`TT/models/TreeUploader.kt:78-91`, `TT/usecases/SyncDataUseCase.kt:111-128`
- Each 50-tree window is all-or-nothing. A single image failure (missing file,
  `photoPath!!`, `?: error("No imageUrl")`) is caught by
  `catch (e: Exception) { Timber.e("NewTree upload failed") }`.
- `treeUpload` then sees `treeIds.containsAll(remainingIds)`, logs, and calls
  `completeStep`, so the worker returns success.
- Every later sync puts the same trees in the same first window, so the other
  49 trees never upload.
- **Fix:** bundle the trees that did upload an image, and track per-tree
  failure/retry. Report partial failure. Rethrow `CancellationException`.

**C2. The dashboard and Sync button count "image uploaded" as "synced".** ✔
`TT/database/TreeTrackerDAO.kt:180-184`, `TT/dashboard/TreesToSyncHelper.kt:28`, `TT/dashboard/DashboardViewModel.kt:156-163`
- "Remaining" is `COUNT(*) WHERE photo_url IS NULL`.
- If the image uploads but the bundle PUT fails (swallowed by C1), trees have
  `photo_url` set and `uploaded = 0`. The dashboard shows 0.
- In non-debug builds, Sync then refuses to run ("nothing to sync"). A field
  worker who trusts the dashboard may hand the phone off with data still on it.
- **Fix:** count `uploaded = 0` for both `tree` and `tree_capture`.

**C3. No database upgrade path from schema 7.** ✔
`TT/database/AppDatabase.kt:59-62, 94-99`
- `AutoMigration(from = 7, to = 8)` was replaced, not supplemented, by 8→9 in
  7948ac51. Schema 7 was the production schema per d820c63e.
- There is no destructive fallback, so any install still on schema 7 throws
  `IllegalStateException` on first DB access, on every launch. Its unsynced
  trees are stuck.
- There are no migration tests anywhere, and `5.json` is missing from `app/schemas`.
- **Fix:** re-add `AutoMigration(7, 8)`; the 7→8 diff is additive. Wire
  `schemas` into test assets and add a Robolectric `MigrationTestHelper`
  test for 3→9.

**C4. Crash after tapping the avatar on the capture screen, then picking a user (Nav3 regression).** ✔ (code path; Nav3 retention behavior confirmed from library source, not reproduced on device)
`TT/navigation/CaptureFlowNavigationController.kt:86-91`, `TT/userselect/UserSelectViewModel.kt:87-107`, `TT/navigation/CaptureSetupNavigationController.kt:52`
- Setup completion closes `CaptureSetupScope`. `goToUserSelect` then turns the
  stack `[Dashboard, UserSelect, …, TreeCapture]` into `[UserSelectRoute]`.
- Entries use the default content key (`toString()` = `"UserSelectRoute"`),
  so Nav3 keeps the old entry's ViewModel store. `UserSelectViewModel.init`,
  which calls `CaptureSetupScopeManager.open()`, does not run again.
- `SelectUser` then calls `getData()` and throws "CaptureSetupScope not open".
  The same key reuse means Dashboard's `init` side effects (e.g. stopping GPS)
  don't re-run on `goToDashboard`.
- **Fix:** give each push a unique content key. Open flow scopes from the
  screen or controller, not from ViewModel `init`.

**C5. A message upload failure is uncaught: it crashes the UI and aborts tree sync.** ✔
`TT/models/messages/MessagesRepo.kt:158`, `TT/usecases/SyncDataUseCase.kt:58-60`
- `messageUploader.uploadMessages()` runs outside the per-wallet try/catch, so
  an S3/Cognito error propagates.
- In `DashboardViewModel.syncMessages` and in `UserSelectViewModel.deleteUser`
  (which queues a "delete my account" message and syncs with no connectivity
  check), it crashes the app.
- In a full sync, messages are the first step, so users, sessions, trees and
  locations never upload.
- **Fix:** catch inside `syncMessages` and return a result. Consider moving
  messages after trees.

**C6. The Play release pipeline cannot produce a shippable build.** ✔
`app/build.gradle` (no `signingConfigs`; `release` sets none), `.github/workflows/deploy-play-store.yml:38-44`
- `assembleRelease` produces `app-release-unsigned.apk`. The keystore that CI
  decodes is never referenced.
- The deploy step uploads `app-release-release.apk`, which can't exist, straight
  to `track: production, status: completed`, with no test gate.
- The workflow has never run. 9 of the last 10 Firebase beta runs failed at
  "Bump version code".
- **Fix:** add `signingConfigs.release` from env, build an AAB, fix the path,
  run tests first, and deploy to internal or staged rollout.

## High

**H1. Planter PII bundles and all photos are uploaded to S3 as public-read.** ✔
`TT/api/ObjectStorageClient.kt:110-111, 145-146`
- `acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)` is applied to
  images and to bundles.
- Registration bundles contain name, phone, email and GPS
  (`TT/models/PlanterUploader.kt:166-177`). Image keys also embed lat/long.
- Exposure depends on bucket Block Public Access settings, so check those.
- **Fix:** drop the ACLs, serve via signed URLs or a CDN, and enable BucketOwnerEnforced.

**H2. Raw phone and email are sent to Firebase Analytics; the wallet (phone/email) is the Crashlytics user ID.** ✔
`TT/analytics/Analytics.kt:83-92`, `TT/analytics/ExceptionDataCollector.kt:43-45`, `TT/models/SessionTracker.kt:67-72`
- Free-text session notes, ANDROID_ID and precise lat/long are also sent.
- This violates the Google Analytics no-PII policy and GDPR data minimisation.
- **Fix:** remove the phone/email params, use an opaque ID, and drop notes.

**H3. After a GPS timeout, the tree is pinned to a stale location window.** ✔
`TT/models/location/LocationDataCapturer.kt:152, 182-195, 203-207`
- `turnOnTreeCaptureMode()` resets only the status and UUID. The deque and
  `lastConvergenceWithinRange` are cleared only at session end.
- If fixes stop under canopy, `converge()` times out, `isLocationCoordinateAvailable()`
  is still true, and the tree is saved at the last good fix, possibly hundreds
  of meters away. Nothing on the record marks this.
- **Fix:** timestamp samples and require fresh fixes since capture start.
  Persist convergence status and accuracy on the tree.

**H4. Surveys with more than 3 questions crash.** ✔
`TT/messages/survey/SurveyViewModel.kt:59`
- `Array<Int?>(3)` is indexed by `currentQuestionIndex`, so question 4 throws
  ArrayIndexOutOfBounds.

**H5. The message sync cursor permanently skips server messages.** ✔
`TT/models/messages/database/MessagesDAO.kt:42`
- `since = MAX(composed_at)` includes locally composed replies, stamped with
  device (or stale GPS) time.
- A server message sent before the user's offline reply is never fetched. A
  device clock set ahead skips everything.
- Related: saves are parallel with no `@Transaction`, so a partial save can
  leave a SURVEY row whose survey is missing. That crashes the list via
  `surveyEntity!!` (`DatabaseConverters.kt:96`) and never heals.
- **Fix:** keep a per-wallet server-only cursor; save each message and its
  children in one transaction.

**H6. One user whose selfie upload failed aborts the entire sync before trees.** ✔
`TT/models/PlanterUploader.kt:175`
- `imageUrl = user.photoUrl!!`, but `uploadUserImages` leaves `photoUrl` null
  on failure. The NPE aborts the USERS step, then the whole sync (see C5 for
  the ordering). A missing selfie file repeats this forever.
- `SessionUploader.kt:41` has the same hazard: `session.deviceConfigId!!` followed by `!!`.

**H7. A full-row `@Update` from a stale snapshot reverts sync state.** ◐
`TT/treeedit/TreeDetailViewModel.kt:62, 77-78`
- Saving a note writes back the entity loaded when the screen opened. If sync
  uploaded the tree in the meantime, this restores `uploaded = 0`, clears
  `photo_url`, and points `photo_path` at a deleted file.
- The result is a duplicate upload plus a missing file, which triggers C1 for
  49 neighbours. The reverse race exists in `TreeUploader.kt:138`.
- **Fix:** targeted `UPDATE … SET note = :n WHERE _id = :id AND uploaded = 0`.

**H8. Process death during setup or capture crashes on restore.** ◐
`TT/di/AppModule.kt:144`, `AddOrgViewModel.kt:61`, `SessionNoteViewModel.kt:43`, `WalletSelectViewModel.kt:54`
- `rememberNavBackStack` restores deep flow routes, but
  `CaptureSetupScopeManager`, `CaptureFlowScopeManager`, `TreeCapturer` and the
  session ID are memory-only, so `getData()`/`nav` throw.
- A restored `TreeCaptureRoute` also never starts GPS.
- **Fix:** on restore, if the top route belongs to a flow with no open scope,
  reset to Dashboard.

**H9. System back on the review screen desyncs the flow index, so the next tree skips review and `forceNote`.** ◐
`TT/root/Host.kt:117`, `TT/navigation/CaptureFlowNavigationController.kt:48-60`
- Only `TreeCaptureScreen` has a `BackHandler`. Back on review pops the screen
  without `navBackward`, so the next capture jumps past the end and saves
  immediately. The setup flow has the same issue: AddOrg is skipped on the
  second pass.

**H10. The splash screen is a dead end if precise location is denied.** ✔
`TT/splash/SplashScreen.kt:75-93`
- There is no `else` branch, and the request fires once. "Approximate",
  "Deny" or permanent denial leaves the app on the splash image forever.

**H11. Captured-image processing runs on the main thread and can crash.** ✔
`TT/camera/Camera.kt:111-124`, `TT/utilities/ImageUtils.kt:420-457, 535`
- `resizeImage` and `orientImage` (two decodes, two encodes) run on the main
  executor, which risks an ANR on low-end devices.
- A null `decodeFile` causes an NPE. `FileOutputStream` is never closed, and
  the original file is truncated in place. The force-scale path decodes at
  full resolution.
- `view/Images.kt:59-61` (`LocalImage`) also decodes full-size bitmaps on the
  main thread, for every avatar.

**H12. Signup credential bugs.**
- `TT/utilities/Validation.kt:40-47`: `cleanPhoneNumber` discards every
  `replace()` result, so `+254712345678` and `0712 345 678` are rejected. ✔
- `TT/signup/SignupViewModel.kt:160-162, 243-247`: switching the phone/email
  tab keeps `isCredentialValid` true. Next then saves the wallet as
  `"DEFAULT"`, which collides for the next person who does the same.
- Email validity is only `contains('@')`, and input isn't trimmed.
  `SignupViewModelTest` "invalid email" asserts nothing meaningful.

**H13. The only auth on the messages API is the client secret compiled into the APK.**
`TT/models/messages/network/MessagesApiService.kt:27-28`, `app/build.gradle:39,82,99`
- Anyone who decompiles the APK can query messages for any handle, and a
  handle is a phone number or email. This depends on the server trusting
  `apiKey` alone.
- **Fix:** per-user tokens, server-side handle authorization, rotate the secret.

**H14. Check the Play target-API requirement.** `gradle/libs.versions.toml:5` has
`targetSdk = "35"`. If Play's Aug 2026 deadline moved to API 36, as the yearly
pattern suggests, updates are already blocked. Confirm in Play Console;
compileSdk is already 36.

## Medium

Sync & data
- **M1. Failed syncs are never retried.** ✔ `TreeSyncWorker.kt:78` returns `failure()` rather than `retry()`, and there's no CONNECTED constraint. `IS_SYNCING` isn't reset if an exception is thrown.
- **M2. Cancelling between the S3 PUT and the DB write causes duplicates.** Examples: `TreeUploader.kt:203-204`, and the session, user, device-config and message uploaders. **Fix:** `withContext(NonCancellable)`.
- **M3. Editing a user writes the edited wallet into both phone and email; a new selfie never uploads.** `UserRepo.kt:145-146` sets phone and email, but not `wallet`. `photoUrl` isn't reset when `photoPath` changes.
- **M4. Deleting a user drops an unuploaded registration while their trees still upload.** `UserRepo.kt:52`; the selfie file is also leaked.
- **M5. Location upload is one unbounded bundle.** `UploadLocationDataUseCase.kt:43` risks OOM or timeouts after weeks offline. Its `false` result is ignored.
- **M6. `syncMessages` has no mutex.** It's called from Splash, Dashboard, the worker and UserSelect, so overlapping runs upload duplicates and hit ABORT-conflict inserts.
- **M7. The 7 new entity DAOs are unused duplicates.** The DAOs from 949d9624 copy `TreeTrackerDAO` SQL verbatim, and nothing injects them. Fixes to C2 and H7 won't carry over. **Fix:** finish migrating callers, or remove them.

Orgs & deep links
- **M8. Opening an org link offline replaces the stored full config with a minimal one.** `OrgConfigProvider.kt:39-63` + `REPLACE` insert; the walletId and features are lost.
- **M9. The deep link is unauthenticated and the `ORG_LINK` feature flag is never read.** ✔ `FeatureFlags.kt:31`, `OrgDeepLink.kt`, `SplashScreenViewModel.kt:72-84`: any web page can switch the device's org. `singleTask` without `onNewIntent` means warm links are dropped.
- **M10. Org-config problems.**
  - The Remote Config key prefix `org_` ≠ the documented `org_config_{id}`.
  - The default 12 h fetch interval applies.
  - Feature matching ignores route aliases, e.g. `image-review` + `forceNote`, so `forceNote` isn't enforced.
  - Capture routes pass `captureSetupFlow` validation, then crash in `CaptureSetupNavigationController.kt:58`.
- **M11. The wallet-select choice is ignored.** ✔ `SessionTracker.kt:56` uses the non-null `userEntity.wallet`, so `CaptureSetupData.destinationWallet` and the org `walletId` are dead. This was intentional in a73ce43f for the default flow, but `wallet-select` is still accepted from org configs, including the docs' own example.

Messages
- **M12. Decoding is too strict.** `MessageType` has no unknown fallback, and nullable response fields lack `= null` with `explicitNulls = true`. One new server type breaks that wallet's sync on every run.
- **M13. The Announcement `videoLink` can crash the app.** It goes to `startActivity` with no scheme check or catch (`AnnouncementScreen.kt:171-178`).
- **M14. Chat and survey submission issues.** Chat shows queued messages as sent, and empty messages can be queued. A double tap submits a survey twice.

Capture & sensors
- **M15. The sliding-window variance update is inexact, and the error accumulates.** ✔ `Convergence.kt:55-58`: the increment is off by a factor of (n-1)/n and is never recomputed from the deque. It can cause false converged/not-converged results. **Fix:** recompute from the 5-sample deque.
- **M16. Stale capture state.** A stale `pinLocationDeferred` skips convergence on the next capture after a failed `takePicture` (`TreeCaptureViewModel.kt:96`). The `areLocationUpdatesOn` flag is set even when start fails (`LocationDataCapturer.kt:158-160`).
- **M17. Permission composables run side effects in composition.** `Permissions.kt`: `startActivity` runs in composition, and `launchPermissionRequest(); popBackStack()` leaves the session and GPS running.
- **M18. The rotation matrix is never recorded, yet the sensor runs at full rate.** `takeSnapshotAndDisable()` has no callers, so the rotation matrix is never recorded, despite the device-data doc. `DeviceOrientation` meanwhile runs at `SENSOR_DELAY_FASTEST` all session and builds a log string on every event.
- **M19. Orphaned image files are never cleaned up.** They come from rejected reviews, the bad-GPS path, capture errors and rejected selfies.
- **M20. Tree, session and message timestamps can be hours stale.** They come from the last GPS fix: `TimeProvider.kt:26`, and `currentLocation` is never cleared.

UI
- **M21. GPS keeps running after backing out of UserSelect.** This affects Profiles, Edit Trees and Messages. The only stop is in Dashboard `init` (see C4).
- **M22. The Dashboard WorkInfo observer is never removed.** `removeObserver` is called on a new LiveData instance (`DashboardViewModel.kt:230-232`).
- **M23. Adding a profile from Settings → Profiles drops the user into the capture flow.**
- **M24. `TreeTrackerButton` can't be activated with TalkBack.** It uses `pointerInput` with no click semantics, and `ArrowButton` has a null `contentDescription`. This affects every primary button.
- **M25. MapLibre issues.** A click listener is added on every `update`. `onDestroy` runs without `onStop`. The map may crash if restored directly into `MapRoute`.

Security & build
- **M26. Backups include the DB, photos and cached Cognito credentials.** `allowBackup="true"` is set with no extraction rules. ✔
- **M27. The `prerelease` build is debuggable, debug-signed and unminified, but talks to production.** It uses `initWith(debug)`. `beta` is also debuggable.
- **M28. A blanket R8 rule turns shrinking and obfuscation off.** `-keep class * { public private *; }` (`proguard-rules.pro:35-37`). ✔
- **M29. CI never writes the test/dev client credentials.** `setup-property-file/action.yml` omits `test_*`/`dev_*`, so CI beta/debug/dev APKs embed a placeholder secret.
- **M30. CI secret exposure.**
  - `deploy-firebase-beta.yml` checks out any branch with `ADMIN_PAT` and runs that branch's local action, so a write collaborator can exfiltrate the PAT, the keystore and service accounts.
  - Third-party actions that receive credentials are pinned to mutable tags (`@v1`).
- **M31. CI gates are weak.**
  - Only "test and assemble" is a required check.
  - Android lint and `spotlessCheck` never run, and `abortOnError false` is set.
  - About 60 detekt rules are disabled, including SwallowedException and UnsafeCallOnNullableType.

## Low

- `app/build.gradle:37`: the defaultConfig `API_GATEWAY` value is missing its inner quotes. This is masked because every build type overrides it. ✔
- `usesCleartextTraffic="true"` is unnecessary, since all endpoints are https. ✔ The FileProvider's `<external-path path=".">` is unused and too broad.
- There's a committed Fabric `apiSecret` (`app/fab.properties`, `app/fabric.properties`). The legacy `secret_properties.enc` and `decrypt_secret.sh` are still in the repo; delete them and rotate.
- `CODEOWNERS` has no pattern (needs `* @Elforama`). No `permissions:` block is set on most workflows. The jitpack and retired `oss.sonatype.org` repos are unused.
- `AppDatabase`/`MessageDatabase` singletons lack `@Volatile` and double-checked locking.
- Session and location uploaders PUT empty bundles on every sync.
- `SessionTracker.kt:60` has `getLatestDeviceConfig()!!`.
- `BaseViewModel.kt:56` does a non-atomic state update (use `_state.update {}`). Snackbar `replay = 5` replays stale snackbars.
- `goToUserSelect` pops Dashboard inclusive, so back exits the app.
- `TextButton.kt:83-85` discards the result of `.apply { aspectRatio(1f) }`.
- `ChatScreen.kt:240` passes `true` where it should pass `false`. `MessageRequest.parentMessageId` has no `@SerialName`.
- Blur detection is dead code, with Int overflow and an out-of-bounds index if it's ever enabled. Photos are re-encoded twice at quality 70.
- Hard-coded user-facing English strings: Validation messages, "Organization", "Yes"/"No".

## Tests, dependencies, docs

Test coverage gaps (no tests): Room migrations, `TreeSyncWorker`, `ImageUtils`,
`LocationUpdateManager`, `Preferences`, `Validation` (direct), and the new
entity DAOs. The uploader tests cover the happy path only. There is no
`androidTest` source set.

Test hygiene issues:
- `LocationDataCapturerTest` waits a real 1 s for a negative assertion.
- `DashboardViewModelTest` relies on the real `Dispatchers.IO`; 19 main files hard-code dispatchers.
- `LiveDataUtilTest` contains no tests.

Dependencies:
- Firebase BoM 32.8.0 still uses KTX modules, which were removed in BoM 34+.
- The AWS Android SDK 2.16.8 (about 2020) handles all production uploads.
- Coroutines 1.8.1 is paired with Kotlin 2.3.20.
- The multidex dependency is unnecessary at minSdk 23.

Docs drift:
- `docs/process/task-workflow.md` and `docs/tasks/README.md` were deleted in
  30bd11bd, which looks accidental, but CLAUDE.md and `docs/README.md` still
  link to them. Restore with `git show 30bd11bd^:<path>`.
- `docs/engineering/architecture.md:78-101` and `navigation-flows.md` still
  describe Navigation 2.
- `preferences-and-prefkeys.md` says session prefs are cleared on logout, but
  `Preferences.clearSessionData()` has no callers.
- README CI badge is broken. Fastlane docs are stale (fastlane was removed in
  e0b2f0e8). `environment-setup.md` says API 21+. `releases.md` stops at 2.1.
  `data-entities.md` lacks the v2 entities. There is no example keys file for
  contributors.
- `TD.md` is a stale 2023 list that contradicts `detekt.yml`; delete it or file it as an issue.

## Checked and fine

- No `fallbackToDestructiveMigration`, `allowMainThreadQueries`, `runBlocking`
  or `GlobalScope` in main code.
- Entities match `9.json`.
- `enqueueUniqueWork(KEEP)` prevents concurrent sync workers.
- Local images are deleted only after the bundle PUT succeeds.
- HTTP logging is `NONE` outside dev/debug.
- `treetracker.keys.properties` and keystores were never committed.
- Only `TreeTrackerActivity` is exported.
- Photos are stored in app-private storage.
- Koin: ViewModels are not singletons, and no Activity is captured.
- Navigator popUpTo edge cases are correct, and the capture loop doesn't grow
  the stack.
- `HandleUIEvents` top-of-stack guard prevents double advance.
- EXIF orientation matrices are correct.
- Lat/lon ordering is correct end to end.
- CameraX is bound to the per-entry lifecycle and unbinds on leave.
- Remote-config routes are validated against `RouteRegistry` (apart from M10).

## Not done

- No fixes applied; this task is findings only.
- C4 was not reproduced on an emulator. Its code path and Nav3 entry-retention
  behavior were confirmed by reading source.
- S3 bucket policy, Cognito unauth IAM role, server-side `apiKey` handling, and
  Play Console target-API status were not checked; they're outside the repo.
