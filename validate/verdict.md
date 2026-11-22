### Before and after

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `settings-delete-user-offline` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |
| `org-process-death-session-note` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |
| `first-screen` | ✅ passed | ✅ passed | ⚠️ **does not demonstrate the change**: passes without it too |

**`settings-delete-user-offline`** before: Assertion is false: "Account Deleted", id: DialogTitle is visible

**`org-process-death-session-note`** before: Assertion is false: id: SessionNoteField is visible
