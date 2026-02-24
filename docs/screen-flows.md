# Screen Flows

The app has several main screen flows, controlled by `CaptureSetupNavigationController` and `CaptureFlowNavigationController`.

## 1. Onboarding and Signup

- The app starts with the `SplashScreen`.
- From the splash screen, the user might be taken to the `Dashboard`, `Language` selection, or the `SignupFlow`.
- The `SignupFlow` is used for new user registration.

## 2. Capture Setup Flow

- Managed by `CaptureSetupNavigationController`.
- Starts from the `Dashboard` and navigates to `UserSelect`.
- If a new user is created, it follows a dynamic path defined by the user's organization to set up the capture process.
- Ends by navigating to the `TreeCapture` screen.

## 3. Tree Capture Flow

- Managed by `CaptureFlowNavigationController`.
- Starts on the `TreeCapture` screen.
- The flow for capturing tree data is dynamic and defined by the user's organization.
- Can include screens like `TreeHeightScreen`, `TreeImageReview`, and `SessionNote`.
- After capture is complete, tree data is saved and the user is returned to `TreeCapture` to capture another tree.
- The user can navigate back to the `Dashboard` from this flow.

## 4. Messaging Flow

- Starts from `MessagesUserSelect`, then goes to `IndividualMessageList`.
- From the message list, the user can navigate to `Chat`, `Announcement`, or `Survey`.
