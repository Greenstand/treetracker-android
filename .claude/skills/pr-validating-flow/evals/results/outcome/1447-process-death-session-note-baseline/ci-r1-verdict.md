### Before and after

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `org-process-death-session-note` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |
| `org-process-death-wallet-select` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |
| `org-process-death-add-wallet` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |

**`org-process-death-session-note`** before: Assertion is false: id: SessionNoteField is visible

**`org-process-death-wallet-select`** before: Assertion is false: id: UserButton_5551230001 is visible

**`org-process-death-add-wallet`** before: Assertion is false: id: WalletNameField is visible
