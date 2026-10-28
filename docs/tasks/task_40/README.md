# Task 40 - Expand unit and Compose UI test coverage

## Goal

Thorough unit tests for everything that isn't UI, and Robolectric Compose UI tests for every
screen, shared component and dialog, covering each state they render and each interaction.

## Test setup

- Unit tests run with Android resources (`includeAndroidResources = true`).
  `app/src/test/resources/robolectric.properties` sets a plain `Application`, so
  `TreeTrackerApplication` and Koin never start in unit tests.
- `uitest/ComposeUiTest` is the base class for Compose UI tests: app theme, a real `Navigator`
  over a test back stack, a `SnackbarController`, `string(R.string.x)`, and the Koin instance
  the test started (Compose caches CompositionLocal defaults per JVM, so without it
  `koinInject` would use a closed scope from an earlier test).
- `uitest/` runs only in `testDebugUnitTest`, because `ui-test-manifest` is a debug-only
  dependency.
- detekt's `LargeClass` rule no longer applies to test sources.

## Coverage added

- Navigation: the org-configured setup and capture flow controllers end to end, Navigator edge
  cases, RouteRegistry, OrgDeepLink, `HandleUIEvents`, the screen-tracking nav entry decorator.
- ViewModel plumbing: `BaseViewModel`, UI events, `TextRef`, `SnackbarController`.
- Every ViewModel: each action, state transition and emitted event, with navigation events run
  against a real Navigator.
- Persistence: every DAO and the messages DAO on in-memory Room, migrations from schemas 7
  and 8, `Preferences` and pref keys, the dev configurator.
- Sync: `SyncDataUseCase` step order and failures, every uploader, all use cases, the messages
  network layer, upload request serialization, `TreeSyncWorker`, the sync notification.
- Domain: tree capture, sessions, step counter, device orientation, convergence, location
  capture, language switching, analytics, image utilities, validation.
- Orgs and users: `OrgRepo` lifecycle and remote config parsing, feature resolution, install
  referrer, `UserRepo`.
- Compose UI tests for every screen, the shared `view/` components, the dialogs and the debug
  overlays. Root screens with navigation logic are also tested with real ViewModels.

## Main-code changes (behaviour-preserving)

- Extracted stateless composables where a screen mixed ViewModel and UI: `UserSelectContent`,
  `TreeCapture`, `Selfie`, `TreeHeight`, `MapContent`. CameraX and MapLibre are passed in as
  slots so the rest of the screen renders under Robolectric.
- `UserSelect`, `UserSelectScreen`, `TreeHeightScreen`, `TreeListScreen` and `TreeDetailScreen`
  take their ViewModel as a defaulted parameter.
- Test tags on buttons built on `TreeTrackerButton` (arrows, approve/decline, info, capture,
  add, user image, fake trees, tree height colours), which expose no semantics of their own.

## Bugs found (not fixed)

Found while writing the tests. None has a failing test; tests pin current behaviour where they
touch it. Each should get its own issue.

