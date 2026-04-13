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

import kotlinx.serialization.Serializable

// No-arg routes

@Serializable data class SplashRoute(
    val orgId: String? = null,
    val orgName: String? = null,
)

@Serializable data object SignupFlowRoute

@Serializable data object DeleteProfileRoute

@Serializable data object OrgRoute

@Serializable data object DashboardRoute

@Serializable data object TreeHeightScreenRoute

@Serializable data object UserSelectRoute

@Serializable data object ProfileSelectRoute

@Serializable data object WalletSelectRoute

@Serializable data object AddWalletRoute

@Serializable data object SettingsRoute

@Serializable data object SessionNoteRoute

@Serializable data object AddOrgRoute

@Serializable data object SelfieRoute

@Serializable data object MessagesUserSelectRoute

@Serializable data object DevOptionsRoute

@Serializable data object MapRoute

@Serializable data object TreeEditUserSelectRoute

// Routes with arguments

@Serializable data class ProfileRoute(
    val planterInfoId: Long,
)

@Serializable data class IndividualMessageListRoute(
    val planterInfoId: Long,
)

@Serializable data class SurveyRoute(
    val messageId: String,
)

@Serializable data class ImageReviewRoute(
    val photoPath: String,
)

@Serializable data class LanguageRoute(
    val isFromTopBar: Boolean = true,
)

@Serializable data class TreeCaptureRoute(
    val profilePicUrl: String,
)

@Serializable data class TreeImageReviewRoute(
    val photoPath: String,
)

@Serializable data class ChatRoute(
    val planterInfoId: Long,
    val otherChatIdentifier: String,
)

@Serializable data class AnnouncementRoute(
    val messageId: String,
)

@Serializable data class TreeListRoute(
    val userWallet: String,
    val userName: String,
)

@Serializable data class TreeDetailRoute(
    val treeId: Long,
)