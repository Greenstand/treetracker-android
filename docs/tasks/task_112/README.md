# Task 112 - Profile edits upload with a valid registration

Reported in #android_chat (Nov 17): a profile name edit showed on the phone but never in production,
while captures from the same phone uploaded.

## Trace

1. Profile → Edit → Save calls `UserRepo.updateUser`, which saves the new name and sets
   `uploaded = false`.
2. The next sync's `PlanterUploader.uploadUsers` sends the user again as a v2 `wallet_registration`
   with the same `id` (`UserEntity.uuid`).
3. `bulk-pack-transformer-v2` upserts the grower account by wallet (`PUT /grower_accounts`), then
   posts the registration to field-data.

## Bugs in the app

- `updateUser` wrote the wallet into both `phone` and `email`. For a phone wallet the email became a
  phone number, and field-data rejects an email without `@` (`walletRegistrationPostSchema`), so the
  bundle failed processing.
- A new profile photo kept the old `photoUrl`, so `uploadUserImages` skipped it and the old photo
  was sent again.

## Change

- `UserRepo.updateUser` keeps the registered phone and email, and clears `photoUrl` when the photo
  changes so the new photo uploads before the registration.
- `UserRepoTest`: four tests; the first three fail on the old code.
- `e2e/flows/settings-profile-edit.yaml` (E10): edit first and last name, save, relaunch, the new name
  stays.

## Not fixed here (backend)

- field-data keeps the first registration for an `id` and ignores later ones, so its
  `wallet_registration` row keeps the old name.
- `treetracker-api` `upsertGrowerAccount` updates `grower_account` but never the legacy `planter`
  row, which the admin panel's Growers page reads (`/api/planter`). Edits don't show there until the
  backend also updates `planter` (or the admin reads `grower_account`).
