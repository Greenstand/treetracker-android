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
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.navigation.FastFadeIn
import org.greenstand.android.TreeTracker.navigation.FastFadeOut
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.Navigator
import org.greenstand.android.TreeTracker.navigation.SplashRoute
import org.greenstand.android.TreeTracker.navigation.rememberScreenTrackingNavEntryDecorator
import org.greenstand.android.TreeTracker.overlay.DebugOverlayHost
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(ExperimentalFoundationApi::class, KoinExperimentalAPI::class)
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
                    // Each feature registers the screens for its routes in its Koin module
                    // (see di/FeatureModules.kt).
                    entryProvider = koinEntryProvider(),
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