### Before and after

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `settings-delete-user` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |
| `settings-delete-admin` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |

**`settings-delete-user`** before: Assertion is false: "Account Deleted", id: DialogTitle is visible

**`settings-delete-admin`** before: Assertion is false: "Account Deleted", id: DialogTitle is visible
