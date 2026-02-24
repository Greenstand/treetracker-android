# Navigation Flows

## Capture Setup Flow

Managed by `CaptureSetupNavigationController`. Dynamic flow defined by the user's organization:

1. Dashboard -> UserSelect (user selection or creation)
2. Organization-specific setup screens (varies by org)
3. TreeCapture (final destination)

When setup completes, step counter, session tracker, and GPS updates are started before entering tree capture.

## Tree Capture Flow

Managed by `CaptureFlowNavigationController`. Dynamic flow configured per organization that can include:

- TreeCapture: Main capture screen
- TreeHeightScreen: Height measurement (if enabled)
- TreeImageReview: Review captured photos
- SessionNote: Add notes to capture session
- TreeDBH: Diameter at breast height (if enabled)

## Messaging Flow

- MessagesUserSelect -> IndividualMessageList -> Chat/Announcement/Survey
