# Admin panel upload check

`capture-upload.yaml` is one [Test Buddy](https://github.com/Elforama/testbuddy) flow with two
devices: an Android emulator (`phone`) and headless Chrome (`admin`).

1. `phone` signs up Ada Admin, captures one tree with a note unique to the run
   (`tb-e2e-<run id>-<time>`), uploads it, and waits for the Dashboard to show 0 to upload and
   1 uploaded.
2. Meanwhile `admin` logs in to https://dev-admin.treetracker.org.
3. Once the phone has uploaded, `admin` reloads Verify and opens the newest capture until its
   details show the note: up to 40 tries, about 8 minutes, since the backend takes a few minutes
   to ingest an upload.

It replaces the WebdriverIO + Appium + Cucumber test on `releases/2.2.1`
(`e2e/features/03_capture_setup.feature`, `e2e/utils/admin.ts`): no Appium, ChromeDriver or
Node, and the same `tb` the other flows use.

It is kept out of `e2e/flows/` because it needs the network, the dev build and credentials, so
`e2e/run.sh` and the "Android end-to-end" check do not run it.

## CI

`.github/workflows/admin-upload-e2e.yml` ("Admin upload end-to-end") runs it by hand (Actions >
Admin upload end-to-end > Run workflow), on pushes to `release*` and `releases/**` branches, and
nightly. It never runs on pull requests: a pull request can change the workflow or the flow, and
would then be able to read the credentials.

One-time setup, by a repository admin, in Settings > Environments:

1. Create the environment `e2e-admin`.
2. Under "Deployment branches and tags", choose "Selected branches and tags" and add `master`,
   `release*` and `releases/**`. Runs from any other branch then get no secrets.
3. Add these environment secrets:

   | Secret | What |
   |---|---|
   | `ADMIN_USER` | Dev admin panel user. Use an account made for this test that can only see Verify, not a person's account. |
   | `ADMIN_PASSWORD` | Its password, 8 characters or more. |
   | `S3_DEV_IDENTITY_POOL_ID` | The dev S3 identity pool. Already a repository secret (the build workflows use it); add it here only to override it. |

## How the credentials are kept out of logs and artifacts

- They are GitHub secrets, given only to the steps that use them (not to the Gradle build). The
  pool id goes into `treetracker.keys.properties` with `printf`, and that file is deleted after
  the build.
- tb gets the admin login as `MAESTRO_ADMIN_USER` / `MAESTRO_ADMIN_PASSWORD` environment
  variables, which tb passes to flows. They never appear on a command line, and the flow only
  names them (`inputText: ${MAESTRO_ADMIN_PASSWORD}`).
- tb prints what it types (`Input text <value>`) and saves it in a failed flow's `log.txt`,
  `screen.txt` and `hierarchy.json`. GitHub masks secrets in the log, and `run.sh` also pipes
  tb's output through `scrub.py` and scrubs every file under `build/e2e-admin` when it exits,
  including after a failure. `scrub.py` replaces each value as is, JSON-escaped and
  HTML-escaped, then deletes any file that still holds a run of 6+ letters and digits from one
  of them, and fails.
- The workflow scrubs once more and uploads the report only if that passes. Screenshots show the
  password field masked, as Chrome draws it.
- The workflow has read-only repository permissions and checks out without keeping a token.

## Run it locally

You need an emulator with emulated cameras, the dev build with a real `s3_dev_identity_pool_id`
in `treetracker.keys.properties`, Chrome, and `tb` on PATH. Read the login into the environment
without echoing it, so it stays out of your shell history:

```bash
./gradlew :app:installDev
read -r MAESTRO_ADMIN_USER; read -rs MAESTRO_ADMIN_PASSWORD; export MAESTRO_ADMIN_USER MAESTRO_ADMIN_PASSWORD
TB=tb bash e2e/admin/run.sh -s emulator-5554
```

The JUnit report and debug files land in `build/e2e-admin/` (`OUT` overrides it).

## Limits

- Like the WebdriverIO test, it checks the newest capture on Verify. If someone else uploads to
  dev between our upload and the check, ours is no longer on top and the run fails. The
  `concurrency` group keeps two runs of this workflow from overlapping.
- Each run adds a planter (phone 5551230001) and a tree to the dev backend.
