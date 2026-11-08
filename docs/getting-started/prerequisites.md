# Prerequisites

Required: request the `treetracker.keys.properties` file from the `#android_chat` Slack channel before building. This file contains AWS S3 credentials, API client IDs, and secrets for all build variants.

Required: JDK 25 to run Gradle. Android Studio's bundled JDK is 25, so builds from Android Studio
work as is. From a terminal, point `JAVA_HOME` at a JDK 25 install, for example Android Studio's:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```
