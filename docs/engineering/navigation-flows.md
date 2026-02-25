# Navigation Flows

The app uses flow-based navigation, primarily through `CaptureSetupNavigationController` and `CaptureFlowNavigationController`.

## Main Flows

### Signup Flow

Typical sequence:

1. Privacy policy
2. Phone/email input
3. First/last name input
4. Selfie capture

### Capture Setup Flow

Managed by `CaptureSetupNavigationController`.

Typical sequence:

1. Dashboard -> UserSelect
2. Optional org-specific setup steps (for org-linked configurations)
3. TreeCapture entry

Legacy/optional setup screens include wallet/org selection based on organization configuration.

When setup completes, step counter, session tracking, and GPS updates are started before tree capture.

### Capture Flow

Managed by `CaptureFlowNavigationController`.

Core/optional screens can include:

- TreeCapture
- TreeImageReview
- TreeHeightScreen (if enabled)
- SessionNote
- TreeDBH (if enabled)

The flow loops back to `TreeCapture` after each completed capture until the user exits.

### Messaging Flow

1. MessagesUserSelect
2. IndividualMessageList / MessageSelect
3. Message type: announcement, survey, or direct chat

## Single Screens

Screens that can appear outside multi-step flows:

- Splash
- Language Select
- Organization Select
- Dashboard
