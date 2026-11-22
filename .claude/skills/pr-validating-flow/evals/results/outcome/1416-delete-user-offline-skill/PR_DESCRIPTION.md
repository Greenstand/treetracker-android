# fix(settings): deleting a user while offline no longer crashes the app (#1416)

- [x] Make sure to open a GitHub issue as a bug/feature request before writing your code! That way we can discuss the change, evaluate designs, and agree on the general idea
- [x] Ensure the tests are added/updated (if necessary)
- [ ] Ensure the linter passes (`./codeAnalysis` to automatically apply formatting/linting): not run, the agent container has no Android SDK
- [x] Appropriate docs were updated (if necessary)

## What was wrong

Confirming Settings → Delete account with the device offline closed the app with an
`AmazonClientException`. `UserSelectViewModel.deleteUser` deletes the user locally, queues a
message asking the admin to delete the account, then calls `messageRepo.syncMessages()`. The
upload inside it throws when Cognito can't be reached, and nothing caught it in `viewModelScope`.

## The change

`UserSelectViewModel.deleteUser` catches a failed `syncMessages()` (cancellation is rethrown) and
logs it. The request to the admin is already saved with `shouldUpload = true`, so the next sync
sends it. The "Account Deleted" dialog then shows and closing it goes to Settings (or Signup for
the admin), as it does online.

The issue points at `MessagesRepo.syncMessages`, where `uploadMessages()` sits outside the
try/catch. I left that as it is: `SyncDataUseCase` runs `syncMessages()` as its tracked "Message
Sync" step and reports a failed upload from that exception, and `MessagesRepoTest` asserts that it
propagates. Say so if you would rather have it caught in the repo.

Not in this PR:

- Splash and the Dashboard also call `syncMessages()` unguarded, but only when the internet check
  passes (online with bad keys). Same kind of crash, different trigger.
- `settings-delete-user.yaml` (E7) and `settings-delete-admin.yaml` (E8) still have their "KNOWN
  BUG" header and `stopApp`/`launchApp` workaround. They pass with and without the fix, so I did
  not edit them here: an edited flow is picked up as a validating flow and would show as "does
  not demonstrate the change". A follow-up can make E8 assert the dialog and the clean Signup
  stack.

## Tests

- New flow `e2e/flows/settings-delete-user-offline.yaml` (the issue's steps for Bob).
- New unit test in `UserSelectViewModelTest`: `syncMessages()` throws, the state still becomes
  `ACCOUNTDELETEDLOCALLY` and no exception is left uncaught.
- Task notes: `docs/tasks/task_95/README.md`.

**Nothing was built or run by me.** The agent container has no Android SDK or emulator, so the
unit test, the flow and the before/after verdict are all unverified until this PR's checks run.

## 📸 Proof of change (REQUIRED)

### 🎥 Screen recording / video (always required)

validates: settings-delete-user-offline

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

Expected, to be confirmed against the verdict CI writes above. Before: after confirming the
delete offline the app closes, so the flow fails at the "Account Deleted" dialog or at closing
it. After: the dialog shows, OK returns to Settings, the Delete account list has only Ada, and
back reaches the Dashboard.

One thing to check in the "before" run: the crash comes a moment after the confirm tap (the AWS
client retries first), not on it. The flow reads the dialog's title, message and user card before
tapping OK and keeps using the app afterwards, but if the base app survives all of that the
verdict will be "does not demonstrate the change" and the flow needs rework, not a merge.

### 🖼️ Screenshots (required for any UI change)

| Before | After |
| ------ | ----- |
| App closes after confirming the delete (see the "before" video) | "Account Deleted" dialog, then Settings (see the "after" video) |

No layout or visual change: the dialog and screens are the existing ones.

Fixes #1416 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
