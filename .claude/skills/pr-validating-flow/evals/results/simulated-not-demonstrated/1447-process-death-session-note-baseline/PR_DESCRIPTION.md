# fix(setup): restore session note, wallet select and add wallet after process death

Fixes #1447.

## The bug

In an org whose capture setup has a session note or wallet select step, if Android killed the app in the background on session note, wallet select or add wallet, reopening it from recents crashed:

```
java.lang.IllegalStateException: CaptureSetupScope not open. Call open() before getData().
```

Navigation 3 restores the step's screen, but the capture setup scope only lived in memory and the three ViewModels read it in `init`.

## The fix

Add org had the same crash and its own fix (#1431): it saved the user id when Add org opened and reloaded it there. Copying that into three more ViewModels would have left four restores that each bring back only the user, so I moved it to one place that covers every setup step.

- **Saved where it is written.** `CaptureSetupData` saves itself through a new `CaptureSetupStore` on every change (user id, destination wallet, session note, organization name; one JSON value in preferences, `system-settings/capture-setup`).
- **Restored in one place.** `CaptureSetupScopeManager.getOrRestoreData()` returns the open scope's data, or reopens a closed scope from the saved setup and reloads the user from the database. It returns null when nothing is saved or the user is gone.
- **A new setup starts clean.** `CaptureSetupScopeManager.open()` (user select) forgets the saved setup.
- **The four step ViewModels** (session note, wallet select, add wallet, Add org) get their data from `getOrRestoreData()`. While a restore is loading (`restoring`), Next, Back and input wait. If nothing can be restored (`setupLost`), the screen goes to the Dashboard through a shared `SetupLostEffect`, which is the code Add org already had.

After the fix, the step comes back and Next finishes setup and opens capture. A restored session note shows the note that was typed before the kill.

## Behaviour changes beyond the three screens

- **Add org now uses the shared restore.** Its `system-settings/add-org-user-id` key and its own restore code are removed. A note or org name entered on an earlier step now survives a restore on a later step; #1431 listed losing them as a limit.
- **Restore sets the preferences user id** (`Preferences.setUserId`), as picking the user on user select does. Add org's old restore did not.
- **The session note is written to preferences on each keystroke** (an async `apply()`), since every change to the setup is saved.

## Limits

- **App update between the kill and the reopen:** the old version saved no setup, so a restored setup step falls back to the Dashboard once.
- **Add wallet and wallet select come back empty.** Add wallet has an empty name field and wallet select has no wallet picked, so Next stays disabled until the user enters or picks again. The saved `destinationWallet` can't tell "picked on wallet select" from "typed on add wallet", so I don't show it.
- **Not changed, found while reading:** `SessionTracker.createSession` never reads `CaptureSetupData.destinationWallet`; it uses the user's own wallet. The wallet picked or added in setup doesn't reach the session, with or without process death. That looks like a separate bug and should get its own issue.

## How it was checked

**The fix is not shown working on a device.** This session had no Android SDK or emulator; locally I ran only ktlint (the CLI, with the repo's `.editorconfig`; clean) and the `e2e/ci` Python tests (68 pass).

**CI's before/after run did not demonstrate the change.** "Affected flows" ran both new flows on master's app and on this branch's app:

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `org-process-death-session-note` | passed | passed | does not demonstrate the change |
| `org-process-death-wallet-select` | passed | passed | does not demonstrate the change |

- **What I think happened:** the process survived `killApp` in these flows, so neither app ever restored anything. The base app has no code that could bring a typed session note back after its process dies, yet the flow's check for the note passed on it. This is inferred from the table; I did not have the run's logcat.
- **Why the kill would not take effect is unknown.** These flows start from `seed-org.sh` and a plain `launchApp`; the process death flows that do validate their fixes start from `launchApp: {clearState: true}` and sign up in the flow. I found nothing in the app code that explains it.
- **What the "after" pass does show:** this branch builds, and the three screens still work through setup into capture.
- **I have not seen the result of the unit test, ktlint or detekt checks.** Only the "Affected flows" result came back to me.

**Before merging, someone with a device should run the issue's steps on master and on this branch**, checking with `adb shell pidof org.greenstand.android.TreeTracker.debug` that the process is gone before reopening. The flows are kept, each with a note at the top saying it is not proof yet; if you would rather not carry them until the kill is sorted out, drop them and their two lines in `e2e/run-org.sh`.

validates: org-process-death-session-note org-process-death-wallet-select

- **e2e (new, org-config, listed in `e2e/run-org.sh`):**
  - `org-process-death-session-note.yaml` types a note, kills the app on session note, reopens it, checks the note is back and goes on to capture.
  - `org-process-death-wallet-select.yaml` kills the app on wallet select and again on add wallet, then goes on to capture.
  - Both passed on master's app too (see above), so neither is evidence for the fix yet.
- **Unit tests (new or extended):**
  - `CaptureSetupStoreTest` (new, real preferences)
  - `CaptureSetupScopeManagerTest`, `CaptureSetupDataTest`
  - `SessionNoteViewModelTest`, `WalletSelectViewModelTest`, `AddWalletViewModelTest`, `AddOrgViewModelTest`: restored, still restoring, setup lost
- **UI tests:** `SetupRestoreUiTest` (new) checks that Next and Back wait while restoring and that a lost setup ends on the Dashboard. `AddOrgScreenUiTest` keeps its restore tests, now on the shared restore.
- **Existing tests touched:** tests that build `CaptureSetupData` or stub `CaptureSetupScopeManager` were adapted to the new constructor argument and to `getOrRestoreData()`.

Task notes: `docs/tasks/task_111/README.md`.

## 📸 Proof of change (REQUIRED)

### 🎥 Screen recording / video (always required)

**Not attached.** I could not record one without an emulator. The "Affected flows" check recorded the two flows on both apps, but those videos show the same passing run on each side, not the crash and the fix, so linking them here would overstate what they prove.

### 🖼️ Screenshots (required for any UI change)

- [x] This change has **no user-visible / UI effect** on any screen's layout, so screenshots are not applicable. The change in behaviour is that the app no longer crashes on reopening.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
