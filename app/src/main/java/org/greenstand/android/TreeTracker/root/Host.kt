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
package org.greenstand.android.TreeTracker.root

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.overlay.DebugOverlayHost
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.koin.compose.koinInject
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.greenstand.android.TreeTracker.camera.ImageReviewScreen
import org.greenstand.android.TreeTracker.camera.SelfieScreen
import org.greenstand.android.TreeTracker.capture.TreeCaptureScreen
import org.greenstand.android.TreeTracker.capture.TreeImageReviewScreen
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.devoptions.DevOptionsRoot
import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.map.MapScreen
import org.greenstand.android.TreeTracker.messages.ChatScreen
import org.greenstand.android.TreeTracker.messages.MessagesUserSelectScreen
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementScreen
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListScreen
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreen
import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.ChatRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DeleteProfileRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.navigation.ImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.OrgRoute
import org.greenstand.android.TreeTracker.navigation.ProfileRoute
import org.greenstand.android.TreeTracker.navigation.ProfileSelectRoute
import org.greenstand.android.TreeTracker.navigation.SelfieRoute
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.SplashRoute
import org.greenstand.android.TreeTracker.navigation.SurveyRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.TreeHeightScreenRoute
import org.greenstand.android.TreeTracker.navigation.TreeImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.navigation.trackedComposable
import org.greenstand.android.TreeTracker.orgpicker.AddOrgScreen
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.profile.DeleteProfileScreen
import org.greenstand.android.TreeTracker.profile.ProfileScreen
import org.greenstand.android.TreeTracker.profile.ProfileSelectScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.settings.SettingsScreen
import org.greenstand.android.TreeTracker.signup.SignUpScreen
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.treeheight.TreeHeightScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@ExperimentalComposeApi
@Composable
fun Host() {
    val navController = LocalNavHostController.current

    TreeTrackerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController, startDestination = SplashRoute) {
                trackedComposable<SplashRoute>(
                    deepLinks =
                        listOf(
                            navDeepLink { uriPattern = "app://mobile.treetracker.org/org?params={orgJson}" },
                        ),
                ) {
                    SplashScreen(it.arguments?.getString("orgJson"))
                }

                trackedComposable<LanguageRoute> {
                    val route = it.toRoute<LanguageRoute>()
                    LanguageSelectScreen(route.isFromTopBar)
                }

                trackedComposable<SignupFlowRoute> { SignUpScreen() }
                trackedComposable<DashboardRoute> { DashboardScreen() }
                trackedComposable<OrgRoute> { OrgPickerScreen() }
                trackedComposable<UserSelectRoute> { UserSelectScreen() }
                trackedComposable<WalletSelectRoute> { WalletSelectScreen() }
                trackedComposable<AddWalletRoute> { AddWalletScreen() }
                trackedComposable<AddOrgRoute> { AddOrgScreen() }
                trackedComposable<SelfieRoute> { SelfieScreen() }
                trackedComposable<TreeHeightScreenRoute> { TreeHeightScreen() }
                trackedComposable<SessionNoteRoute> { SessionNoteScreen() }
                trackedComposable<SettingsRoute> { SettingsScreen() }
                trackedComposable<ProfileSelectRoute> { ProfileSelectScreen() }
                trackedComposable<DeleteProfileRoute> { DeleteProfileScreen() }
                trackedComposable<MessagesUserSelectRoute> { MessagesUserSelectScreen() }
                trackedComposable<DevOptionsRoute> { DevOptionsRoot() }
                trackedComposable<MapRoute> { MapScreen() }

                trackedComposable<ProfileRoute> {
                    val route = it.toRoute<ProfileRoute>()
                    ProfileScreen(route.planterInfoId)
                }

                trackedComposable<IndividualMessageListRoute> {
                    val route = it.toRoute<IndividualMessageListRoute>()
                    IndividualMessageListScreen(route.planterInfoId)
                }

                trackedComposable<SurveyRoute> {
                    val route = it.toRoute<SurveyRoute>()
                    SurveyScreen(route.messageId)
                }

                trackedComposable<ImageReviewRoute> {
                    val route = it.toRoute<ImageReviewRoute>()
                    ImageReviewScreen(route.photoPath)
                }

                trackedComposable<TreeCaptureRoute> {
                    val route = it.toRoute<TreeCaptureRoute>()
                    TreeCaptureScreen(route.profilePicUrl)
                }

                trackedComposable<TreeImageReviewRoute> { TreeImageReviewScreen() }

                trackedComposable<ChatRoute> {
                    val route = it.toRoute<ChatRoute>()
                    ChatScreen(route.planterInfoId, route.otherChatIdentifier)
                }

                trackedComposable<AnnouncementRoute> {
                    val route = it.toRoute<AnnouncementRoute>()
                    AnnouncementScreen(route.messageId)
                }
            }

            if (FeatureFlags.DEBUG_ENABLED) {
                val overlayManager: DebugOverlayManager = koinInject()
                val syncProgressTracker: SyncProgressTracker = koinInject()
                val sensorDiagnosticsTracker: SensorDiagnosticsTracker = koinInject()

                DebugOverlayHost(
                    overlayManager = overlayManager,
                    syncProgressTracker = syncProgressTracker,
                    sensorDiagnosticsTracker = sensorDiagnosticsTracker,
                )
            }
        }
    }
}