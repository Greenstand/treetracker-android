// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        classpath(libs.android.gradlePlugin)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.kotlin.compose.compiler.gradlePlugin)
        classpath(libs.kotlin.serialization.gradlePlugin)
        classpath(libs.roborazzi.gradlePlugin)

        classpath(libs.ktlint.gradle)
        classpath(libs.spotless.gradle)
        classpath(libs.detekt.gradlePlugin)
        classpath(libs.ksp.gradlePlugin)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        verbose.set(true)
        filter {
            exclude { element -> element.file.path.contains("generated/") }
        }
    }
    apply(plugin = "dev.detekt")
    configure<dev.detekt.gradle.extensions.DetektExtension> {
        config.setFrom(files("${project.rootDir}/detekt.yml"))
        parallel.set(true)
        buildUponDefaultConfig.set(true)
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
