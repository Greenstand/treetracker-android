# Task 79 - Android builds in Claude Code cloud sessions

## Goal

Let Claude Code on the web build and test this app, not just read it.

## Findings

- The cloud VM has JDK 21 and Gradle but no Android SDK.
- The default Trusted network blocks `dl.google.com`. `maven.google.com` redirects there, so
  Google Maven fails along with the SDK download. The environment needs Custom network access.
- `repo.maven.apache.org` answered 429 to the shared egress IP. Google's Maven Central mirror
  resolved the same artifacts.
- Gradle needs no proxy or CA settings: the VM's `JAVA_TOOL_OPTIONS` already sets them.
- Ubuntu's `openjdk-25-jdk-headless` uses the system trust store, which trusts the proxy.

## Changes

- `docs/getting-started/claude-cloud-sessions.md`, linked from `CLAUDE.md`: the network access,
  setup script and environment variables to set in the cloud environment. Those settings install
  JDK 25, the Android SDK and a Gradle init script for the Maven Central mirror.
- `.claude/hooks/cloud-dummy-keys.sh` and `.claude/settings.json`: a SessionStart hook that writes
  a dummy `treetracker.keys.properties` in cloud sessions. That's the one piece the environment
  settings can't do, because the file lives in the checkout.

## Decisions

- **Toolchain in the environment, not the repo.** The environment's setup script is cached, and
  keeping it out of the repo leaves local builds and the repo's tooling untouched.

## Not verified

The SDK download and a full `./gradlew assembleDebug` weren't run, because the session that wrote
this had no access to `dl.google.com`. The first cloud session with the allowlist set is the test.
