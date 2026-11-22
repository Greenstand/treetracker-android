# fix(settings): deleting a user while offline no longer crashes the app (#1416)

## What and why

Confirming Settings → Delete account with the device offline crashed the app, after the user had
already been removed locally. `UserSelectViewModel.deleteUser` saves a delete request for the
admin and calls `messageRepo.syncMessages()` to send it; offline, the upload in it throws an
`AmazonClientException`, which nothing caught on `viewModelScope`.

`deleteUser` now catches and logs that failure (cancellation is rethrown). The request stays
queued (`shouldUpload = true`) and goes up with the next sync. This fixes both the non-admin and
the admin delete, which share this code.

I left `MessagesRepo.syncMessages` throwing on a failed upload: `SyncDataUseCase` uses that to
mark the message step as failed, and `MessagesRepoTest` pins it
("sync messages propagates message upload failures").

Not in this PR:

- Splash and Dashboard also call `syncMessages()` unguarded, but only when the device is online.
  With unreachable storage that could fail the same way; it is a separate case from this issue.
- The header comments of `settings-delete-user.yaml` and `settings-delete-admin.yaml` still
  describe this bug. I did not touch those flows so that the before/after check only picks up the
  new one; the comments (and the extra asserts they suggest for the admin path) can follow.

## Tests

- `UserSelectViewModelTest`: new case, `syncMessages()` throws after a successful delete → nothing
  escapes, state is `ACCOUNTDELETEDLOCALLY`.
- New flow `e2e/flows/settings-delete-user-offline.yaml`: a regression flow, **not proof of this
  fix** (see below).

- [x] Issue exists: #1416
- [x] Tests added
- [ ] Linter (`./codeAnalysis`): not run, no SDK in the session this was written in
- [x] Docs updated (`docs/testing/e2e-flow-test-plan.md`, `docs/tasks/task_95/`)

## 📸 Proof of change

### 🎥 Screen recording / video

No validating flow: the one written for this PR passes on the app without the fix too.

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

`settings-delete-user-offline` follows the issue's steps (sign up as Ada, add Bob, Settings →
Delete account → Bob → confirm) and then asserts the "Account Deleted" dialog, the return to
Settings, the list without Bob and the Dashboard, without relaunching. CI ran it once on each app:

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `settings-delete-user-offline` | passed | passed | ⚠️ does not demonstrate the change |

So the videos CI puts between the markers (it picks up any flow a PR adds) only show that the
delete path works on this PR's app. They do not show the crash, and they are not evidence that
the fix fixes it.

What was tried and what is not known:

- One flow, one run per app, the result above. I had a single CI result and no device, so the
  flow was not reworked after it.
- Why the base app did not crash under the flow is not established; I could not read the run's
  artifacts. Possibilities I could not tell apart: the failed upload throws later than the flow's
  last step (it runs on a background thread behind the SDK's retries), or the crash does not
  happen under CI's emulator setup the way the issue describes.
- Not tried: a flow that stays on screen longer after the delete, and a manual run of the
  issue's steps on a device with logcat. Either would settle the point above.

Other proof, and its limit: the new `UserSelectViewModelTest` case is written to fail without
the fix (the exception thrown by `syncMessages()` is left uncaught in `viewModelScope`). I have
not run it on either side, so there is no failing-before / passing-after output to show here;
the unit test check on this PR gives the "after" half only.

Whether this can merge without a validating flow is the maintainers' call. A manual offline run
of the issue's steps on the base and PR builds would be the quickest real confirmation.

### 🖼️ Screenshots

No layout or styling change: the screens are the existing dialog and Settings, now reachable
offline. The videos above cover it.

Fixes #1416 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
