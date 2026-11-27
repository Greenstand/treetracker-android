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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager

class CaptureSetupNavigationController(
    orgRepo: OrgRepo,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
    private val deviceOrientation: DeviceOrientation,
) : FlowNavigationController(orgRepo.currentOrg().captureSetupFlow) {
    private var navForwardInProgress = false

    suspend fun navForward(navigator: Navigator) {
        if (navForwardInProgress || !navigator.canNavigate) return
        navForwardInProgress = true
        try {
            syncIndexWithBackStack(navigator)
            advance(navigator)
        } finally {
            navForwardInProgress = false
        }
    }

    private suspend fun advance(navigator: Navigator) {
        if (isAtEnd) {
            stepCounter.enable()
            deviceOrientation.enable()
            sessionTracker.startSession()
            withContext(Dispatchers.Main) {
                locationDataCapturer.startGpsUpdates()
            }

            val userPhotoPath = CaptureSetupScopeManager.getData().user?.photoPath ?: ""
            withContext(Dispatchers.Main) {
                navigator.throttledNavigate(TreeCaptureRoute(profilePicUrl = userPhotoPath))
            }
        } else {
            incrementIndex()
            val destination = navPath[currentNavPathIndex]
            val route =
                resolveNoArgDestination(destination)
                    ?: error("Unknown setup flow destination: ${destination.route}")
            withContext(Dispatchers.Main) {
                navigator.throttledNavigate(route)
            }
        }
    }

    private fun syncIndexWithBackStack(navigator: Navigator) {
        for (key in navigator.backStack.asReversed()) {
            val matches = navPath.indices.filter { resolveNoArgDestination(navPath[it]) == key }
            if (matches.isNotEmpty()) {
                currentNavPathIndex = matches.lastOrNull { it <= currentNavPathIndex } ?: matches.first()
                return
            }
        }
    }

    fun navBackward(navigator: Navigator) {
        decrementIndex()
        navigator.throttledPopBackStack()
    }

    fun navToUserSelect(navigator: Navigator) {
        resetIndex()
        navigator.throttledNavigate(UserSelectRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
    }

    suspend fun navFromNewUserCreation(navigator: Navigator) {
        syncIndexWithBackStack(navigator)
        incrementIndex()
        if (currentNavPathIndex >= navPath.size) {
            currentNavPathIndex = navPath.size - 1
            advance(navigator)
            return
        }
        val destination = navPath[currentNavPathIndex]
        val route =
            resolveNoArgDestination(destination)
                ?: error("Unknown setup flow destination: ${destination.route}")
        withContext(Dispatchers.Main) {
            navigator.throttledNavigate(route) {
                popUpTo<SignupFlowRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}