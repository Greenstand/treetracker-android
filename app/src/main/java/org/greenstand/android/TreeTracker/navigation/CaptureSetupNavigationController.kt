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

    /**
     * Navigate forward in the setup flow. Suspend because completing setup
     * requires starting a session (DB write) before navigating to capture.
     *
     * A repeat call from the same screen (e.g. a double tap on Next) is dropped: either
     * while the first call is still running, or once its screen is no longer on top.
     * Without this the second call would see the index already advanced and could
     * start a session and open capture, skipping the next setup step.
     */
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

    /**
     * One step forward from [currentNavPathIndex]. Callers sync the index first; it isn't synced
     * here because [navFromNewUserCreation] sets the index past signup before calling this.
     */
    private suspend fun advance(navigator: Navigator) {
        if (isAtEnd) {
            // Setup complete — start session and transition to capture flow
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
            // The setup scope stays open: the setup screens are still on the back stack, and
            // capture can pop back onto them (e.g. cancelling the camera rationale). The capture
            // flow closes it when it leaves for the dashboard or user select.
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

    /**
     * System back pops the stack through NavDisplay's onBack without calling [navBackward], so
     * the step index can be ahead of the screen the user is on. Re-derive it from the topmost
     * setup step on the back stack. Screens outside the setup flow (signup, add wallet) are
     * skipped, so the step underneath them counts. If a route appears more than once in the
     * flow, the occurrence at or before the current index wins, so a repeated step doesn't send
     * the user back to its first occurrence.
     */
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

    /**
     * Navigate forward after a new user was created from the signup flow.
     * Includes bounds checking to prevent IndexOutOfBoundsException.
     */
    suspend fun navFromNewUserCreation(navigator: Navigator) {
        syncIndexWithBackStack(navigator)
        incrementIndex()
        if (currentNavPathIndex >= navPath.size) {
            // Signup was the last step — treat as setup complete
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