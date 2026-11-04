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

/**
 * Lets tools that read the accessibility tree (Test Buddy `tb`, Maestro, UiAutomator) see
 * `testTag`s as resource ids, so end-to-end flows can find elements by id instead of by text
 * that changes with the language.
 *
 * Compose applies it to the node it is set on and everything under it, so it is set once on each
 * composition root: each activity's content and each dialog, which is a window of its own.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }

/**
 * Test tags for end-to-end flows. Each is unique on its screen, including dialogs shown over it,
 * since a tool that finds two elements with one id can tell neither apart. Repeated items (users,
 * languages, trees, messages) get a tag per item. Older tags live next to their components
 * ([ARROW_FORWARD_BUTTON_TAG], [CAPTURE_BUTTON_TAG], [APPROVE_BUTTON_TAG], ...).
 */
object TestTags {
    // Shared
    const val SPLASH = "SplashScreen"
    const val TOP_BAR_LOGO = "TopBarLogo"
    const val TOP_BAR_LANGUAGE_BUTTON = "TopBarLanguageButton"
    const val SNACKBAR = "Snackbar"

    // Dialogs (CustomDialog, tutorials, privacy policy)
    const val DIALOG_TITLE = "DialogTitle"
    const val DIALOG_MESSAGE = "DialogMessage"
    const val DIALOG_TEXT_FIELD = "DialogTextField"
    const val DIALOG_POSITIVE_BUTTON = "DialogPositiveButton"
    const val DIALOG_NEGATIVE_BUTTON = "DialogNegativeButton"
    const val DIALOG_USER_BUTTON = "DialogUserButton"
    const val TUTORIAL_DONE_BUTTON = "TutorialDoneButton"
    const val PRIVACY_POLICY_CLOSE_BUTTON = "PrivacyPolicyCloseButton"

    // Signup
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

    // Dashboard
    const val SETTINGS_BUTTON = "SettingsButton"
    const val TREES_SYNCED_COUNT = "TreesSyncedCount"
    const val TREES_TO_SYNC_COUNT = "TreesToSyncCount"
    const val UPLOAD_BUTTON = "UploadButton"
    const val MESSAGES_BUTTON = "MessagesButton"
    const val TRACK_BUTTON = "TrackButton"

    // Capture setup and capture
    const val ORG_NAME_FIELD = "OrgNameField"
    const val PREVIOUS_ORG_BUTTON = "PreviousOrgButton"
    const val SESSION_NOTE_FIELD = "SessionNoteField"
    const val WALLET_NAME_FIELD = "WalletNameField"
    const val CAPTURE_PROGRESS = "CaptureProgress"
    const val NOTE_BUTTON = "NoteButton"

    // Settings
    const val SETTINGS_PROFILE = "SettingsProfile"
    const val SETTINGS_EDIT_TREES = "SettingsEditTrees"
    const val SETTINGS_MAP = "SettingsMap"
    const val SETTINGS_PRIVACY = "SettingsPrivacy"
    const val SETTINGS_LOGOUT = "SettingsLogout"
    const val SETTINGS_DELETE_ACCOUNT = "SettingsDeleteAccount"
    const val APP_VERSION = "AppVersion"

    // Profile
    const val PROFILE_PHOTO = "ProfilePhoto"
    const val EDIT_PROFILE_BUTTON = "EditProfileButton"
    const val SAVE_PROFILE_BUTTON = "SaveProfileButton"
    const val PROFILE_FIRST_NAME = "ProfileFirstName"
    const val PROFILE_LAST_NAME = "ProfileLastName"
    const val PROFILE_WALLET = "ProfileWallet"

    // Tree edit
    const val TREE_LIST_EMPTY = "TreeListEmpty"
    const val TREE_UUID = "TreeUuid"
    const val TREE_LATITUDE = "TreeLatitude"
    const val TREE_LONGITUDE = "TreeLongitude"
    const val TREE_CREATED_AT = "TreeCreatedAt"
    const val TREE_UPLOADED = "TreeUploaded"
    const val TREE_NOTE_FIELD = "TreeNoteField"
    const val SAVE_NOTE_BUTTON = "SaveNoteButton"
    const val DELETE_TREE_BUTTON = "DeleteTreeButton"

    // Map
    const val MAP_BACK_BUTTON = "MapBackButton"

    // Messages
    const val NO_MESSAGES = "NoMessages"
    const val CHAT_INPUT_FIELD = "ChatInputField"
    const val SURVEY_QUESTION = "SurveyQuestion"
    const val ANNOUNCEMENT_TITLE = "AnnouncementTitle"
    const val ANNOUNCEMENT_BODY = "AnnouncementBody"
    const val ANNOUNCEMENT_LINK = "AnnouncementLink"

    // Dev options
    const val SYNC_OVERLAY_SWITCH = "SyncOverlaySwitch"
    const val SENSOR_OVERLAY_SWITCH = "SensorOverlaySwitch"

    /** A language on the language picker, by its enum name: `LanguageOption_ENGLISH`. */
    fun languageOption(name: String) = "LanguageOption_$name"

    /** A user (planter) card, by wallet (the phone or email they signed up with). */
    fun userButton(wallet: String) = "UserButton_$wallet"

    /** An org on the org picker, by name. */
    fun orgOption(name: String) = "OrgOption_$name"

    /** A tree card on the tree list, by position (0 is the first). */
    fun treeButton(index: Int) = "TreeButton_$index"

    /** A tree card on the map's carousel, by position (0 is the first). */
    fun mapTreeCard(index: Int) = "MapTreeCard_$index"

    /** A message card on a user's message list, by position (0 is the first). */
    fun messageItem(index: Int) = "MessageItem_$index"

    /** An answer on a survey question, by position (0 is the first). */
    fun surveyAnswer(index: Int) = "SurveyAnswer_$index"

    /** A dev options parameter, by its config name. */
    fun devParam(name: String) = "DevParam_$name"
}