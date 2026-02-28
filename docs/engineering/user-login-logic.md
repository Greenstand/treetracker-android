# User Login Logic

Status: Work in progress (legacy design notes consolidated).

## Problem

The login/check-in model is designed for shared-device usage, where multiple planters may use one phone and identity attribution is required for payout verification.

## Design Intent

- Verify who actually tracked trees.
- Support users without reliable email access.
- Work in intermittent/offline contexts.
- Keep login simple for low-literacy users.

## Identity Concepts

- Tree records are uploaded with planter identity context.
- Repeated check-ins (identifier + photo) provide attribution over time.
- Re-identification can be required after timeout windows (for example every 2 hours).
- If a user already exists on that device, re-entry can be reduced to identifier + verification photo.

## Data Model Distinction

- `planter_details` style records: profile-like details, typically one per user per device.
- `planter_identifications` style records: repeated check-ins over time.

This distinction enables many check-ins per single user profile while preserving time-bounded attribution.

Legacy discussion links:

- [User Login Flow issue comment](https://github.com/Greenstand/treetracker-android/issues/178#issuecomment-467268480)
- [Issue #178](https://github.com/Greenstand/treetracker-android/issues/178)
