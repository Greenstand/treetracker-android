// The data and domain layer: repositories (users, orgs, messages), uploaders and sync, tree and
// session tracking, location and sensor capture, and use cases.
// Also the messages Room database (MessageDatabase) and the background sync worker.
plugins {
    id("treetracker.android.library")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.greenstand.android.TreeTracker.data"

    // Robolectric tests read strings (e.g. the sync notification text).
    testOptions.unitTests.isIncludeAndroidResources = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:analytics"))
    api(project(":core:common"))
    api(project(":core:config"))
    api(project(":core:database"))
    api(project(":core:model"))
    api(project(":core:navigation"))
    api(project(":core:network"))
    api(project(":core:preferences"))
    implementation(project(":core:resources"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.aws.android.sdk.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.installations)
    implementation(libs.install.referrer)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:navigation")))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
}