// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        verbose.set(true)
        filter {
            exclude { element -> element.file.path.contains("generated/") }
        }
    }

    ext {
        // Keys properties - these still need to be loaded from treetracker.keys.properties
        set("s3_production_identity_pool_id", "configure in treetracker.keys.properties")
        set("prod_treetracker_client_id", "configure in treetracker.keys.properties")
        set("prod_treetracker_client_secret", "configure in treetracker.keys.properties")
        set("s3_test_identity_pool_id", "configure in treetracker.keys.properties")
        set("test_treetracker_client_id", "configure in treetracker.keys.properties")
        set("test_treetracker_client_secret", "configure in treetracker.keys.properties")
        set("s3_dev_identity_pool_id", "configure in treetracker.keys.properties")
        set("dev_treetracker_client_id", "configure in treetracker.keys.properties")
        set("dev_treetracker_client_secret", "configure in treetracker.keys.properties")
        set("treetracker_client_id", "configure in treetracker.keys.properties")
        set("treetracker_client_secret", "configure in treetracker.keys.properties")
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(files("${project.rootDir}/detekt.yml"))
        parallel = true
        buildUponDefaultConfig = true
    }
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${project.rootDir}/build-logic/**/*.kt")
            licenseHeaderFile(
                rootProject.file("${project.rootDir}/spotless/copyright.kt"),
                "^(package|object|import|interface)",
            )
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}