package org.greenstand.android.TreeTracker.models

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.compose.NamedNavArgument
import androidx.navigation.compose.navArgument
import org.greenstand.android.TreeTracker.camera.ImageReviewScreen
import org.greenstand.android.TreeTracker.camera.SelfieScreen
import org.greenstand.android.TreeTracker.capture.TreeCaptureScreen
import org.greenstand.android.TreeTracker.capture.TreeImageReviewScreen
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.messages.MessagesUserSelectScreen
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.signup.NameEntryView
import org.greenstand.android.TreeTracker.signup.SignupFlow
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen

sealed class NavRoute {

    abstract val content: @Composable (NavBackStackEntry) -> Unit
    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()
    open val deepLinks: List<NavDeepLink> = emptyList()

    object Splash : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SplashScreen()
        }
        override val route: String = "splash"
    }

    object SignupFlow : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SignupFlow()
        }
        override val route: String = "signup-flow"
    }

    object NameEntryView : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            NameEntryView()
        }
        override val route: String = "signup-flow/nameEntryView"
    }

    object Org : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            OrgPickerScreen()
        }
        override val route: String = "org"
    }

    object Dashboard : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            DashboardScreen()
        }
        override val route: String = "dashboard"
    }

    object UserSelect : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            UserSelectScreen()
        }
        override val route: String = "user-select"
    }

    object WalletSelect : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            WalletSelectScreen(getPlanterInfoId(it))
        }
        override val route: String = "wallet-select/{planterInfoId}"
        override val arguments = listOf(navArgument("planterInfoId") { type = NavType.LongType })

        private fun getPlanterInfoId(backStackEntry: NavBackStackEntry): Long {
            return backStackEntry.arguments?.getLong("planterInfoId") ?: -1
        }

        fun create(planterInfoId: Long) = "wallet-select/$planterInfoId"
    }

    object Selfie : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SelfieScreen()
        }
        override val route: String = "selfie"
    }

    object ImageReview : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            ImageReviewScreen(photoPath(it))
        }
        override val route: String = "camera-review/{photoPath}"
        override val arguments = listOf(navArgument("photoPath") { type = NavType.StringType })

        fun photoPath(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("photoPath").fromSafeNavUrl()
        }

        fun create(photoPath: String): String {
            return "camera-review/${photoPath.toSafeNavUrl()}"
        }
    }

    object Language : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            LanguageSelectScreen(isFromTopBar(it))
        }
        override val route: String = "language/{isFromTopBar}"
        override val arguments = listOf(navArgument("isFromTopBar") { type = NavType.BoolType })

        private fun isFromTopBar(backStackEntry: NavBackStackEntry): Boolean {
            return backStackEntry.arguments?.getBoolean("isFromTopBar") ?: false
        }

        fun create(isFromTopBar: Boolean = true) = "language/$isFromTopBar"
    }

    object TreeCapture : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            TreeCaptureScreen(getProfilePicUrl(it))
        }
        override val route: String = "capture/{profilePicUrl}"
        override val arguments = listOf(navArgument("profilePicUrl") { type = NavType.StringType })

        private fun getProfilePicUrl(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("profilePicUrl", "").fromSafeNavUrl()
        }

        fun create(profilePicUrl: String) = "capture/${profilePicUrl.toSafeNavUrl()}"
    }

    object TreeImageReview : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            TreeImageReviewScreen(photoPath(it))
        }
        override val route: String = "tree-image-review/{photoPath}"
        override val arguments = listOf(navArgument("photoPath") { type = NavType.StringType })

        fun photoPath(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("photoPath").fromSafeNavUrl()
        }

        fun create(photoPath: String): String {
            return "tree-image-review/${photoPath.toSafeNavUrl()}"
        }
    }

    object MessagesUserSelect : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            MessagesUserSelectScreen()
        }
        override val route: String = "messages-user-select"
    }
}

// Navigation uses '/' to denote params or nesting. Having a param that contains '/'
// will break the navigation. These replace '/' with '*' while navigating
private fun String?.toSafeNavUrl(): String = this?.replace('/', '*') ?: ""
private fun String?.fromSafeNavUrl(): String = this?.replace('*', '/') ?: ""
