// Map screen (trees on a MapLibre map).
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.map"
}

dependencies {
    implementation(libs.maplibre.android.sdk)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.extended)
}