# Claude Code Cloud Sessions

Claude Code on the web (claude.ai/code) runs each session on a fresh Ubuntu 24.04 VM. The VM
has JDK 21 and Gradle, but no Android SDK. Two pieces in `.claude/` get it ready to build.

## One-time environment setup

In the cloud environment's settings:

1. **Network access**: choose **Custom**, tick **Also include default list of common package
   managers**, and add these domains:

   ```
   dl.google.com
   maven.google.com
   edgedl.me.gvt1.com
   maven-central.storage-download.googleapis.com
   jitpack.io
   oss.sonatype.org
   ```

   `dl.google.com` is the one that matters. The default Trusted list doesn't include it, and
   `maven.google.com` redirects there, so without it neither the SDK nor AGP and AndroidX can
   download.

2. **Setup script**: paste in the contents of `.claude/cloud-environment-setup.sh`. It installs
   JDK 25, the Android command-line tools, `platform-tools`, `platforms;android-36` and
   `build-tools;35.0.0`, and the result is cached between sessions.

The setup script is optional. Without it, the SessionStart hook runs the same script on the first
session, which makes that session start a few minutes slower.

## What the SessionStart hook does

`.claude/hooks/cloud-android.sh` runs at the start or resume of every session, and exits straight
away unless `CLAUDE_CODE_REMOTE=true`, so it does nothing on your own machine. In a cloud session
it:

- Runs the setup script if the SDK or JDK 25 is missing.
- Installs the `compileSdk` platform from `gradle/libs.versions.toml` if it isn't installed yet.
- Sets `JAVA_HOME` and `ANDROID_HOME` for the session.
- Writes `local.properties` with `sdk.dir` if it's missing.
- Writes a dummy `treetracker.keys.properties`, with the same values CI uses for tests.
- Adds a Gradle init script in `~/.gradle/init.d` that points Maven Central at Google's official
  mirror, because `repo.maven.apache.org` answers 429 to the cloud's shared IP.
- Starts `./gradlew help` in the background so the wrapper and dependencies download early.

## What works

Unit tests, Robolectric tests, lint, detekt, spotless and `assemble*` tasks. The VM has no KVM,
so emulators and `connected*AndroidTest` tasks don't run there.