### Org/user
- FeatureResolver ignores route aliases ("image-review" org never gets forceNote).
- OrgRepo.addOrgFromRemoteConfig stores JSON null walletId as "null" string.
- Config missing a flow falls back to minimal org, losing walletId/version.
- OrgRepo.setOrg with unknown id persists that id before falling back.
- UserRepo.updateUser overwrites phone and email with user.wallet.
- InstallReferrer fetch exceptions (e.g. SecurityException) not caught; propagate into SplashScreenViewModel.bootstrap().
### VM group B
- Validation.cleanPhoneNumber discards replace() results; "+254 712-345678" invalid.
- SignupViewModel UpdateEmail only checks contains('@'); Credential.Email uses regex.
- SignupViewModel falls back to wallet "DEFAULT" when selected credential field empty.
- SurveyViewModel: answers fixed size 3 (>3 questions crash); unanswered question crashes requireNoNulls; actions before load hit lateinit.
- AddOrgViewModel ApplyOrgAutofill NPE when previousOrgName null.
- `!!` NPE risks: AddOrg/AddWallet init user!!, Chat/IndividualMessageList currentUser!!, Survey user!!, Chat SendMessage before user loads.
- UserSelectViewModel DeleteUser with no selection calls deleteUser("").
- ChatAction.NavigateBack / SurveyAction.NavigateBack are dead.
### VM group A
- DevOptionsViewModel.updateParam: indexOf on stale Config → -1 → IndexOutOfBounds after putValue already ran (safe from the screen today).
- TreeDetailViewModel.SaveNote mutates TreeEntity in place; StateFlow sees equal value, doesn't emit.
- TreeDetailViewModel.DeleteTree deletes file on Main.
- TreeCaptureViewModel timeout doesn't cancel pending pinLocation; late result overwrites state.
- SessionNoteViewModel init user!!; nav bypasses event pattern.
- TreeHeightSelectionViewModel stores "-1" for unknown colour.
- DashboardViewModel treesToSyncHelper param unused.
### Navigation
- Flow controllers increment index before throttledNavigate; a dropped (double-tap) navigation still advances the index → later steps skipped / setup ends early.
- navFromNewUserCreation on last step leaves SignupFlowRoute on the stack under TreeCaptureRoute.
- CaptureFlowNavigationController.goToUserSelect pops Dashboard inclusive → stack [UserSelectRoute].
- goToDashboard/goToUserSelect end the session even when the throttled navigation is dropped.
- CaptureFlowScopeManager.getData() returns the scope, not CaptureFlowData (naming).
### Persistence
- getLatestDeviceConfig / MessagesDAO MAX(composed_at) sort ISO text; fractional seconds sort wrong.
- PrefKey.plus drops user scoping when a UserPrefKey is followed by a plain child key.
- Preferences.clearPrefKeyUsage matches by substring.
- UserPrefKey before setUserId writes to ".../null".
- DatabaseConverters.createMessageFromEntities NPE for SURVEY without survey row.
- Existing TreeTrackerDaoTest mutates FakeFileGenerator singletons (order-dependent) and has assertNotEquals vs Unit (always pass).
### UI
- TreeTrackerButton: no click/role/disabled semantics (invisible to accessibility); `modifier.apply { aspectRatio(1f) }` result discarded so Circle buttons get no aspect ratio; InfoButton ignores shape.
- ChatScreen Messages(): passes checkChatAuthor(index, true) for both first and last flags.
- TreeEditUserSelectScreen / UserButton show "Ada null" when lastName null.
- BorderedTextField(autofocusEnabled = true) with default FocusRequester.Default throws.
- CustomDialog / TutorialDialog onDismissRequest = {} (back/outside tap do nothing).
- PermissionRequest calls handleAction + enableLocation (startActivity) during composition, on every recomposition, twice.
- WalletSelectScreen NavigateForward checks state.currentUser not selectedUser.
- DeleteProfileScreen: approving "Account deleted" dialog dispatches DeleteUser again; ACCOUNTDELETEDANDADMINREQUESTED never set.
- SignUpScreen ExistingUserSelected treats isTherePowerUser == null as true → CaptureSetupScopeManager.getData() may throw.
- Dashboard org button commented out; OrgRoute unreachable from Dashboard.
- ProfileScreen camera callback mutates state.selectedUser?.photoPath directly.
### Sync
- PlanterUploader.uploadUsers: user.photoUrl!! NPE when image upload returned null → whole sync aborts (sessions/trees/locations not uploaded).
- SessionUploader and UploadLocationDataUseCase upload empty bundles every sync (no empty-list guard).
- UploadLocationDataUseCase swallows all exceptions → false; SyncDataUseCase ignores result, reports success.
- SyncDataUseCase catches CancellationException as Exception; after cancel, skipped steps still marked SUCCESS.
- TreeSyncWorker.doWork: if execute threw, IS_SYNCING never reset / progress job not cancelled.
- CreateFakeTreesUseCase.createLegacyTree passes planter info id as planterCheckInId (debug only).
- Message decoding: explicitNulls=true → a missing nullable field or unknown type fails the whole page; wallet skipped.
### Domain
- SessionTracker.createSession ignores CaptureSetupData.destinationWallet; orgRepo walletId fallback unreachable.
- LocationDataCapturer.lastConvergenceWithinRange aliases currentConvergence; mutated in place after converging.
- DeviceOrientation.takeSnapshotAndDisable stores live rotationMatrix array, not a copy.
- ImageUtils.orientImage on a missing path creates the file instead of skipping it.
- ImageUtils.printImage(IntArray) off by one row / out of bounds; decodeBitmap exif!! after catching IOException; resizeImage/orientImage leak FileOutputStream.
- StepCounter.absoluteStepCount reads -1 not null; "-1" stored with no sensor.
- TreeCapturer.pinLocation sets newTreeUuid even on failure and keeps previous convergence.
