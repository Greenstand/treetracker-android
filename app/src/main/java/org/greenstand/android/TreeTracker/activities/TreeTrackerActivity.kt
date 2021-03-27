package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.databinding.TreeTrackerActivityBinding
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.models.*
import org.greenstand.android.TreeTracker.signup.SignupFlow
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.android.ext.android.inject

val LocalViewModelFactory = compositionLocalOf<TreeTrackerViewModelFactory> { error { "No active ViewModel factory found!" } }
val LocalNavHostController = compositionLocalOf<NavHostController> { error { "No NavHostController found!" } }

class TreeTrackerActivity : ComponentActivity() {

    lateinit var bindings: TreeTrackerActivityBinding

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

    val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)

    TreeTrackerTheme {
        NavHost(navController, startDestination = NavRoute.SplashScreen.route) {
            composable(NavRoute.SplashScreen.route) {
                SplashScreen()
            }

            composable(
                route = NavRoute.LanguagePickerView.route,
                arguments = listOf(navArgument("isFromTopBar") { type = NavType.BoolType })
            ) { backStackEntry ->
                LanguageSelectScreen(
                    isFromTopBar = backStackEntry.arguments?.getBoolean("isFromTopBar") ?: false
                )
            }

            composable(NavRoute.DashboardView.route) {
                DashboardScreen()
            }

            composable(NavRoute.SignupView.route) {
                SignupFlow()
            }
        }
    }
}
