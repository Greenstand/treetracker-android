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

- `.claude/cloud-environment-setup.sh`: machine setup (JDK 25, cmdline-tools, SDK packages),
  meant for the environment's Setup script field.
- `.claude/hooks/cloud-android.sh` and `.claude/settings.json`: a SessionStart hook for
  per-session project setup. It exits immediately outside cloud sessions.
- `docs/getting-started/claude-cloud-sessions.md`, linked from `CLAUDE.md`.

## Not verified

The SDK download and a full `./gradlew assembleDebug` weren't run, because the session that wrote
this had no access to `dl.google.com`. The first cloud session with the allowlist set is the test.
