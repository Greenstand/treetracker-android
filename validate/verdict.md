### Before and after

| Flow | Before (base app) | After (this PR's app) | Verdict |
|---|---|---|---|
| `settings-logout-keeps-power-user` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |
| `settings-logout-relaunch` | ❌ failed | ✅ passed | ✅ **validated**: fails without this change, passes with it |

**`settings-logout-keeps-power-user`** before: Assertion is false: "Ada Admin" is visible

**`settings-logout-relaunch`** before: Assertion is false: id: PrivacyPolicyCloseButton is visible
