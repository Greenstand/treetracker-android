#!/bin/bash
# SessionStart hook for Claude Code cloud sessions (claude.ai/code).
#
# Gives every contributor's cloud session the same build setup, so tests, ktlint and detekt
# run without first installing a toolchain:
#   - JDK 25 (Gradle needs it) and the Android SDK at the compileSdk in libs.versions.toml
#   - local.properties pointing at that SDK, and the dummy treetracker.keys.properties CI uses
#   - JAVA_HOME and ANDROID_HOME for the session
#   - Gradle and Robolectric dependencies downloaded once, retrying when Maven Central answers
#     429 Too Many Requests, which a cold build often hits
# Local machines are left alone. See docs/getting-started/cloud-sessions.md.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-.}"

log() { echo "[session-start] $*" >&2; }

sudo_if_needed() {
  if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi
}

# --- JDK 25 -------------------------------------------------------------------------------
JAVA_HOME="/usr/lib/jvm/java-25-openjdk-$(dpkg --print-architecture)"
if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  log "Installing JDK 25"
  sudo_if_needed apt-get update -qq
  sudo_if_needed env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-25-jdk-headless >/dev/null
fi
export JAVA_HOME

# --- Android SDK --------------------------------------------------------------------------
ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
BUILD_TOOLS="36.0.0"
COMPILE_SDK="$(sed -n 's/^compileSdk = "\(.*\)"/\1/p' gradle/libs.versions.toml)"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "$SDKMANAGER" ]; then
  log "Installing Android command-line tools into $ANDROID_HOME"
  tmp="$(mktemp -d)"
  curl -fsSL --retry 4 -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  sudo_if_needed mkdir -p "$ANDROID_HOME/cmdline-tools"
  sudo_if_needed rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  sudo_if_needed mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi

sdk_install() {
  yes 2>/dev/null | sudo_if_needed env JAVA_HOME="$JAVA_HOME" "$SDKMANAGER" --sdk_root="$ANDROID_HOME" "$@" >/dev/null
}

if ! compgen -G "$ANDROID_HOME/platforms/android-$COMPILE_SDK*" >/dev/null; then
  log "Installing Android SDK platform $COMPILE_SDK"
  # Newer platforms are published as android-NN.0, older ones as android-NN.
  sdk_install "platforms;android-$COMPILE_SDK.0" || sdk_install "platforms;android-$COMPILE_SDK"
  compgen -G "$ANDROID_HOME/platforms/android-$COMPILE_SDK*" >/dev/null
fi
if [ ! -d "$ANDROID_HOME/build-tools/$BUILD_TOOLS" ] || [ ! -d "$ANDROID_HOME/platform-tools" ]; then
  log "Installing build-tools $BUILD_TOOLS and platform-tools"
  sdk_install "build-tools;$BUILD_TOOLS" "platform-tools"
fi
if [ ! -d "$ANDROID_HOME/licenses" ]; then
  sdk_install --licenses
fi
export ANDROID_HOME

# --- Local files Gradle reads (both are gitignored) ------------------------------------
if ! grep -q '^sdk.dir=' local.properties 2>/dev/null; then
  echo "sdk.dir=$ANDROID_HOME" >> local.properties
fi
if [ ! -f treetracker.keys.properties ]; then
  # The same dummy keys CI uses (.github/workflows/pull_request.yml).
  cat > treetracker.keys.properties <<'KEYS'
#Treetracker API Keys
treetracker_client_id=dummy-id
treetracker_client_secret=dummy-secret
s3_dev_identity_pool_id=dummy-pool-id-dev
s3_test_identity_pool_id=dummy-pool-id-test
KEYS
fi

# --- Session environment ------------------------------------------------------------------
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export JAVA_HOME=\"$JAVA_HOME\""
    echo "export ANDROID_HOME=\"$ANDROID_HOME\""
    echo "export PATH=\"$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:\$PATH\""
  } >> "$CLAUDE_ENV_FILE"
fi

# --- Gradle and Robolectric downloads ----------------------------------------------------
# Compiles the unit tests and runs one package of Robolectric tests, which downloads the
# Android runtime Robolectric loads at test time. Skipped while the build files are unchanged.
hashes="$HOME/.gradle/.treetracker-session-start.sha256"
hashed_files=(
  .claude/hooks/session-start.sh
  gradle/libs.versions.toml
  gradle/wrapper/gradle-wrapper.properties
  settings.gradle
  build.gradle
  app/build.gradle
  gradle.properties
  app/src/test/resources/robolectric.properties
)
if ! sha256sum --check --status "$hashes" 2>/dev/null; then
  rm -f "$hashes"
  warm_log="$(mktemp)"
  for attempt in 1 2 3 4 5 6; do
    log "Downloading Gradle and Robolectric dependencies (attempt $attempt)"
    if ./gradlew --console=plain --quiet :app:compileDebugUnitTestKotlin \
      :app:testDebugUnitTest --tests 'org.greenstand.android.TreeTracker.database.dao.*' \
      >"$warm_log" 2>&1; then
      warmed=true
      break
    fi
    warmed=false
    # Retry only when a download failed; any other failure is the code's, not the setup's.
    if ! grep -qE '429|Too Many Requests|Could not (GET|resolve|download)|MavenArtifactFetcher' "$warm_log"; then
      break
    fi
    sleep $((attempt * 30))
  done
  if [ "$warmed" = true ]; then
    mkdir -p "$(dirname "$hashes")"
    sha256sum "${hashed_files[@]}" > "$hashes"
  else
    # Do not block the session: the toolchain is ready, and the agent can rerun the build.
    log "Warming the Gradle cache failed; the last lines of its output follow."
    tail -n 30 "$warm_log" >&2
  fi
  rm -f "$warm_log"
fi
