# fix(setup): restore capture setup after process death on Add org (#1418)

Thank you for opening a Pull Request!

Before submitting your PR, there are a few things you can do to make sure it goes smoothly:
- [x] Make sure to open a GitHub issue as a bug/feature request before writing your code! That way we can discuss the change, evaluate designs, and agree on the general idea
- [x] Ensure the tests are added/updated (if necessary)
- [ ] Ensure the linter passes (`./codeAnalysis` to automatically apply formatting/linting) — not run: the session had no Android SDK. CI's lint is the first run.
- [x] Appropriate docs were updated (if necessary)

## What was wrong

Track → pick a user → Next shows Add org. When Android killed the app in the background there,
reopening it crashed with `IllegalStateException: CaptureSetupScope not open`.

Navigation 3 restores the back stack and composes only its top entry, Add org. The capture setup
scope (the picked user, org name, wallet, note, and the setup navigation controller) lived in a
Koin scope held in memory, and the screen that opens it (user select) is not recreated.
`AddOrgViewModel`'s `init` asked for the scope and threw. Wallet select, add wallet and session
note have the same dependency.

## The fix

The setup in progress is saved to preferences, and the scope is rebuilt from it when a restored
screen asks for it.

- New `CaptureSetupStore`: the four fields of `CaptureSetupData` as one JSON string in
  preferences. The entry exists from `CaptureSetupScopeManager.open()` until `close()`.
- `CaptureSetupData` saves on every change.
- `CaptureSetupScopeManager.getData()` / `.nav`: with no open scope, restore from the saved setup
  if there is one. With none they still throw, so a missing `open()` outside process death is
  not hidden.
- The setup navigation controller already finds its step from the back stack on `navForward`, so
  Next on the restored Add org starts the session for the picked user and opens capture.

Because the fix is in the scope manager, the other setup screens are covered by the same code,
but only Add org is exercised by a flow (the others need a seeded org).

Things a reviewer may want to know:

- The org name field writes to preferences on each keystroke (`apply()`, a small string).
- After the restore the org field shows what it shows on a first visit, not the text typed before
  the kill. I left that alone.
- Back from the restored Add org starts a fresh setup on user select (no planter selected).
- The store is looked up with `getOrNull`, so the small Koin graphs in existing unit tests work
  unchanged, without persistence.

## 📸 Proof of change (REQUIRED)

### 🎥 Screen recording / video (always required)

validates: sad-process-death-add-org

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

Not run yet: this was written in a session with no emulator and no way to push, so the flow has
run on neither app and there is no verdict or video. CI fills the block above on the first push.
What the flow should show, to be checked against the verdict:

- **Before** (merge base): after `killApp` and relaunch the app crashes, so the wait for
  `OrgNameField` after the relaunch fails. If "before" fails at any earlier step, it is not
  proof.
- **After**: Add org comes back, Next opens the capture screen.

Also added, as supporting tests and not as the proof: `CaptureSetupProcessDeathTest`
(Robolectric, 7 cases: restore of data and nav, saving after a restore, closed / never-started /
unreadable setups are not restored, `open()` after a kill starts empty). Not run locally either.

### 🖼️ Screenshots (required for any UI change)

No screen looks different; the change is that the app no longer crashes. The videos cover it.

- [ ] This change has **no user-visible / UI effect** (refactor, docs, CI, etc.), so screenshots are not applicable.

Fixes #1418 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
