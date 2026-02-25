# Add Tree Flow

The planter starts this flow from the map/dashboard context by choosing `Add Tree`.

The same flow supports:

- New tree capture
- Existing tree growth-tracking capture

Server-side analysis determines whether a capture maps to a new or existing tree.

## Convergence Parameter

Before allowing capture, the app attempts to improve GPS confidence using convergence logic based on recent location samples.

Behavior:

- Show a loading/progress indicator while convergence runs.
- Allow capture immediately if convergence threshold is met.
- Allow capture after timeout even if convergence is not met.

This balances data quality with field usability when GPS conditions are poor.
