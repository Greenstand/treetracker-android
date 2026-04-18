# Task 17: Fix session persistence / login loop (Issue #1259)

## Problem
After completing authentication and capturing a tree, navigating to other screens
forces the user back through the full login flow (Privacy Policy + phone number).

## Root Causes
1. `Preferences._userId` is in-memory only — lost on process death
2. No session restoration during app bootstrap
3. `SignupViewModel` state not saved via `SavedStateHandle` — process death during
   selfie capture (ImageCaptureActivity) causes NPE in `createUser()`
4. `createUser()` crashes silently on null state fields

## Changes
1. Persist `_userId` to SharedPreferences and restore on init
2. Restore session in `SplashScreenViewModel.bootstrap()` for existing power users
3. Save SignupViewModel form state with SavedStateHandle
4. Guard `createUser()` against null state
