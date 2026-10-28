// Navigation and screen plumbing: Navigation 3 routes, the Navigator, org deep links, screen
// tracking, and the base ViewModel / UI event / snackbar types screens share.
plugins {
    id("treetracker.android.library")
    id("treetracker.android.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.greenstand.android.TreeTracker.navigation"

    // FakeNavigator and FakeClock, for this module's tests and other modules' tests.
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(project(":core:analytics"))

    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.animation)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.androidx.navigation3.runtime)
}