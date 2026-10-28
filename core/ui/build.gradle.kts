// Screen-level UI that several features share: the user picker (UserSelect + its ViewModel), the
// camera (CameraX preview, selfie and image review screens, ImageCaptureActivity and its result
// contract), the language button, and the permission request prompt used by the capture flow.
plugins {
    id("treetracker.android.library")
    id("treetracker.android.compose")
}

android {
    namespace = "org.greenstand.android.TreeTracker.ui"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:designsystem"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.bundles.camerax)
    implementation(libs.accompanist.permissions)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:navigation")))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.androidx.arch.core.testing)
}