// Settings: the settings screen (privacy policy, log out, delete account, links to profiles, tree
// editing and the map) and the profile screens (pick, view and edit, delete).
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.settings"
}

dependencies {
    implementation(libs.maplibre.android.sdk)
}