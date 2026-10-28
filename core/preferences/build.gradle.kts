// Preferences: the SharedPreferences wrapper (Preferences) and the keys the app stores.
plugins {
    id("treetracker.android.library")
}

android {
    namespace = "org.greenstand.android.TreeTracker.preferences"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}