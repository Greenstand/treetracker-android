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
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager

class CaptureSetupNavigationController(
    orgRepo: OrgRepo,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
) {

    private var navPath = emptyList<Destination>()
    private var currentNavPathIndex = 0

    init {
        navPath = orgRepo.currentOrg().captureSetupFlow
    }

    fun navForward(navController: NavHostController) {
        // If we're done navigating inside the setup flow, go to the capture flow
        if (currentNavPathIndex == navPath.size - 1) {
            runBlocking {
                stepCounter.enable()
                sessionTracker.startSession()
                locationDataCapturer.startGpsUpdates()
            }

            val userPhotoPath = CaptureSetupScopeManager.getData().user!!.photoPath
            navController.navigate(TreeCaptureRoute(profilePicUrl = userPhotoPath))
            CaptureSetupScopeManager.close()
        } else {
            currentNavPathIndex++
            val destination = navPath[currentNavPathIndex]
            val route = RouteRegistry.resolveNoArgRoute(destination.route)
                ?: error("Unknown setup flow destination: ${destination.route}")
            navController.navigate(route)
        }
    }

    fun navBackward(navController: NavHostController) {
        currentNavPathIndex--
        navController.popBackStack()
    }

    fun navToUserSelect(navController: NavHostController) {
        currentNavPathIndex = 0
        navController.navigate(UserSelectRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
    }

    fun navFromNewUserCreation(navController: NavHostController) {
        currentNavPathIndex++
        val destination = navPath[currentNavPathIndex]
        val route = RouteRegistry.resolveNoArgRoute(destination.route)
            ?: error("Unknown setup flow destination: ${destination.route}")
        navController.navigate(route) {
            popUpTo<SignupFlowRoute> { inclusive = true }
            launchSingleTop = true
        }
    }
}
