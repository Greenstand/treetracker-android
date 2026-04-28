# Task 14: Refactor Dashboard Screen Sections

## Summary

Refactored `DashboardScreen.kt` so the large dashboard layout is composed from smaller, named section composables.

## Changes

- Extracted `DashboardHeader` for the synced tree count display
- Extracted `DashboardStats` for upload progress and sync action UI
- Extracted `SyncStatusSection` for the messages button and unread indicator
- Extracted `DashboardActions` for the track/capture action
- Kept `Dashboard` as the top-level composition layer so behavior and navigation wiring stay unchanged
- Removed the extra trailing blank line at the end of `DashboardScreen.kt` so the file ends with exactly one newline
- Applied section weights (`DashboardHeader`, `DashboardStats`, `SyncStatusSection`, `DashboardActions`) at `Dashboard` `Column` call sites and removed internal child `weight(...)` usage where scope resolution could fail during Kotlin compile
- Removed the explicit `androidx.compose.foundation.layout.weight` import to avoid resolving to internal/non-callable `weight` symbols after Compose API changes
