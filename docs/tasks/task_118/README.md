# Task 118 - SessionStart hook for cloud sessions

## Problem

A contributor's agent in a Claude Code cloud session starts with no JDK 25, no Android SDK, no
`local.properties` and no keys file, so it cannot run a single unit test. Each agent installed
the toolchain its own way, or skipped the tests. A cold Gradle build also often fails on
`429 Too Many Requests` from Maven Central, during dependency resolution and again when
Robolectric downloads its Android runtime at test time.

## Change

- `.claude/hooks/session-start.sh`, registered in `.claude/settings.json`. In cloud sessions
  only, it installs JDK 25 and the Android SDK (platform from `compileSdk`, build-tools 36.0.0,
  platform-tools) when missing, writes `local.properties` and the CI dummy keys when missing,
  sets `JAVA_HOME`/`ANDROID_HOME`/`PATH`, and warms the Gradle and Robolectric caches with
  retries on rate limits. The warm-up is skipped while the build files are unchanged.
- `docs/getting-started/cloud-sessions.md` explains it, linked from `CLAUDE.md`, the docs index
  and Environment Setup.
