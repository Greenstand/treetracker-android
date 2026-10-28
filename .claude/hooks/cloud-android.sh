#!/bin/bash
# SessionStart hook: gets a Claude Code cloud session ready to build this app.
# Does nothing outside cloud sessions, so local machines are untouched.
# See docs/getting-started/claude-cloud-sessions.md.
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
set -euo pipefail
cd "$CLAUDE_PROJECT_DIR"

SDK=/opt/android-sdk
JDK=/usr/lib/jvm/java-25-openjdk-amd64

# Fallback when the environment has no setup script, or its cache predates a change.
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ] || [ ! -x "$JDK/bin/java" ]; then
  bash .claude/cloud-environment-setup.sh
fi

# Install the compileSdk platform if the version catalog moved past what's installed.
compile_sdk=$(sed -nE 's/^compileSdk *= *"([0-9]+)".*/\1/p' gradle/libs.versions.toml | head -1)
if [ -n "$compile_sdk" ] && [ ! -d "$SDK/platforms/android-$compile_sdk" ]; then
  "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platforms;android-$compile_sdk" >/dev/null
fi

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export ANDROID_HOME=$SDK"
    echo "export ANDROID_SDK_ROOT=$SDK"
    echo "export JAVA_HOME=$JDK"
    echo "export PATH=$JDK/bin:$SDK/platform-tools:\$PATH"
  } >> "$CLAUDE_ENV_FILE"
fi

# A fresh clone has no local.properties, and AGP stops with "SDK location not found".
[ -f local.properties ] || echo "sdk.dir=$SDK" > local.properties

# Dummy keys, the same values CI uses for tests. Real keys never go to the cloud.
if [ ! -f treetracker.keys.properties ]; then
  cat > treetracker.keys.properties <<'KEYS'
#Treetracker API Keys (dummy, for Claude cloud sessions)
treetracker_client_id=dummy-id
treetracker_client_secret=dummy-secret
s3_dev_identity_pool_id=dummy-pool-id-dev
s3_test_identity_pool_id=dummy-pool-id-test
prod_treetracker_client_id=dummy-prod-id
prod_treetracker_client_secret=dummy-prod-secret
s3_production_identity_pool_id=dummy-pool-id-prod
KEYS
fi

# Maven Central answers 429 to the cloud's shared egress IP, so send it to Google's
# official Maven Central mirror. Lives in ~/.gradle, so it never affects local builds.
mkdir -p "$HOME/.gradle/init.d"
cat > "$HOME/.gradle/init.d/maven-central-mirror.gradle" <<'GRADLE'
def MIRROR = 'https://maven-central.storage-download.googleapis.com/maven2/'
def useMirror = { RepositoryHandler repos ->
    repos.withType(MavenArtifactRepository).configureEach { r ->
        if (r.name == 'MavenRepo' || r.url.host in ['repo.maven.apache.org', 'repo1.maven.org']) r.url = MIRROR
    }
}
beforeSettings { s -> useMirror(s.pluginManagement.repositories); useMirror(s.dependencyResolutionManagement.repositories) }
beforeProject { p -> useMirror(p.buildscript.repositories); useMirror(p.repositories) }
GRADLE

# Download the wrapper and dependencies in the background so they're ready sooner.
JAVA_HOME=$JDK ANDROID_HOME=$SDK nohup ./gradlew --quiet help > /tmp/gradle-warmup.log 2>&1 &
exit 0
