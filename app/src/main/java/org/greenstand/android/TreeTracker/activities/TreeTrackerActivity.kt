package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.camera.CameraScreen
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.models.*
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.signup.NameEntryView
import org.greenstand.android.TreeTracker.signup.SignupFlow
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.koin.android.ext.android.inject

val LocalViewModelFactory = compositionLocalOf<TreeTrackerViewModelFactory> { error { "No active ViewModel factory found!" } }
val LocalNavHostController = compositionLocalOf<NavHostController> { error { "No NavHostController found!" } }

class TreeTrackerActivity : ComponentActivity() {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FeatureFlags.USE_SWAHILI) {
            languageSwitcher.setLanguage(Language.SWAHILI, resources)
        } else {
            languageSwitcher.applyCurrentLanguage(this)
        }

        setContent {
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalViewModelFactory provides viewModelFactory,
                LocalNavHostController provides navController
            ) {
                Host()
            }
        }
    }
}

@Composable
private fun Host() {

    val navController = LocalNavHostController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    TreeTrackerTheme {
        NavHost(navController, startDestination = NavRoute.Splash.route) {
            composable(NavRoute.Splash.route) {
                SplashScreen()
            }

            composable(
                route = NavRoute.Language.route,
                arguments = NavRoute.Language.arguments
            ) { backStackEntry ->
                LanguageSelectScreen(
                    isFromTopBar = NavRoute.Language.isFromTopBar(backStackEntry)
                )
            }

            composable(NavRoute.SignupFlow.route) {
                SignupFlow()
            }

            composable(NavRoute.NameEntryView.route) {
                NameEntryView()
            }

            composable(NavRoute.Dashboard.route) {
                DashboardScreen()
            }

            composable(NavRoute.Org.route) {
                OrgPickerScreen()
            }

            composable(NavRoute.UserSelect.route) {
                UserSelectScreen()
            }

            composable(
                route = NavRoute.WalletSelect.route,
                arguments = NavRoute.WalletSelect.arguments
            ) {
                WalletSelectScreen(planterInfoId = NavRoute.WalletSelect.getPlanterInfoId(it))
            }

            composable(
                route = NavRoute.Camera.route,
                arguments = NavRoute.Camera.arguments
            ) {
                CameraScreen(
                    isSelfieMode = NavRoute.Camera.isSelfieMode(it)
                )
            }
        }
    }
}
