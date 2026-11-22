# fix(delete-account): deleting a user while offline no longer crashes the app

Fixes #1416.

## What was wrong

Confirming Settings → Delete account with the device offline crashed the app with an
`AmazonClientException`, after the user had already been removed locally.
`UserSelectViewModel.deleteUser` calls `messageRepo.syncMessages()`, and the upload at the end of
it (`messageUploader.uploadMessages()`) is outside that function's try/catch, so the exception
escaped the `viewModelScope` coroutine.

## The fix

`deleteUser` now catches and logs a failed `syncMessages()` (cancellation is rethrown). The delete
is local and already done by then, and the request to the admin is saved with
`shouldUpload = true`, so it goes out with the next sync.

I left `MessagesRepo.syncMessages()` throwing on a failed upload. `SyncDataUseCase` uses that to
report the message step of a sync as failed, and `MessagesRepoTest` pins it.

## Tests

- `UserSelectViewModelTest`: new case where `syncMessages()` throws after a successful delete.
- `settings-delete-user.yaml` (E7) and `settings-delete-admin.yaml` (E8) now assert what the bug
  made impossible: the "Account Deleted" dialog, and that closing it returns to Settings (E7) or
  opens Signup as the only screen, with back exiting the app (E8).
- `docs/testing/e2e-flow-test-plan.md` and `docs/tasks/task_95/README.md` updated.

validates: settings-delete-user settings-delete-admin

## Not verified locally

There is no Android SDK or emulator in the environment this was written in, so nothing here was
compiled or run: not the unit test, not the flows. CI is the first run of both.

Two things to look at in the before/after result:

- The "before" run should fail at the "Account Deleted" assertion or just after it. If the crash
  on the base app comes later than the flow's next taps, "before" could pass and the verdict would
  read "does not demonstrate the change".
- E8's "back from Signup exits the app" step is new and has never been run.

## Not in this PR

`SplashScreenViewModel` and `DashboardViewModel` call `syncMessages()` without a try/catch too.
Both check for a connection first, so they are safe offline, but a failed upload while online
would still crash there. That is why the e2e run stays offline.

## 📸 Proof of change

### 🎥 Screen recording / video

None attached: no device was available. The "Affected flows" workflow records both flows on the
base app and on this branch and posts the videos on the PR.

### 🖼️ Screenshots

- [x] This change has **no user-visible / UI effect** beyond the app not crashing; no layout or
  text changed.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
