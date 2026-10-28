// Settings screen (profile, edit trees, map, privacy policy, log out, delete account).
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.settings"
}

dependencies {
    implementation(libs.maplibre.android.sdk)
}