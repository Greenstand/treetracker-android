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

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.ChatRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DeleteProfileRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.navigation.FastFadeIn
import org.greenstand.android.TreeTracker.navigation.FastFadeOut
import org.greenstand.android.TreeTracker.navigation.ImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.Navigator
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
import org.greenstand.android.TreeTracker.navigation.TreeDetailRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.TreeHeightScreenRoute
import org.greenstand.android.TreeTracker.navigation.TreeImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.TreeListRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.navigation.rememberScreenTrackingNavEntryDecorator
import org.greenstand.android.TreeTracker.orgpicker.AddOrgScreen
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.overlay.DebugOverlayHost
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.profile.DeleteProfileScreen
import org.greenstand.android.TreeTracker.profile.ProfileScreen
import org.greenstand.android.TreeTracker.profile.ProfileSelectScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.settings.SettingsScreen
import org.greenstand.android.TreeTracker.signup.SignUpScreen
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.treeedit.TreeDetailScreen
import org.greenstand.android.TreeTracker.treeedit.TreeEditUserSelectScreen
import org.greenstand.android.TreeTracker.treeedit.TreeListScreen
import org.greenstand.android.TreeTracker.treeheight.TreeHeightScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@ExperimentalComposeApi
@Composable
fun Host(startRoute: SplashRoute = SplashRoute()) {
    // Persists across configuration changes and process death; on restore, the
    // saved stack wins over startRoute — same as Nav2's restored NavController state.
    val backStack = rememberNavBackStack(startRoute)
    val navigator = remember(backStack) { Navigator(backStack) }
    TreeTrackerTheme {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.popBackStack() },
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                            rememberScreenTrackingNavEntryDecorator(),
                        ),
                    transitionSpec = { FastFadeIn togetherWith FastFadeOut },
                    popTransitionSpec = { FastFadeIn togetherWith FastFadeOut },
                    predictivePopTransitionSpec = { FastFadeIn togetherWith FastFadeOut },
                    entryProvider =
                        entryProvider {
                            entry<SplashRoute> { route ->
                                SplashScreen(
                                    orgId = route.orgId,
                                    orgName = route.orgName,
                                )
                            }

                            entry<LanguageRoute> { route -> LanguageSelectScreen(route.isFromTopBar) }

                            entry<SignupFlowRoute> { SignUpScreen() }
                            entry<DashboardRoute> { DashboardScreen() }
                            entry<OrgRoute> { OrgPickerScreen() }
                            entry<UserSelectRoute> { UserSelectScreen() }
                            entry<WalletSelectRoute> { WalletSelectScreen() }
                            entry<AddWalletRoute> { AddWalletScreen() }
                            entry<AddOrgRoute> { AddOrgScreen() }
                            entry<SelfieRoute> { SelfieScreen() }
                            entry<TreeHeightScreenRoute> { TreeHeightScreen() }
                            entry<SessionNoteRoute> { SessionNoteScreen() }
                            entry<SettingsRoute> { SettingsScreen() }
                            entry<ProfileSelectRoute> { ProfileSelectScreen() }
                            entry<DeleteProfileRoute> { DeleteProfileScreen() }
                            entry<MessagesUserSelectRoute> { MessagesUserSelectScreen() }
                            entry<DevOptionsRoute> { DevOptionsRoot() }
                            entry<MapRoute> { MapScreen() }

                            entry<TreeEditUserSelectRoute> { TreeEditUserSelectScreen() }

                            entry<TreeListRoute> { route ->
                                TreeListScreen(userWallet = route.userWallet, userName = route.userName)
                            }

                            entry<TreeDetailRoute> { route -> TreeDetailScreen(treeId = route.treeId) }

                            entry<ProfileRoute> { route -> ProfileScreen(route.planterInfoId) }

                            entry<IndividualMessageListRoute> { route ->
                                IndividualMessageListScreen(route.planterInfoId)
                            }

                            entry<SurveyRoute> { route -> SurveyScreen(route.messageId) }

                            entry<ImageReviewRoute> { route -> ImageReviewScreen(route.photoPath) }

                            entry<TreeCaptureRoute> { route -> TreeCaptureScreen(route.profilePicUrl) }

                            entry<TreeImageReviewRoute> { TreeImageReviewScreen() }

                            entry<ChatRoute> { route ->
                                ChatScreen(route.planterInfoId, route.otherChatIdentifier)
                            }

                            entry<AnnouncementRoute> { route -> AnnouncementScreen(route.messageId) }
                        },
                )

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
}