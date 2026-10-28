// Screen-level UI that several features share: the user picker (UserSelect + its ViewModel), the
// camera (CameraX preview, selfie and image review screens, ImageCaptureActivity and its result
// contract), and the language button. `uiModule` registers its ViewModels and the selfie and
// image review screens.
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
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:navigation")))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.androidx.arch.core.testing)
}