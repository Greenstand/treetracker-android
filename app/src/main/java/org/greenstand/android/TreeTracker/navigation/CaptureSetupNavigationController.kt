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

import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
) : FlowNavigationController(orgRepo.currentOrg().captureSetupFlow) {

    /**
     * Navigate forward in the setup flow. Suspend because completing setup
     * requires starting a session (DB write) before navigating to capture.
     */
    suspend fun navForward(navController: NavHostController) {
        if (isAtEnd) {
            // Setup complete — start session and transition to capture flow
            stepCounter.enable()
            sessionTracker.startSession()
            withContext(Dispatchers.Main) {
                locationDataCapturer.startGpsUpdates()
            }

            val userPhotoPath = CaptureSetupScopeManager.getData().user?.photoPath ?: ""
            withContext(Dispatchers.Main) {
                navController.navigate(TreeCaptureRoute(profilePicUrl = userPhotoPath))
            }
            CaptureSetupScopeManager.close()
        } else {
            incrementIndex()
            val destination = navPath[currentNavPathIndex]
            val route = resolveNoArgDestination(destination)
                ?: error("Unknown setup flow destination: ${destination.route}")
            withContext(Dispatchers.Main) {
                navController.navigate(route)
            }
        }
    }

    fun navBackward(navController: NavHostController) {
        decrementIndex()
        navController.popBackStack()
    }

    fun navToUserSelect(navController: NavHostController) {
        resetIndex()
        navController.navigate(UserSelectRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
    }

    /**
     * Navigate forward after a new user was created from the signup flow.
     * Includes bounds checking to prevent IndexOutOfBoundsException.
     */
    suspend fun navFromNewUserCreation(navController: NavHostController) {
        incrementIndex()
        if (currentNavPathIndex >= navPath.size) {
            // Signup was the last step — treat as setup complete
            currentNavPathIndex = navPath.size - 1
            navForward(navController)
            return
        }
        val destination = navPath[currentNavPathIndex]
        val route = resolveNoArgDestination(destination)
            ?: error("Unknown setup flow destination: ${destination.route}")
        withContext(Dispatchers.Main) {
            navController.navigate(route) {
                popUpTo<SignupFlowRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
