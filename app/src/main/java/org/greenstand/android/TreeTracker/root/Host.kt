package org.greenstand.android.TreeTracker.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.get
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@ExperimentalComposeApi
@Composable
fun Host() {

    val navController = LocalNavHostController.current

    TreeTrackerTheme {
        NavHost(navController, startDestination = NavRoute.Splash.route) {
            listOf(
                NavRoute.Splash,
                NavRoute.Language,
                NavRoute.SignupFlow,
                NavRoute.Dashboard,
                NavRoute.Org,
                NavRoute.UserSelect,
                NavRoute.WalletSelect,
                NavRoute.AddWallet,
                NavRoute.TreeImageReview,
                NavRoute.Selfie,
                NavRoute.TreeCapture,
                NavRoute.MessagesUserSelect,
            ).forEach { addNavRoute(it) }
        }
    }
}

fun NavGraphBuilder.addNavRoute(navRoute: NavRoute) {
    addDestination(
        ComposeNavigator.Destination(provider[ComposeNavigator::class], navRoute.content).apply {
            this.route = navRoute.route
            navRoute.arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            navRoute.deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}
