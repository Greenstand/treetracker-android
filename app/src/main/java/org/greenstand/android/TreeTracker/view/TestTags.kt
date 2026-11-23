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
package org.greenstand.android.TreeTracker.view

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }

object TestTags {
    const val SPLASH = "SplashScreen"
    const val TOP_BAR_LOGO = "TopBarLogo"
    const val TOP_BAR_LANGUAGE_BUTTON = "TopBarLanguageButton"
    const val SNACKBAR = "Snackbar"

    const val DIALOG_TITLE = "DialogTitle"
    const val DIALOG_MESSAGE = "DialogMessage"
    const val DIALOG_TEXT_FIELD = "DialogTextField"
    const val DIALOG_POSITIVE_BUTTON = "DialogPositiveButton"
    const val DIALOG_NEGATIVE_BUTTON = "DialogNegativeButton"
    const val DIALOG_USER_BUTTON = "DialogUserButton"
    const val TUTORIAL_DONE_BUTTON = "TutorialDoneButton"
    const val PRIVACY_POLICY_CLOSE_BUTTON = "PrivacyPolicyCloseButton"
    const val CAMERA_RATIONALE_DIALOG = "CameraRationaleDialog"
    const val LOCATION_RATIONALE_DIALOG = "LocationRationaleDialog"
    const val PERMISSION_SETTINGS_DIALOG = "PermissionSettingsDialog"
    const val BAD_GPS_DIALOG = "BadGpsDialog"

    const val PHONE_CREDENTIAL_BUTTON = "PhoneCredentialButton"
    const val EMAIL_CREDENTIAL_BUTTON = "EmailCredentialButton"
    const val PHONE_FIELD = "PhoneField"
    const val EMAIL_FIELD = "EmailField"
    const val VIEW_WEB_MAP_LINK = "ViewWebMapLink"
    const val EXISTING_USER_BUTTON = "ExistingUserButton"
    const val FIRST_NAME_FIELD = "FirstNameField"
    const val LAST_NAME_FIELD = "LastNameField"
    const val FIRST_NAME_ERROR = "FirstNameError"
    const val LAST_NAME_ERROR = "LastNameError"

    const val SETTINGS_BUTTON = "SettingsButton"
    const val TREES_SYNCED_COUNT = "TreesSyncedCount"
    const val TREES_TO_SYNC_COUNT = "TreesToSyncCount"
    const val UPLOAD_BUTTON = "UploadButton"
    const val MESSAGES_BUTTON = "MessagesButton"
    const val TRACK_BUTTON = "TrackButton"

    const val ORG_NAME_FIELD = "OrgNameField"
    const val PREVIOUS_ORG_BUTTON = "PreviousOrgButton"
    const val SESSION_NOTE_FIELD = "SessionNoteField"
    const val WALLET_NAME_FIELD = "WalletNameField"
    const val CAPTURE_PROGRESS = "CaptureProgress"
    const val NOTE_BUTTON = "NoteButton"

    const val SETTINGS_PROFILE = "SettingsProfile"
    const val SETTINGS_EDIT_TREES = "SettingsEditTrees"
    const val SETTINGS_MAP = "SettingsMap"
    const val SETTINGS_PRIVACY = "SettingsPrivacy"
    const val SETTINGS_LOGOUT = "SettingsLogout"
    const val SETTINGS_DELETE_ACCOUNT = "SettingsDeleteAccount"
    const val APP_VERSION = "AppVersion"

    const val PROFILE_PHOTO = "ProfilePhoto"
    const val EDIT_PROFILE_BUTTON = "EditProfileButton"
    const val SAVE_PROFILE_BUTTON = "SaveProfileButton"
    const val PROFILE_FIRST_NAME = "ProfileFirstName"
    const val PROFILE_LAST_NAME = "ProfileLastName"
    const val PROFILE_WALLET = "ProfileWallet"

    const val TREE_LIST_EMPTY = "TreeListEmpty"
    const val TREE_UUID = "TreeUuid"
    const val TREE_LATITUDE = "TreeLatitude"
    const val TREE_LONGITUDE = "TreeLongitude"
    const val TREE_CREATED_AT = "TreeCreatedAt"
    const val TREE_UPLOADED = "TreeUploaded"
    const val TREE_NOTE_FIELD = "TreeNoteField"
    const val SAVE_NOTE_BUTTON = "SaveNoteButton"
    const val DELETE_TREE_BUTTON = "DeleteTreeButton"

    const val MAP_BACK_BUTTON = "MapBackButton"

    const val NO_MESSAGES = "NoMessages"
    const val CHAT_INPUT_FIELD = "ChatInputField"
    const val SURVEY_QUESTION = "SurveyQuestion"
    const val ANNOUNCEMENT_TITLE = "AnnouncementTitle"
    const val ANNOUNCEMENT_BODY = "AnnouncementBody"
    const val ANNOUNCEMENT_LINK = "AnnouncementLink"

    const val SYNC_OVERLAY_SWITCH = "SyncOverlaySwitch"
    const val SENSOR_OVERLAY_SWITCH = "SensorOverlaySwitch"
    const val DEBUG_OVERLAY_MENU_BUTTON = "DebugOverlayMenuButton"
    const val DEBUG_SYNC_OVERLAY_TOGGLE = "DebugSyncOverlayToggle"
    const val DEBUG_SENSOR_OVERLAY_TOGGLE = "DebugSensorOverlayToggle"
    const val SYNC_OVERLAY = "SyncOverlay"
    const val SYNC_OVERLAY_ERROR = "SyncOverlayError"
    const val SENSOR_OVERLAY = "SensorOverlay"
    const val ORG_OVERRIDES_SWITCH = "OrgOverridesSwitch"
    const val ORG_OVERRIDES_RESET = "OrgOverridesReset"
    const val ORG_OVERRIDE_WALLET = "OrgOverrideWallet"
    const val ORG_OVERRIDES_SETUP_FLOW = "OrgOverridesSetupFlow"
    const val ORG_OVERRIDES_CAPTURE_FLOW = "OrgOverridesCaptureFlow"

    fun languageOption(name: String) = "LanguageOption_$name"

    fun userButton(wallet: String) = "UserButton_$wallet"

    fun orgOption(name: String) = "OrgOption_$name"

    fun treeButton(index: Int) = "TreeButton_$index"

    fun mapTreeCard(index: Int) = "MapTreeCard_$index"

    fun messageItem(index: Int) = "MessageItem_$index"

    fun surveyAnswer(index: Int) = "SurveyAnswer_$index"

    fun devParam(name: String) = "DevParam_$name"

    fun orgOverride(id: String) = "OrgOverride_$id"

    fun orgOverrideReset(id: String) = "OrgOverrideReset_$id"
}