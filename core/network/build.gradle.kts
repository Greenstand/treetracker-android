// Network access: the S3 object storage client, upload request types, and the messaging API
// (Retrofit).
plugins {
    id("treetracker.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.greenstand.android.TreeTracker.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:config"))

    implementation(libs.aws.android.sdk.core)
    implementation(libs.aws.android.sdk.s3)
    api(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}