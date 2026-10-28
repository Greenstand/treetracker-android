// Shared test helpers for unit tests in every module: MainCoroutineRule, FakeFileGenerator (fake
// entities and models) and LiveData helpers. Add with testImplementation(project(":core:testing")).
plugins {
    id("treetracker.android.library")
}

android {
    namespace = "org.greenstand.android.TreeTracker.testing"
}

dependencies {
    api(project(":core:data"))

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.androidx.lifecycle.livedata)
    implementation(libs.kotlinx.datetime)
}