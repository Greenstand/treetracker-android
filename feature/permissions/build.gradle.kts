// Permissions screen and dialogs (location, camera, activity recognition).
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.permissions"
}

dependencies {
    implementation(libs.accompanist.permissions)
}