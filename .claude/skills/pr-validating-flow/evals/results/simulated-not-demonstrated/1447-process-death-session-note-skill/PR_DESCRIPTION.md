# fix(setup): restore session note, wallet select and add wallet after process death instead of crashing

Thank you for opening a Pull Request!

Before submitting your PR, there are a few things you can do to make sure it goes smoothly:
- [x] Make sure to open a GitHub issue as a bug/feature request before writing your code! That way we can discuss the change, evaluate designs, and agree on the general idea
- [x] Ensure the tests are added/updated (if necessary)
- [ ] Ensure the linter passes (`./codeAnalysis` to automatically apply formatting/linting) — not run: this was written in a sandbox without an Android SDK, so CI is the first build, lint and test run of this branch
- [x] Appropriate docs were updated (if necessary)

## What was wrong

In an org whose capture setup flow has a session note or wallet select step, if Android killed the
app in the background on session note, wallet select or add wallet, reopening it from recents
crashed with `IllegalStateException: CaptureSetupScope not open. Call open() before getData().`
Navigation 3 restores the screen, but the capture setup scope only lives in memory, and the three
ViewModels read it in `init`. Add org had the same crash (#1418); its fix was private to
`AddOrgViewModel`.

## What changed

- New `CaptureSetupRestorer`: reopens the setup scope when a setup step finds it closed, saves the
  setup user's id while setup is alive, and loads that user back into the scope after a restore.
- `SessionNoteViewModel`, `WalletSelectViewModel` and `AddWalletViewModel` use it in `init`. Their
  screens ignore Next/Back until the user is reloaded, and fall back to the Dashboard if the user
  no longer exists (same behaviour as Add org).
- The saved id uses the pref key Add org already writes, so flows that contain Add org and one of
  these steps restore from either.

Limits, by choice: only the user is restored. Text typed on the step before the process died (note,
wallet name, picked wallet) is gone and the step starts empty, as on Add org. `AddOrgViewModel`
keeps its own copy of the restore logic for now; details in `docs/tasks/task_111/README.md`.

## 📸 Proof of change (REQUIRED)

### 🎥 Screen recording / video (always required)

No validating flow: the two flows this PR adds pass on the app before the change too, so they do not show the bug.

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

**This PR has no before/after proof. Please decide whether it can merge without one, or whether
the reproduction should be confirmed on a device first.**

What was tried:

- `org-process-death-session-note` (org `session-note`): on the session note, Home, `killApp`,
  reopen, wait for `SessionNoteField`, type a note, Next, wait for capture.
- `org-process-death-wallet-select` (org `wallet-select`): the same on wallet select, then again on
  add wallet.

Result of the "Affected flows (annotated video)" check:

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `org-process-death-session-note` | passed | passed | ⚠️ does not demonstrate the change |
| `org-process-death-wallet-select` | passed | passed | ⚠️ does not demonstrate the change |

So the flows did not reproduce the crash on the base app, and any videos CI puts between the
markers above only show that the screens come back on both apps. Both flows stay in the PR as
regression flows (they are in `e2e/run-org.sh`), not as validation.

I can't explain the "before" pass. From the code, the base app should crash at the reopen step:
the three ViewModels read the closed setup scope in `init`, and nothing on the restore path opens
it or gives it a user. The same Home / `killApp` / `launchApp: {stopApp: false}` steps reproduced
the Add org crash for #1418. I only have the verdict table, not the "before" run's video or logs,
and no emulator, so I could not check whether the process really died in those runs or whether the
issue's manual steps crash the base app on the CI image. That also means **the bug itself is not
confirmed as reported**; the fix follows the issue and the code, not an observed crash.

Other evidence, weaker than a flow:

- `CaptureSetupRestorerTest` (new), and restore cases in `SessionNoteViewModelTest`,
  `AddWalletViewModelTest`, `WalletSelectViewModelTest` and `WalletSelectScreenUiTest`: with the
  setup scope closed, each ViewModel reopens it, reloads the saved user, and falls back when the
  user is gone.
- These were never run locally (no Android SDK) and I have not seen the result of the unit-test or
  lint checks for this branch, only the flows check. I also have no failing run of them against the
  base code: the restore tests use the new constructor argument, so on the base they don't compile
  rather than fail on an assertion.

### 🖼️ Screenshots (required for any UI change)

| Before | After |
| ------ | ----- |
| Not captured: the crash was not reproduced in CI | The setup step is shown again after reopening |

No layout changed; the screens look as they did.

- [ ] This change has **no user-visible / UI effect** (refactor, docs, CI, etc.), so screenshots are not applicable.

Fixes #1447 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
