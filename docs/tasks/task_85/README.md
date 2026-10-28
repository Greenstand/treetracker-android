# Task 85 - feature:trees module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the tree screens and the map into one `feature:trees` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/trees`, same packages, no code changes: `treeedit/` (tree list, tree detail and
  edit, and its planter picker), `map/` (trees on a MapLibre map) and `trees/TreesModule.kt`.
- Its unit tests move with it: `MapViewModelTest`, `TreeDetailViewModelTest`, `TreeListViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.
- Extra dependencies: `maplibre.android.sdk`, `coil.compose`, `kotlinx.serialization.json`,
  `kotlinx.datetime` and `compose.material.icons.extended` (the map).

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
