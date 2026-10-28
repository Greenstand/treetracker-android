// The data and domain layer: repositories (users, orgs, messages), uploaders and sync, tree and
// session capture, location and sensor capture, use cases, and the capture flow controllers.
// Also the messages Room database (MessageDatabase).
plugins {
    id("treetracker.android.library")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.greenstand.android.TreeTracker.data"
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

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.ktx)
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

    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}