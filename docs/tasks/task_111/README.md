# Task 111 - Org link overrides on the dev options screen

## Goal

Try different org link setups (flow steps, features, wallet) on a debug build without making
a new org link for each one. The dev options screen shows what the current org's link (or the
default Greenstand org) sets, and lets each setting be overridden. Overrides win over the org,
and are kept across app restarts.

## Change

- `models/organization/OrgOverrides.kt`
  - `OrgOverrideCatalog`: the settings that can be overridden, in canonical order.
    - Setup flow: user select (required), wallet select, add org, session note.
    - Capture flow: tree capture (required), image review, tree height.
    - Features: `forceNote` on image review.
    - Required steps can't be turned off: the dashboard always opens user select, and the
      setup flow ends on, and the capture flow loops back to, tree capture.
  - `OrgOverrides`: a master switch plus per-setting on/off maps and a wallet ID. Only changed
    settings are stored, so everything else follows the org. `applyTo(org)` removes steps
    turned off, inserts steps turned on after the nearest earlier step in canonical order
    (keeping the org's own order), and adds or removes features. Aliased routes
    (`image-review`, `tree-capture`) count as their canonical route.
  - `OrgOverrideStore`: keeps the overrides as JSON in preferences under
    `system-settings/org-link-overrides`. Release builds ignore whatever is stored.
- `OrgRepo.currentOrg()` returns the org with active overrides applied. Everything that runs the
  flows, checks features or reads the wallet goes through it (the two flow navigation
  controllers, `FeatureResolver`, `SessionTracker`). `OrgRepo.baseCurrentOrg()` is the org
  without overrides, used by dev options to show the org's own values.
- Dev options (`DevOptionsViewModel`, `OrgLinkOverridesSection.kt`): below the existing params, an "Org Link Overrides"
  section with the current org, the "Apply overrides" switch, one switch per step/feature with
  its org value ("Org: On") and an "Overridden" note plus a per-row Reset, a wallet ID field,
  the flows in use, and "Reset all org overrides". The section is hidden when no org is loaded.

Overrides are keyed by route, not by org, so they keep applying after switching orgs or opening
another org link. Flow changes apply from the next capture session (the navigation controllers
read the flow when the setup scope opens); features and wallet apply at once.

## Tests

- `OrgOverridesTest`: merge rules (remove, insert in order, aliases, required steps, custom org
  order, features, wallet, master switch off).
- `OrgOverrideStoreTest` (Robolectric): kept across a restart, cleared, ignored on release
  builds and when unreadable; `OrgRepo.currentOrg()` vs `baseCurrentOrg()`.
- `DevOptionsOrgOverridesTest`: view model rows, actions, reset, persisted values.
- `DevOptionsUiTest`: section hidden without an org, shows org values, sends actions.

The container has no Android SDK, so the build and tests were checked on CI only.
