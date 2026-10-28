// The app's Android resources: strings (en, pt, sw), drawables, fonts, colors, themes and launcher
// icons. Modules reference them through org.greenstand.android.TreeTracker.resources.R.
// Phase 2 of the KMP plan moves these to Compose Multiplatform resources.
plugins {
    id("treetracker.android.library")
}

android {
    namespace = "org.greenstand.android.TreeTracker.resources"
}

dependencies {
    // styles.xml themes extend Theme.AppCompat.
    implementation(libs.androidx.appcompat)
}