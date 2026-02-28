# Product Documentation

## Overview

Greenstand's Treetracker mobile app supports climate restoration and poverty alleviation by creating economic opportunity in planting, growing, and maintaining trees.

The app captures tree photos and metadata as "captures". Captures are uploaded to Greenstand systems for verification, analysis, and impact attribution.

## Intended Use Cases

The product provides transparent evidence for growers and organizations to claim impact ownership as trees grow.

Primary goals:

- Enable reliable field capture of tree growth data.
- Support payouts/work attribution through verifiable records.
- Work in low-connectivity environments.

## Intended Users

### 1. Organizational trackers

- Coordinated by projects/organizations.
- Semi-savvy smartphone users.
- Often capture large volumes daily.
- Data usually routes to organizational admin workflows.

### 2. Village coordinators and independent trackers

- Often support many growers.
- Typically communicate through WhatsApp.
- May have intermittent internet and limited desktop access.

### 3. Individual growers

- May not own a compatible smartphone.
- Often offline and may have limited literacy.
- Frequently rely on shared devices and basic phone access.

## Product Constraints

The app should:

- Work offline for core capture flows.
- Upload reliably on unstable or low-bandwidth networks.
- Minimize battery usage.
- Keep UX simple and localizable.
- Use GPS-centric location evidence.
- Preserve high-quality, attributable capture data.

Additional field constraints:

- Bright sunlight and difficult viewing conditions.
- Limited charging and inconsistent connectivity.
- Shared-device usage patterns.

## Registration, Login, and Identification

Identity flows are designed to improve attribution of work to the right person, especially for shared devices.

Key concepts:

- Identifier is generally phone number or email.
- Check-in/selfie can be used for repeated identity confirmation.
- Tree captures are associated with check-in context for verification.

See also: [User Login Logic](../engineering/user-login-logic.md).

## Activity Verification and Payout Context

Verification relies on combining:

- Tree location and capture metadata.
- Check-in/user identity context.
- Synced records from device to cloud.

Payout tracking concepts historically referenced include uploaded/approved counts and payment status. Exact payout mechanics are handled outside this app.

## Use Cases

The following use cases are documented across this docs system:

- Registration and check-in
- Switching users on shared devices
- Add tree / track tree growth
- High-volume capture sessions
- FMNR-style before/after capture scenarios
- Tree height capture (org-dependent)
- Syncing planter/tree data under intermittent network conditions

Detailed pages:

- [User Stories](user-stories.md)
- [Add Tree Flow](add-tree-flow.md)
- [Device Data Capture](device-data-capture.md)
