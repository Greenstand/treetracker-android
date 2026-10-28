// Analytics events and exception logging, backed by Firebase Analytics and Crashlytics.
plugins {
    id("treetracker.android.library")
}

android {
    namespace = "org.greenstand.android.TreeTracker.analytics"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:config"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.timber)
}