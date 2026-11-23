# Task 100 - Deploy the dev build to Firebase from CI

## Problem

The "Deploy to Firebase Beta" workflow (now named "Deploy to Firebase") could only build and upload the `beta` build type.

## Change

`.github/workflows/deploy-firebase-beta.yml` has a new drop-down input, **Build to deploy**, with
`beta` (the default, so existing use is unchanged) and `dev`. The choice sets:

- the variant passed to the shared `build.yml` workflow,
- the artifact downloaded by the deploy job (`app-beta` or `app-dev`),
- the Firebase app id. Each build type has its own application id and so its own Firebase app;
  the ids come from `app/google-services.json` (`...TreeTracker.test` for beta,
  `...TreeTracker.dev` for dev).

`docs/testing/app-distribution-testing.md` now describes the workflow and its inputs.

## Not verified

The workflow was not run, because a run uploads a build to real testers. Before the first dev
deploy, check in the Firebase console that App Distribution is turned on for the
`org.greenstand.android.TreeTracker.dev` app.

## Found on the way, not changed

CI's `setup-property-file` action writes `treetracker_client_id` and `treetracker_client_secret`,
but the beta build reads `test_treetracker_client_*` and the dev build reads
`dev_treetracker_client_*`. Neither is written, so both CI builds get the placeholder
"configure in treetracker.keys.properties" as the messages API key
(`MessagesApiService`'s `apiKey` header).
