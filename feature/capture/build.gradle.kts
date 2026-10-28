// Tree capture flow: camera capture, tree image review and the capture tutorials.
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.capture"
}

dependencies {
    implementation(libs.accompanist.permissions)
}