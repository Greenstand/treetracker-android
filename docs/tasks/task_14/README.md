# Task 14: Refactor Dashboard Screen Sections

## Summary

Refactored `DashboardScreen.kt` so the large dashboard layout is composed from smaller, named section composables.

## Changes

- Extracted `DashboardHeader` for the synced tree count display
- Extracted `DashboardStats` for upload progress and sync action UI
- Extracted `SyncStatusSection` for the messages button and unread indicator
- Extracted `DashboardActions` for the track/capture action
- Kept `Dashboard` as the top-level composition layer so behavior and navigation wiring stay unchanged
