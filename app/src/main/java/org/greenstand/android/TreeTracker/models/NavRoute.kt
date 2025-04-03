/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.camera.ImageReviewScreen
import org.greenstand.android.TreeTracker.camera.SelfieScreen
import org.greenstand.android.TreeTracker.capture.TreeCaptureScreen
import org.greenstand.android.TreeTracker.capture.TreeImageReviewScreen
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.devoptions.DevOptionsRoot
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.messages.ChatScreen
import org.greenstand.android.TreeTracker.messages.MessagesUserSelectScreen
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementScreen
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListScreen
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreen
import org.greenstand.android.TreeTracker.orgpicker.AddOrgScreen
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.settings.SettingsScreen
import org.greenstand.android.TreeTracker.signup.SignUpScreen
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.treeheight.TreeHeightScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class NavRoute : KoinComponent {

    private val exceptionDataCollector: ExceptionDataCollector by inject()

    val contentProvider: @Composable (NavBackStackEntry) -> Unit = {
        exceptionDataCollector.setScreen(route)
        content(it)
    }
    abstract val content: @Composable (NavBackStackEntry) -> Unit
    abstract val route: String
    open val arguments: List<NamedNavArgument> = emptyList()
    open val deepLinks: List<NavDeepLink> = emptyList()

    object Splash : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SplashScreen(it.arguments?.getString("orgJson"))
        }
        override val route: String = "splash?params={orgJson}"

        override val deepLinks: List<NavDeepLink> = listOf(navDeepLink { uriPattern = "app://mobile.treetracker.org/org?params={orgJson}" })
    }

    object SignupFlow : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SignUpScreen()
        }
        override val route: String = "signup-flow"
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

    object TreeHeightScreen : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            TreeHeightScreen()
        }
        override val route: String = "tree-height-selection"
    }

    object UserSelect : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            UserSelectScreen()
        }
        override val route: String = "user-select"
    }

    object WalletSelect : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            WalletSelectScreen()
        }
        override val route: String = "wallet-select"

        fun create() = route
    }

    object AddWallet : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            AddWalletScreen()
        }
        override val route: String = "add-wallet"

        fun create() = route
    }

    object Settings : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SettingsScreen()
        }
        override val route: String = "settings"

        fun create() = route
    }

    object SessionNote : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SessionNoteScreen()
        }
        override val route: String = "session-note"

        fun create() = route
    }

    object AddOrg : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            AddOrgScreen()
        }
        override val route: String = "add-org"

        fun create() = route
    }

    object Selfie : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SelfieScreen()
        }
        override val route: String = "selfie"
    }

    object IndividualMessageList : NavRoute() {
        @OptIn(ExperimentalFoundationApi::class)
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            IndividualMessageListScreen(getPlanterInfoId(it))
        }
        override val route: String = "message-list/{planterInfoId}"
        override val arguments = listOf(navArgument("planterInfoId") { type = NavType.LongType })

        private fun getPlanterInfoId(backStackEntry: NavBackStackEntry): Long {
            return backStackEntry.arguments?.getLong("planterInfoId") ?: -1
        }

        fun create(planterInfoId: Long) = "message-list/$planterInfoId"
    }

    object Survey : NavRoute() {

        override val content: @Composable (NavBackStackEntry) -> Unit = {
            SurveyScreen(messageId(it))
        }

        override val route: String = "survey/{messageId}"

        override val arguments = listOf(navArgument("messageId") { type = NavType.StringType })

        fun messageId(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("messageId").fromSafeNavUrl()
        }

        fun create(messageId: String): String {
            return "survey/${messageId.toSafeNavUrl()}"
        }
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
        @OptIn(ExperimentalPermissionsApi::class)
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
            TreeImageReviewScreen()
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

    object Chat : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            ChatScreen(getPlanterInfoId(it), getOtherChatIdentifier(it))
        }
        override val route: String = "chat/{planterInfoId}/{otherChatIdentifier}"
        override val arguments = listOf(navArgument("planterInfoId") { type = NavType.LongType }, navArgument("otherChatIdentifier") { type = NavType.StringType })

        private fun getPlanterInfoId(backStackEntry: NavBackStackEntry): Long {
            return backStackEntry.arguments?.getLong("planterInfoId") ?: -1
        }
        private fun getOtherChatIdentifier(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("otherChatIdentifier") ?: ""
        }

        fun create(planterInfoId: Long, otherChatIdentifier: String) = "chat/$planterInfoId/$otherChatIdentifier"
    }

    object Announcement : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            AnnouncementScreen(messageId(it))
        }

        override val route: String = "announcement/{messageId}"

        override val arguments = listOf(navArgument("messageId") { type = NavType.StringType })

        fun messageId(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments?.getString("messageId").fromSafeNavUrl()
        }

        fun create(messageId: String): String {
            return "announcement/${messageId.toSafeNavUrl()}"
        }
    }

    object DevOptions : NavRoute() {
        override val content: @Composable (NavBackStackEntry) -> Unit = {
            DevOptionsRoot()
        }
        override val route: String = "dev"
    }
}

// Navigation uses '/' to denote params or nesting. Having a param that contains '/'
// will break the navigation. These replace '/' with '*' while navigating
private fun String?.toSafeNavUrl(): String = this?.replace('/', '*') ?: ""
private fun String?.fromSafeNavUrl(): String = this?.replace('*', '/') ?: ""