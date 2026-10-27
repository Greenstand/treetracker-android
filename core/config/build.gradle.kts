import java.util.Properties

// Build-time configuration for each build type: API and storage endpoints, secrets, feature
// switches and the app version. Code reads it from
// org.greenstand.android.TreeTracker.config.BuildConfig (and FeatureFlags).
plugins {
    id("treetracker.android.library")
}

// API keys and identity pools come from treetracker.keys.properties (CI writes a dummy one).
// Any key missing from the file falls back to a placeholder.
val keys =
    Properties().apply {
        rootProject.file("treetracker.keys.properties").inputStream().use { load(it) }
    }

fun key(name: String): String = keys.getProperty(name) ?: "configure in treetracker.keys.properties"

fun quoted(value: String) = "\"$value\""

val versionName = providers.gradleProperty("treetracker.versionName").get()
val versionCode =
    providers.gradleProperty("treetracker.baseVersionCode").get().toInt() +
        providers.gradleProperty("treetracker.versionCodeOffset").map { it.toInt() }.getOrElse(0)

android {
    namespace = "org.greenstand.android.TreeTracker.config"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "VERSION_NAME", quoted(versionName))
        buildConfigField("int", "VERSION_CODE", versionCode.toString())
        buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", quoted(key("s3_production_identity_pool_id")))
        buildConfigField("String", "OBJECT_STORAGE_IDENTITY_REGION", "\"eu-central-1\"")
        buildConfigField("String", "OBJECT_STORAGE_ENDPOINT", "\"eu-central-1\"")
        buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-production-images\"")
        buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-production-batch-uploads\"")
        buildConfigField("String", "API_GATEWAY", "\"https://dev-k8s.treetracker.org\"")
        buildConfigField("String", "TREETRACKER_CLIENT_ID", quoted(key("prod_treetracker_client_id")))
        buildConfigField("String", "TREETRACKER_CLIENT_SECRET", quoted(key("prod_treetracker_client_secret")))
        buildConfigField("Boolean", "TREE_HEIGHT_FEATURE_ENABLED", "false")
        buildConfigField("Boolean", "TREE_NOTE_FEATURE_ENABLED", "true")
        buildConfigField("Boolean", "TREE_DBH_FEATURE_ENABLED", "false")
        buildConfigField("Boolean", "AUTOMATIC_SIGN_OUT_FEATURE_ENABLED", "true")
        buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
        buildConfigField("Boolean", "USE_AWS_S3", "false")
        buildConfigField("Boolean", "ORG_LINK", "false")
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", quoted(key("s3_test_identity_pool_id")))
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-test-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-test-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "true")
            buildConfigField("String", "API_GATEWAY", "\"https://dev-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", quoted(key("test_treetracker_client_id")))
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", quoted(key("test_treetracker_client_secret")))
        }

        getByName("release") {
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", quoted(key("s3_production_identity_pool_id")))
            buildConfigField("String", "OBJECT_STORAGE_ENDPOINT", "\"eu-central-1\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-production-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-production-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
            buildConfigField("String", "API_GATEWAY", "\"https://prod-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", quoted(key("prod_treetracker_client_id")))
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", quoted(key("prod_treetracker_client_secret")))
        }

        getByName("prerelease") {
            // Like the app's prerelease: start from debug, then override.
            initWith(getByName("debug"))
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", quoted(key("s3_production_identity_pool_id")))
            buildConfigField("String", "OBJECT_STORAGE_ENDPOINT", "\"eu-central-1\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-production-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-production-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
            buildConfigField("String", "API_GATEWAY", "\"https://prod-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", quoted(key("prod_treetracker_client_id")))
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", quoted(key("prod_treetracker_client_secret")))
        }

        getByName("beta") {
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", quoted(key("s3_test_identity_pool_id")))
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-test-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-test-batch-uploads\"")
            buildConfigField("Boolean", "USE_AWS_S3", "true")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "false")
            buildConfigField("String", "API_GATEWAY", "\"https://test-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", quoted(key("test_treetracker_client_id")))
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", quoted(key("test_treetracker_client_secret")))
        }

        getByName("dev") {
            // Like the app's dev: start from debug, then override.
            initWith(getByName("debug"))
            buildConfigField("String", "OBJECT_STORAGE_IDENTITY_POOL_ID", quoted(key("s3_dev_identity_pool_id")))
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_IMAGES", "\"treetracker-dev-images\"")
            buildConfigField("String", "OBJECT_STORAGE_BUCKET_BATCH_UPLOADS", "\"treetracker-dev-batch-uploads\"")
            buildConfigField("Boolean", "BLUR_DETECTION_ENABLED", "true")
            buildConfigField("String", "API_GATEWAY", "\"https://dev-k8s.treetracker.org\"")
            buildConfigField("String", "TREETRACKER_CLIENT_ID", quoted(key("dev_treetracker_client_id")))
            buildConfigField("String", "TREETRACKER_CLIENT_SECRET", quoted(key("dev_treetracker_client_secret")))
        }
    }
}