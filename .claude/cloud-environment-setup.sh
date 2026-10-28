#!/bin/bash
# Machine setup for Claude Code cloud sessions (claude.ai/code).
#
# Paste this into the cloud environment's "Setup script" field. It runs as root on
# Ubuntu 24.04 before Claude starts, and the result is cached for about 7 days when it
# finishes within about 5 minutes. The SessionStart hook also runs it as a fallback when
# the SDK is missing. See docs/getting-started/claude-cloud-sessions.md.
#
# Needs dl.google.com on the environment's allowed domains.
set -euo pipefail

SDK=/opt/android-sdk
# Latest Linux command-line tools from https://developer.android.com/studio#command-line-tools-only
CLT_ZIP=commandlinetools-linux-15859902_latest.zip
PLATFORM="platforms;android-36"
BUILD_TOOLS="build-tools;35.0.0"

# JDK 25, the same version CI uses. Ubuntu's build reads the system cacerts, which
# already trusts the cloud proxy's CA.
if [ ! -x /usr/lib/jvm/java-25-openjdk-amd64/bin/java ]; then
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-25-jdk-headless unzip
fi

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$SDK/cmdline-tools"
  tmp=$(mktemp -d)
  curl -fsSL -o "$tmp/clt.zip" "https://dl.google.com/android/repository/$CLT_ZIP"
  unzip -q "$tmp/clt.zip" -d "$tmp"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

sdkmanager() {
  local args=(--sdk_root="$SDK")
  # sdkmanager is a JVM tool. The cloud VM's JAVA_TOOL_OPTIONS usually carries the proxy,
  # but pass it explicitly in case this runs before that is set.
  if [ -n "${HTTPS_PROXY:-}" ]; then
    local p=${HTTPS_PROXY#*://}; p=${p%/}
    args+=(--proxy=http --proxy_host="${p%:*}" --proxy_port="${p##*:}")
  fi
  "$SDK/cmdline-tools/latest/bin/sdkmanager" "${args[@]}" "$@"
}

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "$PLATFORM" "$BUILD_TOOLS" >/dev/null

cat > /etc/profile.d/android-sdk.sh <<EOP
export ANDROID_HOME=$SDK
export ANDROID_SDK_ROOT=$SDK
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
EOP
