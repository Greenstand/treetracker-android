# Task 46 - core:resources module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the Android resources out of `:app` so the feature and design-system modules split out next
can use them.

## Changes

- `app/src/main/res` moves to `core/resources/src/main/res` unchanged (strings in en/pt/sw,
  drawables, fonts, colors, themes, launcher icons, xml).
- New `core:resources` library module (namespace `org.greenstand.android.TreeTracker.resources`),
  depending on AppCompat for its themes. `app` depends on it.
- No Kotlin changes. The app keeps `android.nonTransitiveRClass=false`, so the app's own
  `org.greenstand.android.TreeTracker.R` still has every resource. Files switch to
  `org.greenstand.android.TreeTracker.resources.R` only when they move into other modules.
- `app_name` stays a `resValue` per build type in `app`.

## Verification

- The debug APK's resource table has the same 1823 resources and an identical string pool as
  before.
- All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless
  pass.
- Emulator smoke test: no crashes; dashboard, language and settings screenshots are
  pixel-identical to the previous build.
