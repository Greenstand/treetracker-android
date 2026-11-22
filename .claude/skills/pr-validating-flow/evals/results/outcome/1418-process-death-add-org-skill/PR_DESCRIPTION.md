# fix(setup): restore capture setup after process death on Add org (#1418)

Thank you for opening a Pull Request!

Before submitting your PR, there are a few things you can do to make sure it goes smoothly:
- [x] Make sure to open a GitHub issue as a bug/feature request before writing your code! That way we can discuss the change, evaluate designs, and agree on the general idea
- [x] Ensure the tests are added/updated (if necessary)
- [ ] Ensure the linter passes (`./codeAnalysis` to automatically apply formatting/linting): not run, see "Not verified locally"
- [x] Appropriate docs were updated (if necessary)

## What was wrong

Reopening the app after Android killed it on Add org crashed with
`IllegalStateException: CaptureSetupScope not open`. Navigation 3 restores the back stack
(`[Dashboard, UserSelect, AddOrg]`) and composes only Add org, but the capture setup scope was in
memory only, and the one place that opens it (`UserSelectViewModel` init) doesn't run on restore.

## What changed

The setup in progress is now saved and restored, so Add org comes back with the picked user and
Next continues to capture.

- `CaptureSetupStore` (new) keeps the picked user's id, destination wallet, session note and org
  name in preferences. `CaptureSetupData` writes to it on every change once a user is picked.
- `CaptureSetupScopeManager.open()` and `close()` forget the saved copy (a new setup starts, or the
  setup/capture ended). A new `restore {}` opens the scope without forgetting it.
- `CaptureSetupRestorer` (new) reopens the scope from the saved copy. `Root` calls it in the
  once-per-process effect that already re-hydrates `OrgRepo`, before `Host` shows the back stack.
  It also sets the preferences user id and starts GPS updates, as user select does in a normal
  setup.
- Fallback: if nothing could be restored and a setup screen that reads the scope (Add org, wallet
  select, add wallet, session note) is on top of the restored back stack, `Host` pops to the
  screen under it (user select, which starts a new setup) instead of crashing.

Things to know when reviewing:

- Text typed in the org field but not confirmed with Next is not put back after process death.
- A saved setup is also restored on a plain cold start (task swiped away mid-setup). The scope is
  then open but unused, and Track starts a new setup that forgets it.
- The same crash on image review (capture flow scope, #1419) is not touched.

Details in `docs/tasks/task_111/README.md`.

## 📸 Proof of change (REQUIRED)

### 🎥 Screen recording / video (always required)

validates: sad-process-death-add-org

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

`e2e/flows/sad-process-death-add-org.yaml` follows the issue's steps: sign up, Track, pick the
user, Next, Home, kill the process, reopen from recents; then it waits for Add org and taps Next
to reach capture.

- Before (expected): the app crashes on reopen, so the wait for `OrgNameField` after
  `launchApp: {stopApp: false}` fails.
- After (expected): Add org comes back, and Next opens the capture screen.

These are what the flow is written to show. **I have not run it on either app**: this session had
no Android SDK or emulator, so the verdict and the videos come from the "Affected flows" check on
this PR. If the "before" run fails at any step other than that wait, the flow is not proof and
needs fixing.

### 🖼️ Screenshots (required for any UI change)

No screen's layout changed; the before/after videos above are the visual proof.

| Before | After |
| ------ | ----- |
|        |       |

- [ ] This change has **no user-visible / UI effect** (refactor, docs, CI, etc.), so screenshots are not applicable.

## Tests

- New unit tests: `CaptureSetupStoreTest`, `CaptureSetupRestorerTest`, `RestoredBackStackTest`.
- `RootUiTest` binds a `CaptureSetupRestorer` double, because `Root` now injects one.
- `docs/testing/e2e-flow-test-plan.md`: H5 row and finding 3 updated.

## Not verified locally

Nothing here was compiled or run: no build, no unit tests, no lint, no emulator. CI on this PR is
the first time the code is built.

Fixes #1418 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
