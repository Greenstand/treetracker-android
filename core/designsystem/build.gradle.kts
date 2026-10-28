// The design system: theme, colors, typography, and the shared Compose components (buttons,
// action bar, text fields, dialogs, images) that screens are built from.
plugins {
    id("treetracker.android.library")
    id("treetracker.android.compose")
}

android {
    namespace = "org.greenstand.android.TreeTracker.designsystem"
}

dependencies {
    implementation(project(":core:config"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    api(project(":core:resources"))

    api(libs.bundles.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)
}