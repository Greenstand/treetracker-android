plugins {
    `kotlin-dsl`
}

group = "org.greenstand.android.TreeTracker.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compose.compiler.gradlePlugin)
    compileOnly(libs.ktlint.gradle)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.spotless.gradle)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "treetracker.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "treetracker.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "treetracker.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "treetracker.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("codeQuality") {
            id = "treetracker.code-quality"
            implementationClass = "CodeQualityConventionPlugin"
        }
    }
}
