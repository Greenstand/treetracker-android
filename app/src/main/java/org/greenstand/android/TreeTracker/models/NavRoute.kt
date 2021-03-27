package org.greenstand.android.TreeTracker.models

enum class NavRoute(val route: String) {

    SplashScreen("splash"),
    LanguagePickerView("language/{isFromTopBar}"),
    DashboardView("dashboard"),
    SignupView("signup")
}
