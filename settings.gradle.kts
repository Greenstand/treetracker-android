pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://jitpack.io") }
    }
}
include(":app")
include(":core:analytics")
include(":core:common")
include(":core:config")
include(":core:data")
include(":core:database")
include(":core:designsystem")
include(":core:model")
include(":core:navigation")
include(":core:network")
include(":core:preferences")
include(":core:resources")
include(":core:testing")
include(":core:ui")
include(":feature:languagepicker")
include(":feature:orgpicker")
include(":feature:permissions")
include(":feature:profile")
include(":feature:sessionnote")
include(":feature:settings")
include(":feature:signup")
include(":feature:splash")
include(":feature:treeheight")
include(":feature:walletselect")
rootProject.name = "treetracker-android"
