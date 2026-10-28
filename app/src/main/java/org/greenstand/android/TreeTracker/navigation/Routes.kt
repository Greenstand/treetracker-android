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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen's route. Routes live here, in one place every feature can see, so a feature can
 * open another feature's screen without depending on it. Each feature registers the screen for
 * its routes in its Koin module with `navigation<SomeRoute> { ... }`.
 */
sealed interface Route : NavKey

// No-arg routes

@Serializable data class SplashRoute(
    val orgId: String? = null,
    val orgName: String? = null,
) : Route

@Serializable data object SignupFlowRoute : Route {
    /**
     * When sign-up was opened from another screen (a user picker), it goes back to that screen
     * and hands it the new user's id under this key. The first user goes to the dashboard instead.
     */
    val NewUserId = NavResultKey<Long>("signup.newUserId")
}

@Serializable data object DeleteProfileRoute : Route

@Serializable data object OrgRoute : Route

@Serializable data object DashboardRoute : Route

@Serializable data object TreeHeightScreenRoute : Route

@Serializable data object UserSelectRoute : Route

@Serializable data object ProfileSelectRoute : Route

@Serializable data object WalletSelectRoute : Route

@Serializable data object AddWalletRoute : Route

@Serializable data object SettingsRoute : Route

@Serializable data object SessionNoteRoute : Route

@Serializable data object AddOrgRoute : Route

@Serializable data object SelfieRoute : Route

@Serializable data object MessagesUserSelectRoute : Route

@Serializable data object DevOptionsRoute : Route

@Serializable data object MapRoute : Route

@Serializable data object TreeEditUserSelectRoute : Route

// Routes with arguments

@Serializable data class ProfileRoute(
    val planterInfoId: Long,
) : Route

@Serializable data class IndividualMessageListRoute(
    val planterInfoId: Long,
) : Route

@Serializable data class SurveyRoute(
    val messageId: String,
) : Route

@Serializable data class ImageReviewRoute(
    val photoPath: String,
) : Route

@Serializable data class LanguageRoute(
    val isFromTopBar: Boolean = true,
) : Route

@Serializable data class TreeCaptureRoute(
    val profilePicUrl: String,
) : Route

@Serializable data class TreeImageReviewRoute(
    val photoPath: String,
) : Route

@Serializable data class ChatRoute(
    val planterInfoId: Long,
    val otherChatIdentifier: String,
) : Route

@Serializable data class AnnouncementRoute(
    val messageId: String,
) : Route

@Serializable data class TreeListRoute(
    val userWallet: String,
    val userName: String,
) : Route

@Serializable data class TreeDetailRoute(
    val treeId: Long,
) : Route