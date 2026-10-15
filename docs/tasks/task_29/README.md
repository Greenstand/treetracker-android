# Task 29 - Fresh nav entry (and ViewModel) per navigate()

Fixes audit finding C4 ([task_25](../task_25/README.md)).
Regression from the Navigation 3 migration ([task_24](../task_24/README.md)).

## Problem

Nav3 keys an entry's ViewModel store and saved state by its `contentKey`, which
defaults to `route.toString()`. Nav3 only clears an entry's ViewModels when
that content key leaves the back stack (`DecoratedNavEntries.PrepareBackStack`
→ `onPop`). A navigation that pops a route and pushes it again in one step
therefore keeps the old entry and its ViewModel, and `init` doesn't run again.
Navigation 2 created a new entry.

The crash: finishing capture setup closes `CaptureSetupScope`. Tapping the
avatar on the capture screen (`goToUserSelect`: `popUpTo<Dashboard>(inclusive)`,
then push `UserSelectRoute`) brings back the old `UserSelectViewModel`, whose
`init` is what calls `CaptureSetupScopeManager.open()`. Selecting a user then
calls `getData()`, which throws "CaptureSetupScope not open". `goToDashboard`
reused `DashboardViewModel` the same way, so its `init` (stop GPS, refresh the
unread messages badge) was skipped.

## Reproduction

`NavEntryIdentityTest` runs a real `NavDisplay` with Host's entry decorators
(saveable state + ViewModel store) and records which ViewModel instance each
screen gets. Before the fix, the `goToUserSelect` and `goToDashboard`
navigations returned the same instance (`Values should be different. Actual: 4`).
The two control cases (pop back, `launchSingleTop` onto the same route) keep
their instance, as they should.

## Changes

- `Navigator`: every `navigate()` gives the pushed route a new id, held in
  `NavEntryIds`, and `contentKeyOf(key)` returns `Route#id`. Initial entries keep
  the plain `Route` key. Popping back, and `launchSingleTop` onto an equal route,
  keep the existing entry, which matches Nav2. No new id is assigned when a
  navigation leaves the stack unchanged, because NavDisplay only rebuilds its
  entries when `backStack.toList()` changes. The comparison uses copies
  (`backStack.toList()`): `NavBackStack` and `SnapshotStateList` compare by
  identity, so comparing the stack itself never detects "unchanged".
- `NavEntryIds` keeps ids only for routes currently on the stack, so its size is
  bounded. It is saved with `rememberSaveable` (`NavEntryIds.Saver`) next to
  `rememberNavBackStack`, so content keys and per-entry saved state survive
  process death.
- `Navigator.withEntryContentKeys(entryProvider)` wraps the entry provider in
  `Host` and `ImageCaptureActivity`, so NavDisplay uses those content keys.
- The throttle origin check (`Navigator.isThrottled`) and `HandleUIEvents`'
  top-of-stack check compare against `topContentKey` instead of
  `topKey.toString()`. The screen-tracking decorator strips `#id` from the
  Crashlytics screen name.

## Tests

- `NavEntryIdentityTest` (Robolectric + Compose; `ComponentActivity` is
  registered by hand because unit tests run without the merged manifest):
  - Re-pushing after popping in the same navigation gives a new ViewModel, and
    so does popUpTo self inclusive. Both failed on `master`.
  - Popping back and `launchSingleTop` keep the same ViewModel.
  - After every step it also asserts that the entry NavDisplay is showing has
    the contentKey `Navigator.topContentKey` reports. A navigation that leaves
    the stack unchanged keeps them in sync. That test failed with an earlier
    version of this change, which compared the stack by identity:
    `expected:<[DashboardRoute#1]> but was:<[DashboardRoute]>`.
- `NavigatorTest`:
  - content-key rules, including the unchanged-stack case on a real
    `NavBackStack` and a `SnapshotStateList`;
  - pruning of ids for routes no longer on the stack;
  - a Saver round trip.

  The origin-scoped throttle tests scope with `topContentKey`, as the app
  does.

## Behavior notes

- Content keys for pushed entries now look like `UserSelectRoute#12`.
  Anything that compares a content key must use `Navigator.topContentKey` or
  `contentKeyOf`, not `toString()`.
- Ids are kept per route string. If a route is pushed while an equal copy is
  still lower in the stack, both copies get the new id. The lower copy's old
  key then leaves the stack, so its ViewModel and saved state are cleared, and
  after popping back it shows the upper copy's state. No normal flow pushes such
  duplicates. The only unthrottled push is the debug/beta logo long-press that
  opens Dev Options, and that can't stack two Dev Options screens. Nav3 didn't
  support duplicate content keys before this either.
- **Selfie retake:** during signup, retaking the selfie (`navigate(SelfieRoute)
  { popUpTo<SelfieRoute>(inclusive) }`) now gives the selfie screen a fresh
  `SignupViewModel`, so the selfie tutorial shows again after every retake. That
  is what Navigation 2 did (the released 2.1.x app). On `master` the tutorial
  doesn't come back. Recorded on the emulator for both builds.
- Going from Delete Profile back to Settings likewise creates a new
  `SettingsViewModel`.

## Device validation

On the emulator I followed the default org flow: sign up → Dashboard → TRACK →
select user → AddOrg → capture screen → tap the avatar → tap the user.

- **`master` (pre-fix):** `FATAL EXCEPTION: main java.lang.IllegalStateException:
  CaptureSetupScope not open. Call open() before getData()` at
  `UserSelectViewModel.handleAction(UserSelectViewModel.kt:107)`.
- **Fixed build** (all five audit fixes; only this one affects navigation): no
  crash (same process), and the setup flow runs again (UserSelect → AddOrg →
  capture screen). Leaving capture returns to a working Dashboard.
- The unchanged-stack desync can't be reached from the UI. The only
  pop-then-push call that could produce it, `navToUserSelect`, runs from
  WalletSelect, so the stack always changes. It is covered by the tests above.
