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
- Added missing Compose layout import `androidx.compose.foundation.layout.weight` in `DashboardScreen.kt` to fix `:app:compileDebugKotlin` unresolved reference errors on `Modifier.weight(...)`
- Disambiguated three modifier chains to use `.then(Modifier.weight(1f))` in `DashboardStats`, `SyncStatusSection`, and `DashboardActions` to resolve `Float cannot be invoked as a function` during Kotlin compile
