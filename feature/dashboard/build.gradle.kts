// Dashboard: the home screen (tree counts, upload, messages, track).
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.dashboard"
}

dependencies {
    implementation(libs.androidx.work.runtime.ktx)
}