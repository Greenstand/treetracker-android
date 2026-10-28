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

// No-arg routes

@Serializable data class SplashRoute(
    val orgId: String? = null,
    val orgName: String? = null,
) : NavKey

@Serializable data object SignupFlowRoute : NavKey

@Serializable data object DeleteProfileRoute : NavKey

@Serializable data object OrgRoute : NavKey

@Serializable data object DashboardRoute : NavKey

@Serializable data object TreeHeightScreenRoute : NavKey

@Serializable data object UserSelectRoute : NavKey

@Serializable data object ProfileSelectRoute : NavKey

@Serializable data object WalletSelectRoute : NavKey

@Serializable data object AddWalletRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Serializable data object SessionNoteRoute : NavKey

@Serializable data object AddOrgRoute : NavKey

@Serializable data object SelfieRoute : NavKey

@Serializable data object MessagesUserSelectRoute : NavKey

@Serializable data object DevOptionsRoute : NavKey

@Serializable data object MapRoute : NavKey

@Serializable data object TreeEditUserSelectRoute : NavKey

// Routes with arguments

@Serializable data class ProfileRoute(
    val planterInfoId: Long,
) : NavKey

@Serializable data class IndividualMessageListRoute(
    val planterInfoId: Long,
) : NavKey

@Serializable data class SurveyRoute(
    val messageId: String,
) : NavKey

@Serializable data class ImageReviewRoute(
    val photoPath: String,
) : NavKey

@Serializable data class LanguageRoute(
    val isFromTopBar: Boolean = true,
) : NavKey

@Serializable data class TreeCaptureRoute(
    val profilePicUrl: String,
) : NavKey

@Serializable data class TreeImageReviewRoute(
    val photoPath: String,
) : NavKey

@Serializable data class ChatRoute(
    val planterInfoId: Long,
    val otherChatIdentifier: String,
) : NavKey

@Serializable data class AnnouncementRoute(
    val messageId: String,
) : NavKey

@Serializable data class TreeListRoute(
    val userWallet: String,
    val userName: String,
) : NavKey

@Serializable data class TreeDetailRoute(
    val treeId: Long,
) : NavKey