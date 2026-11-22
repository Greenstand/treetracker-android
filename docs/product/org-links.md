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

## Debug Overrides

Debug builds can override the current org's link settings from Settings → Dev Options →
"Org Link Overrides", to try other flows without making a new org link:

- Each setup/capture step and feature shows what the org sets ("Org: On/Off") and has a switch
  to override it. User select and tree capture are required and can't be turned off.
- A wallet ID can replace the org's wallet.
- "Apply overrides" turns them all on or off without losing them; "Reset all org overrides"
  clears them.
- Overrides are stored in preferences, survive restarts, and apply to whichever org is current.
  Flow changes take effect from the next capture session.
- Release builds ignore them.

See [task 111](../tasks/task_111/README.md).

