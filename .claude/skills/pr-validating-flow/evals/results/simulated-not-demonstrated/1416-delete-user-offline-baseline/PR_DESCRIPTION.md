# fix(delete-account): deleting a user while offline no longer crashes the app

Fixes #1416

## Problem

Settings → Delete account → pick a user → confirm crashed the app when the device was offline, for the admin and for any other user. The user was already deleted locally, so the "Account Deleted" dialog never showed.

`UserSelectViewModel.deleteUser` saves a message asking the admin to delete the account and then calls `messageRepo.syncMessages()` to send it. In there, `messageUploader.uploadMessages()` is outside the try/catch, so offline its `AmazonClientException` escaped `viewModelScope`.

## Fix

`deleteUser` now catches a failure of `syncMessages()` (cancellation is rethrown) and logs it with Timber. The request is already saved locally with `shouldUpload = true`, so the next sync uploads it.

I did not move the catch into `MessagesRepo.syncMessages()`, as the issue's wording might suggest. `SyncDataUseCase` relies on that exception to mark the Messages step failed and report the sync as failed, and `MessagesRepoTest` (`sync messages propagates message upload failures`) asserts it. Swallowing it there would make a failed message upload look like a successful sync.

## Not in this PR

`SplashScreenViewModel.bootstrap` and `DashboardViewModel.syncMessages` call `syncMessages()` with only an internet check in front. They don't crash offline, but an upload failure while online (dummy keys, a dropped connection) can still escape there. That is the reason the e2e run is offline; it deserves its own issue.

## Tests

- `UserSelectViewModelTest`: new test where delete succeeds and `syncMessages()` throws; the state ends as `ACCOUNTDELETEDLOCALLY`.
- `e2e/flows/settings-delete-user.yaml` (E7): now asserts the "Account Deleted" dialog and that closing it returns to Settings, before the existing relaunch check.
- `e2e/flows/settings-delete-admin.yaml` (E8): now asserts the "Account Deleted" dialog, that closing it opens Signup, and that back from there exits the app, before the existing relaunch check.
- `docs/testing/e2e-flow-test-plan.md` and `docs/tasks/task_95/README.md` updated.

validates: settings-delete-user settings-delete-admin

**First CI run: both flows passed on master's app as well as on this branch**, so they did not demonstrate the fix. They closed the "Account Deleted" dialog as soon as it appeared. My reading, not confirmed from a log: the dialog shows before the upload fails, and leaving the screen ends the upload's coroutine, so the flow got past the point where master crashes. Both flows now check the dialog 20 times in a row before closing it, to give the failed upload time to land. There is no further CI result behind this change, so whether the flows now fail on master is unverified; if the before run still passes, the dwell is too short or the crash needs something the CI emulator doesn't reproduce, and the unit test is then the only evidence for the fix.

**Not run locally.** The sandbox this was written in has no Android SDK or emulator. The first CI run did pass both flows on this branch's app (including "back exits the app" in E8), before the dwell steps were added. Locally only the Python checks in `e2e/ci` were run (68 tests pass) and the flow files parse as YAML.

## 📸 Proof of change

### 🎥 Screen recording / video

None attached: no emulator was available. The "E2E before/after videos" workflow records both flows on master's app and on this branch's app.

### 🖼️ Screenshots

- [x] This change has **no user-visible / UI effect** beyond the crash going away; no layout or copy changed, so before/after screenshots are not applicable.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
