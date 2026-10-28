// Tree capture: capture setup (planter, org and wallet, session note) and the capture flow
// (camera, image review, tree height), with the flow controllers and their state, and the location
// and camera permission prompt.
plugins {
    id("treetracker.android.feature")
}

android {
    namespace = "org.greenstand.android.TreeTracker.feature.capture"
}

dependencies {
    implementation(libs.accompanist.permissions)
}