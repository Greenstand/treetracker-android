# Org Link Features

Org links configure the app for a specific organization. The deeplink passes an org ID and name, and the app fetches the full configuration from Firebase Remote Config.

## Deeplink Format

```
app://mobile.treetracker.org/org?id={orgId}&name={orgName}
```

- `id` -- Organization identifier. Used to look up config in Firebase Remote Config under the key `org_config_{id}`.
- `name` -- Organization display name. Used as fallback label if Remote Config is unavailable.

When the app opens this link it checks Firebase Remote Config for a matching key. If found, the full org config is applied. If not (offline, unknown ID, timeout), a minimal org is created using the provided name with default flows.

## Remote Config JSON Format

Stored in Firebase Remote Config under key `org_config_{id}`:

```json
{
  "version": "1",
  "walletId": "destination-wallet-address",
  "captureSetupFlow": [
    { "route": "user-select" },
    { "route": "session-note" }
  ],
  "captureFlow": [
    { "route": "capture/{profilePicUrl}" },
    { "route": "tree-image-review/{photoPath}", "features": ["forceNote"] },
    { "route": "tree-height-selection" }
  ]
}
```

## Configurable Fields

### walletId

Destination wallet for tree uploads. When a user does not have their own wallet, the session uses this wallet as the destination for captured trees. If empty or omitted, falls back to the user's personal wallet.

### captureSetupFlow

Ordered list of screens the user goes through before starting a capture session. Once the last screen completes, a session is created and the capture loop begins.

### captureFlow

Ordered list of screens the user goes through for each tree capture. After the last screen completes, the tree is saved and the flow loops back to the first screen for the next tree.

## Setup Flow Screens

These screens run once at the start of a session, in the order listed in `captureSetupFlow`.

| Route | Description |
|-------|-------------|
| `user-select` | Pick the active user from a list of registered users. |
| `wallet-select` | Pick an alternative destination wallet from available users. |
| `add-org` | Enter or confirm the organization name. Remembers previous entries for autofill. |
| `session-note` | Enter a note that applies to the entire capture session. |

## Capture Flow Screens

These screens run in a loop for each tree, in the order listed in `captureFlow`.

| Route | Alias | Description |
|-------|-------|-------------|
| `capture/{profilePicUrl}` | `tree-capture` | Opens the camera to photograph a tree. Waits for a GPS fix before allowing capture. |
| `tree-image-review/{photoPath}` | `image-review` | Shows the captured photo for review. Allows adding a note per tree. |
| `tree-height-selection` | -- | Presents height/color category buttons. Stores the selection as a tree attribute. |

Either the full route string or its alias can be used in the config JSON.

## Per-Route Feature Flags

Features are optional flags attached to a specific route in the `features` array. They modify behavior on that screen.

### forceNote

- **Key:** `"forceNote"`
- **Applicable route:** `tree-image-review/{photoPath}`
- **Behavior:** Requires the user to enter a note before proceeding. If the user tries to move forward with an empty note, a dialog blocks navigation until a note is added.

**Example:**
```json
{ "route": "tree-image-review/{photoPath}", "features": ["forceNote"] }
```

## Fallback Behavior

When Firebase Remote Config does not return a config (offline, unknown org, timeout after 10 seconds), the app creates a minimal org with these defaults:

**Setup flow:** user-select, add-org
**Capture flow:** capture, image-review
**Wallet:** empty (uses user's own wallet)

The org name from the deeplink is preserved so the user still sees the correct organization label.

## Route Validation

Unknown routes in the config are silently dropped. If a config contains a route the app does not recognize, that step is skipped and the remaining valid routes are used. This means a config will never crash the app -- worst case, some steps are missing.

---

## Test Deeplinks

### Full-featured org (setup note + height selection + forceNote)

Firebase Remote Config key: `org_config_109288091`

```json
{"version":"1","walletId":"klasdlk1-a0a23lmnzcln9o3","captureSetupFlow":[{"route":"user-select"},{"route":"session-note"}],"captureFlow":[{"route":"capture/{profilePicUrl}"},{"route":"tree-image-review/{photoPath}","features":["forceNote"]},{"route":"tree-height-selection"}]}
```

```
adb shell am start -a android.intent.action.VIEW -d "app://mobile.treetracker.org/org?id=109288091\&name=Kasiki%20Hai"
```

### Minimal custom org (default setup + capture with no extras)

Firebase Remote Config key: `org_config_200100300`

```json
{"version":"1","walletId":"test-wallet-abc123","captureSetupFlow":[{"route":"user-select"}],"captureFlow":[{"route":"capture/{profilePicUrl}"},{"route":"tree-image-review/{photoPath}"}]}
```

```
adb shell am start -a android.intent.action.VIEW -d "app://mobile.treetracker.org/org?id=200100300\&name=Test%20Org"
```

### Org with wallet select and all capture screens

Firebase Remote Config key: `org_config_300200100`

```json
{"version":"1","walletId":"full-wallet-xyz","captureSetupFlow":[{"route":"user-select"},{"route":"wallet-select"},{"route":"add-org"},{"route":"session-note"}],"captureFlow":[{"route":"tree-capture"},{"route":"image-review"},{"route":"tree-height-selection"}]}
```

```
adb shell am start -a android.intent.action.VIEW -d "app://mobile.treetracker.org/org?id=300200100\&name=Full%20Config%20Org"
```

### Fallback test (no Remote Config entry)

No Firebase setup needed. This tests the offline/unknown org fallback path.

```
adb shell am start -a android.intent.action.VIEW -d "app://mobile.treetracker.org/org?id=999999999\&name=Unknown%20Org"
```

Expected: App creates a minimal org named "Unknown Org" with default flows (user-select, add-org, capture, image-review) and no destination wallet.
