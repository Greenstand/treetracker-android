package org.greenstand.android.TreeTracker.root

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

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
                NavRoute.AddOrg,
                NavRoute.TreeImageReview,
                NavRoute.Selfie,
                NavRoute.TreeCapture,
                NavRoute.Survey,
                NavRoute.MessagesUserSelect,
                NavRoute.IndividualMessageList,
                NavRoute.Chat,
                NavRoute.Announcement
            ).forEach { addNavRoute(it) }
        }
    }
}

fun NavGraphBuilder.addNavRoute(navRoute: NavRoute) {
    composable(
        route = navRoute.route,
        arguments = navRoute.arguments,
        deepLinks = navRoute.deepLinks,
    ) { backStackEntry ->
        navRoute.content(backStackEntry)
    }
}
