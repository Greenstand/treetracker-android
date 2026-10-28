// Small Android and Kotlin helpers with no app dependencies: device id, GPS and image helpers,
// validation, string and state extensions, constants.
plugins {
    id("treetracker.android.library")
}

android {
    namespace = "org.greenstand.android.TreeTracker.common"
}

dependencies {
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}