// Trees: the tree list, tree detail and edit screens (with their planter picker), and the map of
// trees.
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.trees"
}

dependencies {
    implementation(libs.maplibre.android.sdk)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.compose.material.icons.extended)
}