# Cloud Sessions (Claude Code on the web)

Contributors who work with Claude Code in a cloud session (claude.ai/code) get the build setup
from a committed SessionStart hook, [`.claude/hooks/session-start.sh`](../../.claude/hooks/session-start.sh),
registered in [`.claude/settings.json`](../../.claude/settings.json). Every session starts with
the same toolchain, so an agent can run unit tests, ktlint and detekt without installing
anything first.

## What the hook does

It runs only in cloud sessions (`CLAUDE_CODE_REMOTE=true`); on your own machine it exits at once.

1. Installs JDK 25 with apt if it is missing.
2. Installs the Android command-line tools into `ANDROID_HOME` (default `/opt/android-sdk`), the
   platform for the `compileSdk` in `gradle/libs.versions.toml`, build-tools 36.0.0 and
   platform-tools, if any of them is missing.
3. Writes `sdk.dir` to `local.properties`, and the dummy `treetracker.keys.properties` that CI
   uses, if they are missing. Both files are gitignored. The dummy keys build and test the app,
   but cannot upload to the real backends.
4. Sets `JAVA_HOME`, `ANDROID_HOME` and `PATH` for the session.
5. Compiles the unit tests and runs the DAO tests once, which downloads every Gradle dependency
   and the Android runtime Robolectric loads at test time. Maven Central often answers a cold
   build with `429 Too Many Requests`, so this step retries with backoff. It is skipped while
   the hook and the build files (`libs.versions.toml`, the `build.gradle` files,
   `settings.gradle`, `gradle.properties`, the wrapper and `robolectric.properties`) are
   unchanged since it last succeeded. If it fails for any other reason, the session still
   starts and the hook prints the end of the Gradle output.

The hook runs synchronously: the session waits for it, so the first message never races a
half-installed toolchain. The first start of a new container takes several minutes; later
starts, resumes and compactions take seconds, because every step finds its work done.

## What it does not do

- No emulator: end-to-end flows run on CI. See [Running Flows on CI from a Cloud Session](../testing/ci-flows.md).
- No real API keys. Ask in `#android_chat` for `treetracker.keys.properties` if you need them,
  and never commit them.

## Changing it

- New compileSdk: bump `libs.versions.toml`; the hook installs the new platform on the next start.
- New build-tools version: update `BUILD_TOOLS` in the hook.
- Test a change by running the hook directly:

  ```bash
  CLAUDE_CODE_REMOTE=true CLAUDE_PROJECT_DIR=$PWD ./.claude/hooks/session-start.sh
  ```

  Run it twice; the second run should skip every install.
