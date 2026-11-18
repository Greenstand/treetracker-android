# End-to-end flow test plan

Audit of this repo at `master` 54b6632 (Nov 4, 2026, after the Test Buddy CI PR #1391).
Goal: a black-box flow suite that catches broken contributor PRs and proves nothing regresses during the KMP/CMP move.

## 1. Navigation map

Single activity (`TreeTrackerActivity`, `singleTask`, portrait) hosting a Navigation 3 back stack (`root/Host.kt`, routes in `navigation/Routes.kt`), plus `ImageCaptureActivity` for the selfie camera.

29 routes:

| Area | Routes | Entry points |
|---|---|---|
| Start | `SplashRoute(orgId, orgName)` | Launcher, org deep link `app://mobile.treetracker.org/org?id=&name=` (cold start only) |
| First run | `LanguageRoute(isFromTopBar=false)` → `SignupFlowRoute` | Splash when no admin ("power user") exists |
| Signup (one route, internal steps) | privacy dialog → credential (phone/email) → name → selfie (`ImageCaptureActivity`: `SelfieRoute` → `ImageReviewRoute`) | Language, UserSelect "add user", Settings logout, Delete profile (admin) |
| Home | `DashboardRoute` | Splash (admin exists), Signup (first user) |
| Top bar | `LanguageRoute(isFromTopBar=true)`, `OrgRoute`, `SettingsRoute`, DevOptions (long-press logo, debug/beta) | Dashboard, Signup |
| Capture setup (org config, default = user-select → add-org) | `UserSelectRoute`, `AddOrgRoute`, optional `WalletSelectRoute` → `AddWalletRoute`, `SessionNoteRoute` | Dashboard "capture" (sync reminder dialog if ≥2000 unsynced) |
| Capture loop (org config, default = capture → image review) | `TreeCaptureRoute`, `TreeImageReviewRoute`, optional `TreeHeightScreenRoute`, force-note dialog | End of setup; loops back to capture after each save |
| Messages | `MessagesUserSelectRoute` → `IndividualMessageListRoute` → `ChatRoute` / `SurveyRoute` / `AnnouncementRoute` | Dashboard messages |
| Settings | `ProfileSelectRoute` → `ProfileRoute`, `TreeEditUserSelectRoute` → `TreeListRoute` → `TreeDetailRoute`, `MapRoute`, `DeleteProfileRoute`, logout dialog, privacy dialog | Dashboard settings |

Flow order is data driven: `OrgRepo` stores `captureSetupFlow` and `captureFlow` per org, `RouteRegistry` maps strings to routes and drops unknown ones, `CaptureSetupNavigationController` / `CaptureFlowNavigationController` walk them. Org features (`forceNote`) are per route.

Runtime dialogs that change the path: permission request at splash (camera, location, activity recognition, notifications), permission rationale / permanently denied dialogs on capture, location-off redirect to system settings, bad GPS dialog (retry / exit), capture and review tutorials (first tree only), existing-user dialog in signup, sync reminder dialog, logout and delete dialogs, survey back-on-first-question.

Docs that describe flows: `docs/engineering/navigation-flows.md`, `docs/product/add-tree-flow.md`, `docs/product/org-links.md`, `docs/engineering/upload-business-logic.md`, `docs/testing/app-distribution-testing.md` (manual QA checklist), `docs/testing/agent-app-functions.md`.

## 2. What exists today

- End to end: one Test Buddy flow, `e2e/flows/first-screen.yaml` (fresh install lands on the language picker), run on an API 35 emulator in `.github/workflows/android-e2e.yml`.
- Robolectric multi-screen: `uitest/root/RootUiTest.kt` renders the real `Host`; covers org load → splash and org deep link → language select. Nothing past that.
- Robolectric per-screen Compose tests: ~85 files under `app/src/test/.../uitest/` cover nearly every screen in isolation (stateless composables, fake navigator).
- Unit: `NavigatorTest`, `FlowNavigationControllerTest`, `CaptureSetupNavigationControllerTest`, `CaptureFlowNavigationControllerTest`, `RouteRegistryTest`, `OrgDeepLinkTest`, `RoutesSerializationTest`, scope manager tests.
- `app/src/androidTest/` is empty. Open issue #1243 asks for instrumentation tests of login, capture, sync and dashboard.
- Seeding helpers: debug AppFunctions `createAccount` and `captureTrees` (need API 37; CI emulator is API 35).
- Since this audit, #1402 gave every screen stable test tags (`TestTags`) and added a manual `tb` screen tour (`e2e/tour/`).

Gap: no test walks screen to screen past the language picker, so a PR that breaks a hand-off between screens (wrong popUpTo, scope not opened, org flow misconfigured) passes CI.

## 3. Proposed flows

Priority: P0 = run on every PR (smoke, keep the set short), P1 = every PR if time allows or nightly, P2 = nightly / pre-release.
"Covered" = whether anything end to end covers it today.

### A. First run and signup (P0)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| A1 | First run to dashboard (phone) | Fresh install → grant permissions → pick English → dismiss privacy dialog → phone → first/last name → selfie → accept → Dashboard shows 0 trees | The front door; touches splash bootstrap, language, signup, ImageCaptureActivity result, DB write | Yes (`signup-phone.yaml`) |
| A2 | First run with email | Same as A1 with email credential | Second credential path, different validation | Yes (`signup-email.yaml`) |
| A3 | Signup validation | Invalid phone/email keeps Next disabled; names with digits/symbols show errors; fix and continue | Validation regressions are common in contributor PRs | Yes (`signup-validation.yaml`) |
| A4 | Selfie retake and cancel | In selfie: capture → retake → capture → accept; then repeat signup and back out of camera → still on signup, no user created | Activity result null path | Yes (`signup-selfie-retake.yaml`) |
| A5 | Existing identifier | With a user already on device, sign up again with the same phone → existing-user dialog → pick user → goes to Dashboard (no admin) or continues capture setup (admin exists) | Shared-device core behaviour | Yes (`signup-existing-user.yaml`) |
| A6 | Back from signup | Language → Signup → back → Language; credential step back | Back stack correctness | Yes (`signup-back.yaml`) |

### B. Capture (P0)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| B1 | Capture first tree (default org) | Dashboard → Capture → select user → Next → Add org (keep default) → Next → capture tutorial → capture photo (GPS fixed) → review tutorial → approve → back on capture screen → back → Dashboard shows 1 to sync | The core job of the app; walks both flow controllers, session start/end, tree save | Yes (`capture-first-tree.yaml`) |
| B2 | Capture loop | From B1 capture 3 trees in a row without leaving | Loop back via `popBackStackTo<TreeCaptureRoute>`; count must be 3 | Yes (`capture-loop.yaml`) |
| B3 | Reject photo | Capture → review → reject/back → capture screen, count unchanged | `navBackward` path, no tree saved | Yes (`capture-reject.yaml`) |
| B4 | Add note on review | Capture → review → add note → approve; note visible in Settings → Edit trees → detail | Note persistence end to end | Yes (`capture-note.yaml`) |
| B5 | Exit paths from capture | System back, top-bar back, profile icon (→ user select) | Each ends the session; profile icon goes to UserSelect not Dashboard | Yes (`capture-exit.yaml`) |
| B5b | Profile exit, then setup again | Capture → profile icon → user select → pick user → Next → Add org → capture | User select opens a fresh setup scope after capture closed it | Yes (`capture-exit-profile-next.yaml`) |
| B6 | New user mid setup | Dashboard → Capture → "+" add user → full signup → lands on Add org (not dashboard) → capture | `navFromNewUserCreation` and its popUpTo | Yes (`capture-new-user.yaml`) |

### C. Org-configured flow variants (P1, key for KMP)

Seed each variant with an org deep link carrying the config (see the test link in `OrgRepo.kt`).

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| C1 | Session note setup step | Org with setup `user-select → session-note` → note → capture | Optional screen only reachable by config | Yes (`org-session-note.yaml`) |
| C2 | Tree height step | Org with capture `capture → image-review → tree-height` → pick colour → loops | Optional capture step | Yes (`org-tree-height.yaml`) |
| C3 | Force note | Org with `forceNote` on image review → approve with blank note → dialog → add note → continues | Org feature gate | Yes (`org-force-note.yaml`) |
| C4 | Wallet select / add wallet | Org with `wallet-select` → pick wallet or add wallet → continue | Legacy branch, easy to break silently | Yes (`org-wallet-select.yaml`) |
| C5 | Unknown route in config | Org config with a bad route name → app drops it and flow still works | `RouteRegistry` validation | Unit only (see finding 6) |
| C6 | Switch org from dashboard | Two orgs linked → Dashboard org button → pick other org → capture uses that org's steps | Org picker plus per-org flow | No (no entry point, see finding 6) |

### D. Sync and upload (P1)

Run against a test backend or fake; assertions on dashboard counters and snackbars.

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| D1 | Sync happy path | Capture 2 trees → online → tap sync → "sync started" → counts move from to-sync to synced | Data actually leaving the device is the product | No |
| D2 | Nothing to sync | Release-like build, 0 trees → tap sync → "nothing to sync" | Snackbar branch (skipped in debug builds, so needs a flag or beta build) | No |
| D3 | Stop sync | Start sync with many trees (`captureTrees` 50) → tap again → stops, partial counts kept | Cancel path | No |
| D4 | Offline sync | Airplane mode → sync → failed/blocked message, trees still pending → go online → sync succeeds | Field reality: poor connectivity | No |
| D5 | Sync reminder | ≥2000 unsynced trees → tap capture → reminder dialog → confirm → user select | Dialog gate before capture | No |

### E. Returning user and settings (P1)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| E1 | Cold start returning user | Relaunch with admin present → splash → Dashboard (no language/signup) | Splash routing | Yes (`returning-user-relaunch.yaml`) |
| E2 | Change language | Dashboard language button → pick another language → back on Dashboard with translated text → relaunch, language kept | Top-bar language path plus persistence | Yes (`returning-user-relaunch.yaml`) |
| E3 | Profiles | Settings → Profile → pick user → profile shows their details → back ×2 | Read-only path | Yes (`settings-profile-privacy.yaml`) |
| E4 | Edit and delete tree | Settings → Edit trees → user → tree list → detail → change note → save → delete → dialog → gone from list | Only place trees are edited | Yes (`settings-trees-map.yaml`) |
| E5 | Map | Settings → Map with trees → markers and carousel; with zero trees → empty state | Map rendering regressions | Yes (`settings-trees-map.yaml`, carousel cards only) |
| E6 | Logout | Settings → Logout → confirm → Signup is the only screen; sign back in with existing identifier → Dashboard; back from Dashboard exits app | See finding 2 | Yes (`settings-logout.yaml`, `settings-logout-back.yaml`) |
| E7 | Delete non-admin user | Settings → Delete account → pick non-admin → confirm → "deleted" → back on Settings | Delete branch 1 | Partly (`settings-delete-user.yaml`, see finding 6) |
| E8 | Delete admin | Same for the admin → ends on Signup with clean stack | Delete branch 2 | Partly (`settings-delete-admin.yaml`, see finding 6) |
| E9 | Privacy policy | Settings → privacy dialog → close | Cheap, often broken by dialog refactors | Yes (`settings-profile-privacy.yaml`) |

### F. Messages (P2)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| F1 | Empty inbox | Dashboard → Messages → user → "no messages" state → back | Empty state | No |
| F2 | Direct chat | Seeded chat → open → send a message → appears → back | Only outgoing write in messaging | No |
| F3 | Survey | Seeded survey → answer each question → submit → returns to list; back on first question exits | Multi-step with its own back logic | No |
| F4 | Announcement | Seeded announcement → open link → external browser → return | External intent | No |
| F5 | Unread badge | New message → badge on Dashboard and on user → open → badge clears | Notification state | No |

### G. Org links (P1)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| G1 | Cold start link, online | Launch via `app://mobile.treetracker.org/org?id=…&name=…` → remote config applied → org name on dashboard | Main org onboarding route | Robolectric only (to language) |
| G2 | Cold start link, offline | Same in airplane mode → minimal org with defaults, capture still works | Offline requirement | No |
| G3 | Malformed link | Missing id, wrong host → app starts normally | Defensive parsing | Unit only |
| G4 | Link while app running | App in background → open link | See finding 4 | No |

### H. Sad paths and resilience (P0 for H1–H3, rest P1)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| H1 | Location denied at first launch | Fresh install → deny location | See finding 1 | Yes (`sad-location-denied.yaml`) |
| H2 | Camera denied on capture | Revoke camera → Dashboard → Capture → rationale dialog → cancel returns; "don't ask again" → settings dialog | Permission branches on capture | Yes (`sad-camera-denied.yaml`, `sad-camera-rationale-next.yaml`) |
| H3 | No GPS fix | No mock location → capture → bad GPS dialog → retry, then exit → Dashboard | Field reality, timeout path | Per-screen only (needs host adb to remove the GPS fix) |
| H4 | Location services off | Location toggle off → capture → redirected to system location settings → enable → back to capture | `enableLocation()` redirect | No (needs host adb to turn location off) |
| H5 | Process death mid setup | On Add org (or session note) → background → `am kill` → reopen | See finding 3 | Known bug (`sad-process-death-setup.yaml`) |
| H6 | Process death mid capture | On image review → background → kill → reopen → tree not duplicated, can capture again | Same scope issue, data integrity | Known bug (`sad-process-death-review.yaml`, `sad-capture-after-process-death.yaml`) |
| H7 | Process death on dashboard/settings | Background → kill → reopen → same screen, no crash | Nav3 saved back stack | Yes (`sad-process-death-screens.yaml`) |
| H8 | Double taps | Double-tap Capture, Next, Settings → only one screen pushed | `Navigator` throttle | Yes (`sad-double-tap.yaml`); known bug (`sad-double-tap-setup.yaml`) |
| H9 | Back everywhere | From every screen, system back lands where expected (table in tests) | Nav3 migration and module split risk | Yes (`sad-back.yaml`, 15 screens); known bug (`sad-back-add-org.yaml`) |
| H10 | Interrupted session | Kill during capture → relaunch → dashboard counts correct | `wasSessionInterrupted` path | Yes (`sad-interrupted-session.yaml`) |

### I. Dev tools (P2)

| ID | Flow | Steps | Why | Covered |
|---|---|---|---|---|
| I1 | Dev options | Debug build → long-press logo → dev options → change a value → back | Hidden but used by testers | Per-screen only |
| I2 | Debug overlays | Toggle sync and sensor overlays during capture and sync | Debug-only UI | Per-screen only |

## 4. Findings worth confirming (from reading the code, not run)

1. Splash only advanced when location was granted (`SplashScreen.kt`, `onResult`). If the user denied, the app sat on the splash image with no message; after "don't ask again" it never moved. Fixed in #1417: splash now moves on whatever the user answers, and capture starts GPS once location is granted there.
2. After logout or deleting the admin, the stack is cleared to `SignupFlowRoute`. Signup then navigated to Dashboard with `popUpTo<LanguageRoute>`, which was not on the stack, so Signup stayed under Dashboard and back from Dashboard returned to Signup. Fixed in #1415 for both paths: Signup clears the whole stack when it goes to the Dashboard. E6 and `settings-logout-back.yaml` check the logout path; E8 can't check the delete-admin path yet (offline crash, #1416).
3. `CaptureSetupScopeManager` and `CaptureFlowScopeManager` are in-memory singletons, but the Nav3 back stack survives process death. Restoring onto Add org calls `CaptureSetupScopeManager.getData()` before any `open()`, which throws. H5 likely crashes; H6 may too.
4. The org deep link is parsed only at cold start and the activity is `singleTask` with no `onNewIntent`, so a link tapped while the app is alive is ignored. Decide whether that's intended before writing G4.
5. Seen while writing the A and B flows (tb, API 35):
   - Next on the credential step is never disabled. An invalid phone or email shows an error snackbar and stays on the step. Names drop digits and symbols as they are typed, so the name errors only show for fewer than 2 characters. A3 asserts this. Issue #1413.
   - System back on the name step goes to the language picker; only the on-screen back arrow returns to the credential step. A6 asserts today's behaviour; change it if back should step back. Issue #1412.
   - The capture screen's profile icon opens user select with nothing under it: system back there closes the app instead of returning to the Dashboard. Not asserted by a flow yet. Issue #1411.

6. Seen while writing the C, E and H flows (tb, API 35). Flows tagged `known-bug` assert the
   fixed behaviour, so they fail today; `e2e/run.sh` skips them.
   - Org config only arrives through Firebase Remote Config. The org link reads only `id` and
     `name` (`OrgDeepLink.kt`); the inline `params={...}` link in `OrgRepo.kt` is not implemented.
     The C flows seed the org into the database with `e2e/seed-org.sh`. C5 needs Remote Config
     (`validateOrgRoutes` runs only there). C6 has no entry point: the Dashboard org button is
     commented out (`DashboardScreen.kt`).
   - After logout and signing back in, back from the Dashboard returned to Signup (finding 2
     confirmed). Fixed in #1415: Signup now clears the whole stack when it goes to the Dashboard.
     `settings-logout.yaml` and `settings-logout-back.yaml` check that back then exits the app.
   - Deleting any user while offline crashed the app: `UserSelectViewModel.deleteUser` called
     `messageRepo.syncMessages()`, whose `uploadMessages()` (`MessagesRepo.kt`) is outside the
     try/catch. Fixed in #1416: `deleteUser` now catches the sync failure. `settings-delete-user-offline.yaml`
     and E7 (`settings-delete-user.yaml`) check the "Account Deleted" dialog and the return to
     Settings after deleting a non-admin; E8 (`settings-delete-admin.yaml`) checks the dialog and the
     move to Signup after deleting the admin. Online, the same unguarded upload runs at launch and on
     picking a user; CI's e2e run is offline for that reason.
   - H1: denying location at first launch leaves the app on the splash (finding 1 confirmed) (#1417).
   - H5: process death on Add org crashes on reopen: `CaptureSetupScope not open` from
     `AddOrgViewModel` init (finding 3 confirmed) (#1418).
   - H6: process death on image review crashes on reopen: `CaptureFlowScope not open` while
     creating `TreeImageReviewViewModel` (#1419). Restoring onto the capture screen works, but GPS updates
     are not restarted, so the next capture always shows the bad GPS dialog (#1420).
   - H2: cancelling the camera rationale returns to Add org with the setup scope closed; Next
     there crashes (`AddOrgViewModel` SetDefaultOrg) (#1421). Fixed: the setup scope now stays
     open until the capture flow ends.
   - H9: system back from Add org doesn't step the setup flow back (`navBackward` isn't called),
     so Next on user select then skips Add org (#1422).
   - H8: double-tapping Next on user select opens Add org and then capture; the Navigator
     throttle doesn't stop the second `navForward` (#1423).
   - Each issue says it is fixed only when its known-bug flow passes; the fix PR drops the tag.

## 5. How to build the suite

- Runner: Test Buddy / Maestro YAML in `e2e/flows/`, as today. Black-box flows survive the KMP/CMP rewrite unchanged, which is the point; the same YAML can later run against iOS.
- Selectors: use the ids in `TestTags` (`view/TestTags.kt`, added in #1402), exposed through `testTagsAsResourceId`, never translated text, which breaks when the language changes (E2) or copy changes. Tag any new control there. CMP supports the same tags.
- Seeding: AppFunctions `createAccount` / `captureTrees` skip slow setup, but need API 37 while CI runs API 35. Either bump the CI emulator or add a debug-only seeding path (intent or dev option) usable on 35.
- Device control: GPS via emulator `geo fix` (and no fix for H3), offline via airplane mode / `svc wifi|data disable`, process death via `adb shell am kill` after backgrounding (check tb supports a shell or kill step), camera via the emulator virtual scene.
- Backend: sync flows (D) need a fake or test bucket so PR runs don't write to production.
- Reuse: share a "signed-in admin" subflow (A1) so later flows start from Dashboard.
- Suggested PR smoke set (keep it under ~10 minutes): A1, B1, B2, B3, E1, H1, H3. Everything else nightly and before releases.
