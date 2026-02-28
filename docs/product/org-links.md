# Org Links

## Context

Org links are deep links that open the app and apply organization-specific configuration (theme, enabled features, flow options, and org selection metadata).

## Goals

- Reduce reliance on separate app flavors for org customization.
- Prevent manual org-input errors.
- Allow switching between configured orgs from an org selection screen.

## Behavior

- Clicking an org link adds or updates org configuration on-device.
- If another org link is used, the app can refresh theme/icons/flow config.
- Offline behavior should continue for embedded configuration; downloadable assets can be placeholders until online.
- Users cannot manually type/add orgs; orgs come from valid links.

## Decision Drivers

- Lower maintenance/testing burden vs many flavors.
- Better data integrity for org association.
- Simpler org switching for shared devices.
