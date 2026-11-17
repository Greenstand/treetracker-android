# Task 92 - E2E flows: returning user and settings, sad paths, org variants

Issues: [#1397](https://github.com/Greenstand/treetracker-android/issues/1397) (E),
[#1400](https://github.com/Greenstand/treetracker-android/issues/1400) (H) and
[#1395](https://github.com/Greenstand/treetracker-android/issues/1395) (C), from
`docs/testing/e2e-flow-test-plan.md`.

## What was added

Test Buddy flows in `e2e/flows/`, selecting by test-tag ids, with no sleeps.

| Plan | Flow | Asserts |
|---|---|---|
| E1, E2 | `returning-user-relaunch.yaml` | Relaunch goes straight to the Dashboard; switching to Swahili translates it and survives a relaunch |
| E3, E9 | `settings-profile-privacy.yaml` | Privacy dialog opens and closes; profile shows the user's name and phone; back ×3 to the Dashboard |
| E4, E5 | `settings-trees-map.yaml` | Map shows a tree card; editing a note persists; delete cancel keeps the tree, confirm empties the list and the map |
| E6 | `settings-logout.yaml` | Logout cancel and confirm; sign back in with the existing user; back from the Dashboard (asserts the bug) |
| E7 | `settings-delete-user.yaml` | Delete cancel keeps a second user, confirm removes them (checked after a relaunch) |
| E8 | `settings-delete-admin.yaml` | Deleting the admin ends on the language picker after a relaunch |
| H2 | `sad-camera-denied.yaml` | Camera rationale and permanently-denied dialogs; cancel returns to Add org |
| H7 | `sad-process-death-screens.yaml` | Dashboard and Settings come back after process death |
| H8 | `sad-double-tap.yaml` | Double taps on Settings and Edit trees push one screen |
| H9 | `sad-back.yaml` | System back from 15 screens lands where expected |
| H10 | `sad-interrupted-session.yaml` | Killed capture sessions keep the right counts |
| C1 | `org-session-note.yaml` | Session note step after user select |
| C2 | `org-tree-height.yaml` | Tree height step after review, then back to capture |
| C3 | `org-force-note.yaml` | Blank note on approve opens the note dialog |
| C4 | `org-wallet-select.yaml` | Pick a wallet, then add a wallet |

Known-bug flows (tag `known-bug`, skipped by `run.sh`): `sad-location-denied.yaml` (H1),
`sad-process-death-setup.yaml` (H5), `sad-process-death-review.yaml` and
`sad-capture-after-process-death.yaml` (H6), `sad-camera-rationale-next.yaml` (H2),
`sad-back-add-org.yaml` (H9), `sad-double-tap-setup.yaml` (H8).

Scripts: `e2e/seed-org.sh` writes an org with a custom flow into the database (Remote Config is
unreachable offline and the org link carries only id and name); `e2e/run-org.sh` seeds and runs
each org flow. `e2e/run.sh` excludes `org-config` and `known-bug` from its first run and calls
`run-org.sh`.

App change: test tags `CameraRationaleDialog`, `LocationRationaleDialog`,
`PermissionSettingsDialog` and `BadGpsDialog` on those dialogs.

Not covered: C5 (needs Remote Config), C6 (no entry point), H3 and H4 (need host adb to drop
the GPS fix or turn location off).

## Verification

See the PR. Flows were written by three agents on separate API 35 emulators, each passing at
1080x2400 and at 320x640 / density 160, then the merged suite was rerun with `e2e/run.sh`.

## Findings

Finding 6 in the plan: nine bugs, each with a flow that shows it.
