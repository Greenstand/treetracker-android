import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.google.services)
}

fun loadExtraProperties(fileName: String) {
    val file = file(fileName)
    if (!file.exists()) return
    val props = Properties()
    props.load(FileInputStream(file))
    for (key in props.stringPropertyNames()) {
        project.ext.set(key, props.getProperty(key))
    }
}

loadExtraProperties("treetracker.keys.properties")

android {
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        applicationId = "org.greenstand.android.TreeTracker"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 197
        versionName = "2.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", "\"${project.property("s3_production_identity_pool_id")}\"")
        buildConfigField("String", "OBJECT_STORAGE_IDENTITY_REGION", "\"eu-central-1\"")
        buildConfigField("String", "OBJECT_STORAGE_ENDPOINT", "\"eu-central-1\"")
        buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-production-images\"")
        buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-production-batch-uploads\"")
        buildConfigField("String", "API_GATEWAY", "\"https://dev-k8s.treetracker.org\"")
        buildConfigField("String", "TREETRACKER_CLIENT_ID", "\"${project.property("prod_treetracker_client_id")}\"")
        buildConfigField("String", "TREETRACKER_CLIENT_SECRET", "\"${project.property("prod_treetracker_client_secret")}\"")
        buildConfigField("Boolean", "TREE_HEIGHT_FEATURE_ENABLED", "false")
        buildConfigField("Boolean", "TREE_NOTE_FEATURE_ENABLED", "true")
        buildConfigField("Boolean", "TREE_DBH_FEATURE_ENABLED", "false")
        buildConfigField("Boolean", "AUTOMATIC_SIGN_OUT_FEATURE_ENABLED", "true")
        buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
        buildConfigField("Boolean", "USE_AWS_S3", "false")
        buildConfigField("Boolean", "ORG_LINK", "false")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Tree Tracker(debug)")
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", "\"${project.property("s3_test_identity_pool_id")}\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-test-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-test-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "true")
            buildConfigField("String", "API_GATEWAY", "\"https://dev-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", "\"${project.property("test_treetracker_client_id")}\"")
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", "\"${project.property("test_treetracker_client_secret")}\"")
        }

        getByName("release") {
            isMinifyEnabled = true
            resValue("string", "app_name", "Tree Tracker")
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", "\"${project.property("s3_production_identity_pool_id")}\"")
            buildConfigField("String", "OBJECT_STORAGE_ENDPOINT", "\"eu-central-1\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-production-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-production-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
            buildConfigField("String", "API_GATEWAY", "\"https://prod-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", "\"${project.property("prod_treetracker_client_id")}\"")
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", "\"${project.property("prod_treetracker_client_secret")}\"")
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        create("prerelease") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".prerelease"
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", "\"${project.property("s3_production_identity_pool_id")}\"")
            buildConfigField("String", "OBJECT_STORAGE_ENDPOINT", "\"eu-central-1\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-production-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-production-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
            resValue("string", "app_name", "Tree Tracker Prerelease")
            buildConfigField("String", "API_GATEWAY", "\"https://prod-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", "\"${project.property("prod_treetracker_client_id")}\"")
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", "\"${project.property("prod_treetracker_client_secret")}\"")
        }

        create("beta") {
            applicationIdSuffix = ".test"
            resValue("string", "app_name", "Tree Tracker(test)")
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", "\"${project.property("s3_test_identity_pool_id")}\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-test-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-test-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
            buildConfigField("String", "API_GATEWAY", "\"https://test-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", "\"${project.property("test_treetracker_client_id")}\"")
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", "\"${project.property("test_treetracker_client_secret")}\"")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }

        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "Tree Tracker(dev)")
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", "\"${project.property("s3_dev_identity_pool_id")}\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-dev-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-dev-batch-uploads\"")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "true")
            buildConfigField("String", "API_GATEWAY", "\"https://dev-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", "\"${project.property("dev_treetracker_client_id")}\"")
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", "\"${project.property("dev_treetracker_client_secret")}\"")
        }
    }

    packaging {
        resources {
            excludes +=
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
            isIncludeAndroidResources = gradle.startParameter.taskNames.any { it.lowercase().contains("roborazzi") }
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
val roborazziOutputDir = file("${layout.buildDirectory.get().asFile}/outputs/roborazzi")

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

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
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

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

    // Testing
    testImplementation(libs.bundles.testing)
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