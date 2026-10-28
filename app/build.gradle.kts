plugins {
    id("treetracker.android.application")
    id("treetracker.android.compose")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.firebase.crashlytics")
    id("io.github.takahirom.roborazzi")
}

android {
    defaultConfig {
        applicationId = "org.greenstand.android.TreeTracker"
        // Version lives in gradle.properties, shared with core/config. Release builds use
        // baseVersionCode as is; other CI builds add the workflow's run number (see
        // .github/workflows/actions/build-apk), without committing anything.
        versionCode =
            providers.gradleProperty("treetracker.baseVersionCode").get().toInt() +
            providers.gradleProperty("treetracker.versionCodeOffset").map { it.toInt() }.getOrElse(0)
        versionName = providers.gradleProperty("treetracker.versionName").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            resValue("string", "app_name", "Tree Tracker")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        create("prerelease") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".prerelease"
            resValue("string", "app_name", "Tree Tracker Prerelease")
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Tree Tracker(debug)")
        }

        create("beta") {
            applicationIdSuffix = ".test"
            resValue("string", "app_name", "Tree Tracker(test)")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }

        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "Tree Tracker(dev)")
        }
    }

    // The agent AppFunctions live in src/debug so release, beta and prerelease never ship them.
    // dev is also a local development build, so it compiles the same sources.
    sourceSets {
        getByName("dev") {
            // AGP 9's built-in Kotlin only compiles the kotlin source directories.
            kotlin.directories.add("src/debug/java")
        }
    }

    packaging {
        resources.excludes +=
            setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/io.netty.versions.properties",
                "META-INF/INDEX.LIST",
            )
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }
    namespace = "org.greenstand.android.TreeTracker"

    // includeAndroidResources is only needed for screenshot tests (Compose theme loads
    // fonts from R.font.*). Enabling it globally causes Robolectric to fully initialize
    // TreeTrackerApplication, which starts Koin and breaks existing unit tests.
    testOptions {
        unitTests {
            isIncludeAndroidResources =
                gradle.startParameter.taskNames.any { it.lowercase().contains("roborazzi") }
        }
    }
}

// Screenshot tests and regular unit tests are isolated from each other:
// - `./gradlew test` runs only regular unit tests (excludes screenshot/)
// - `./gradlew verifyRoborazziDebug` runs only screenshot tests (includes screenshot/ only)
// This prevents Koin DI conflicts between the two test suites.
val isRoborazziRun = gradle.startParameter.taskNames.any { it.lowercase().contains("roborazzi") }
tasks.withType<Test>().configureEach {
    if (isRoborazziRun) {
        include("**/screenshot/**")
    } else {
        exclude("**/screenshot/**")
    }
}

// Roborazzi golden images are stored in src/test/snapshots/ and committed to git.
// The plugin always reads/writes build/outputs/roborazzi/, so we sync manually.
val snapshotsDir = file("src/test/snapshots")
val roborazziOutputDir =
    layout.buildDirectory
        .dir("outputs/roborazzi")
        .get()
        .asFile

afterEvaluate {
    tasks.matching { it.name.matches(Regex("test.*UnitTest")) }.configureEach {
        doFirst {
            if (snapshotsDir.exists()) {
                copy {
                    from(snapshotsDir)
                    into(roborazziOutputDir)
                }
            }
        }
        doLast {
            if (roborazziOutputDir.exists()) {
                copy {
                    from(roborazziOutputDir)
                    include("*.png")
                    exclude("*_actual.*", "*_compare.*", "*_delta.*")
                    into(snapshotsDir)
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    // Generates the app-wide AppFunctions index; only debug and dev run the AppFunctions compiler
    arg("appfunctions:aggregateAppFunctions", "true")
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:common"))
    implementation(project(":core:config"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:resources"))
    implementation(project(":core:ui"))
    implementation(project(":feature:languagepicker"))
    implementation(project(":feature:sessionnote"))
    implementation(project(":feature:settings"))

    // AWS S3 SDK
    // Note: android s3 sdk does not support transfer acceleration
    implementation(libs.aws.android.sdk.core)
    implementation(libs.aws.android.sdk.s3)

    // Koin dependencies
    implementation(libs.bundles.koin)
    testImplementation(libs.koin.test)

    // AndroidX Core
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Compose
    implementation(libs.bundles.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.material.icons.extended)

    // MapLibre
    implementation(libs.maplibre.android.sdk)

    // Image Loading
    implementation(libs.coil.compose)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging.interceptor)

    // CameraX
    implementation(libs.bundles.camerax)

    // Logging
    api(libs.timber)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.installations)
    implementation(libs.firebase.config)

    // AppFunctions: debug and dev only, see docs/testing/agent-app-functions.md
    "debugImplementation"(libs.androidx.appfunctions)
    "debugImplementation"(libs.androidx.appfunctions.service)
    "devImplementation"(libs.androidx.appfunctions)
    "devImplementation"(libs.androidx.appfunctions.service)
    "kspDebug"(libs.androidx.appfunctions.compiler)
    "kspDev"(libs.androidx.appfunctions.compiler)

    // Play Install Referrer
    implementation(libs.install.referrer)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(testFixtures(project(":core:navigation")))
    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.turbine)
    testImplementation(libs.turbine)

    // Screenshot testing
    testImplementation(libs.bundles.roborazzi)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }
}

apply(plugin = "com.google.gms.google-services")