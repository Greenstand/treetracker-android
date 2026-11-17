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
    /**
     * Navigate forward in the setup flow. Suspend because completing setup
     * requires starting a session (DB write) before navigating to capture.
     */
    suspend fun navForward(navigator: Navigator) {
        syncIndexWithBackStack(navigator)
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
            CaptureSetupScopeManager.close()
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
     * skipped, so the step underneath them counts.
     */
    private fun syncIndexWithBackStack(navigator: Navigator) {
        for (key in navigator.backStack.asReversed()) {
            val index = navPath.indexOfFirst { resolveNoArgDestination(it) == key }
            if (index >= 0) {
                currentNavPathIndex = index
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
        incrementIndex()
        if (currentNavPathIndex >= navPath.size) {
            // Signup was the last step — treat as setup complete
            currentNavPathIndex = navPath.size - 1
            navForward(navigator)
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