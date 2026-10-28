// Plain domain types with no dependencies on the rest of the app: users, orgs, messages, location
// data and capture flow data.
plugins {
    id("treetracker.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.greenstand.android.TreeTracker.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
}