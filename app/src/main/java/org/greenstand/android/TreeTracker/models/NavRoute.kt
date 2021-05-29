package org.greenstand.android.TreeTracker.models

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.NamedNavArgument
import androidx.navigation.compose.navArgument

sealed class NavRoute {

    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()
    open val deepLinks: List<NavDeepLink> = emptyList()

    object Splash : NavRoute() {
        override val route: String = "splash"
    }

    object Phone : NavRoute() {
        override val route: String = "phone"
    }

    object SignupFlow : NavRoute() {
        override val route: String = "signup-flow"
    }

    object NameEntryView : NavRoute() {
        override val route: String = "signup-flow/nameEntryView"
    }

    object Name : NavRoute() {
        override val route: String = "name/{identifier}"
        override val arguments = listOf(navArgument("identifier") { type = NavType.StringType })

        fun getIdentifier(backStackEntry: NavBackStackEntry): Boolean {
            return backStackEntry.arguments?.getBoolean("identifier") ?: false
        }

        fun create(identifier: Boolean) = "camera/$identifier"
    }

    object Org : NavRoute() {
        override val route: String = "org"
    }

    object Dashboard : NavRoute() {
        override val route: String = "dashboard"
    }

    object UserSelect : NavRoute() {
        override val route: String = "user-select"
    }

    object WalletSelect : NavRoute() {
        override val route: String = "wallet-select/{planterInfoId}"
        override val arguments = listOf(navArgument("planterInfoId") { type = NavType.LongType })

        fun getPlanterInfoId(backStackEntry: NavBackStackEntry): Long {
            return backStackEntry.arguments?.getLong("planterInfoId") ?: -1
        }

        fun create(planterInfoId: Long) = "wallet-select/$planterInfoId"
    }

    object Camera : NavRoute() {
        override val route: String = "camera/{isSelfieMode}"
        override val arguments = listOf(navArgument("isSelfieMode") { type = NavType.BoolType })

        fun isSelfieMode(backStackEntry: NavBackStackEntry): Boolean {
            return backStackEntry.arguments?.getBoolean("isSelfieMode") ?: false
        }

        fun create(isSelfieMode: Boolean) = "camera/$isSelfieMode"
    }

    object ImageReview : NavRoute() {
        override val route: String = "camera-review/{photoPath}"
        override val arguments = listOf(navArgument("photoPath") { type = NavType.StringType })

        fun photoPath(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("photoPath")?.replace('*', '/') ?: ""
        }

        fun create(photoPath: String): String {
            return "camera-review/${photoPath.replace('/', '*')}"
        }
    }

    object Language : NavRoute() {
        override val route: String = "language/{isFromTopBar}"
        override val arguments = listOf(navArgument("isFromTopBar") { type = NavType.BoolType })

        fun isFromTopBar(backStackEntry: NavBackStackEntry): Boolean {
            return backStackEntry.arguments?.getBoolean("isFromTopBar") ?: false
        }

        fun create(isFromTopBar: Boolean = true) = "language/$isFromTopBar"
    }
}
