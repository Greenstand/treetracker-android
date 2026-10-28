# Claude Code Cloud Sessions

Claude Code on the web (claude.ai/code) runs each session on a fresh Ubuntu 24.04 VM. The VM
has JDK 21 and Gradle, but no Android SDK. The SDK and JDK 25 come from the cloud environment's
settings. The repo adds one SessionStart hook for the part those settings can't reach.

## Environment settings

Open the environment's settings in Claude Code on the web.

**Network access**: **Full** works. With **Custom**, tick **Also include default list of common
package managers** and add:

```
dl.google.com
maven.google.com
edgedl.me.gvt1.com
maven-central.storage-download.googleapis.com
jitpack.io
oss.sonatype.org
```

The default **Trusted** list doesn't include `dl.google.com`, and `maven.google.com` redirects
there, so neither the SDK nor AGP and AndroidX can download.

**Setup script** (runs as root before Claude starts, and is cached between sessions):

```bash
#!/bin/bash
set -euo pipefail
SDK=/opt/android-sdk

# JDK 25 (matches CI)
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-25-jdk-headless unzip

# Android cmdline-tools + SDK packages
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$SDK/cmdline-tools"
  curl -fsSL -o /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
  unzip -q /tmp/clt.zip -d /tmp/clt
  mv /tmp/clt/cmdline-tools "$SDK/cmdline-tools/latest"
  rm -rf /tmp/clt /tmp/clt.zip
fi
SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager --sdk_root=$SDK"
yes | $SDKM --licenses >/dev/null 2>&1 || true
$SDKM "platform-tools" "platforms;android-36" "build-tools;35.0.0" >/dev/null

# Maven Central rate-limits the shared cloud IP (429), so use Google's mirror
mkdir -p ~/.gradle/init.d
cat > ~/.gradle/init.d/maven-central-mirror.gradle <<'EOF'
def MIRROR = 'https://maven-central.storage-download.googleapis.com/maven2/'
def useMirror = { RepositoryHandler repos ->
    repos.withType(MavenArtifactRepository).configureEach { r ->
        if (r.name == 'MavenRepo' || r.url.host in ['repo.maven.apache.org', 'repo1.maven.org']) r.url = MIRROR
    }
}
beforeSettings { s -> useMirror(s.pluginManagement.repositories); useMirror(s.dependencyResolutionManagement.repositories) }
beforeProject { p -> useMirror(p.buildscript.repositories); useMirror(p.repositories) }
EOF
```

When `compileSdk` or the build tools change, update the package names in the script.

**Environment variables**:

```
ANDROID_HOME=/opt/android-sdk
ANDROID_SDK_ROOT=/opt/android-sdk
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
```

## SessionStart hook

`app/build.gradle` reads `treetracker.keys.properties` and fails when it's missing. That file
belongs in the checkout, which the environment settings can't reach. So
`.claude/hooks/cloud-dummy-keys.sh` writes it with the dummy values CI uses for tests. It does
nothing outside cloud sessions, or when the file already exists. Real keys never go to the cloud.

## What works

Unit tests, Robolectric tests, lint, detekt, spotless and `assemble*` tasks. The VM has no KVM,
so emulators and `connected*AndroidTest` tasks don't run there.
