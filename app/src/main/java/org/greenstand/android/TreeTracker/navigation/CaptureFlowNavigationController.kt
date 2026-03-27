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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.OrgRepo

class CaptureFlowNavigationController(
    orgRepo: OrgRepo,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
    private val treeCapturer: TreeCapturer,
) {
    private var navPath = emptyList<Destination>()
    private var currentNavPathIndex = 0

    init {
        navPath = orgRepo.currentOrg().captureFlow
    }

    fun navForward(navController: NavHostController) {
        currentNavPathIndex++

        // If navigating from last screen, pop to the first
        if (currentNavPathIndex >= navPath.size) {
            currentNavPathIndex = 0
            GlobalScope.launch {
                treeCapturer.saveTree()
            }
            navController.popBackStack<TreeCaptureRoute>(inclusive = false)
            return
        }

        val destination = navPath[currentNavPathIndex]
        val route = resolveDestinationRoute(destination)
        navController.navigate(route)
    }

    fun navBackward(navController: NavHostController) {
        currentNavPathIndex--

        // If we navigate back while on first screen (TreeCaptureScreen) then go to Dashboard
        if (currentNavPathIndex < 0) {
            goToDashboard(navController)
            return
        }
        navController.popBackStack()
    }

    fun goToDashboard(navController: NavHostController) {
        endSession()
        navController.navigate(DashboardRoute) {
            popUpTo<DashboardRoute> { inclusive = true }
            launchSingleTop = true
        }
    }

    fun goToUserSelect(navController: NavHostController) {
        endSession()
        navController.navigate(UserSelectRoute) {
            popUpTo<DashboardRoute> { inclusive = true }
            launchSingleTop = true
        }
    }

    private fun resolveDestinationRoute(destination: Destination): Any {
        // Check if it's a no-arg route first
        RouteRegistry.resolveNoArgRoute(destination.route)?.let { return it }

        // Handle arg routes based on the pattern
        return when {
            destination.route.startsWith("tree-image-review") -> {
                TreeImageReviewRoute(photoPath = treeCapturer.currentTree!!.photoPath!!)
            }
            destination.route.startsWith("capture") -> {
                TreeCaptureRoute(profilePicUrl = "")
            }
            else -> error("Unknown capture flow destination: ${destination.route}")
        }
    }

    private fun endSession() {
        CaptureFlowScopeManager.close()
        GlobalScope.launch {
            sessionTracker.endSession()
            stepCounter.disable()
            withContext(Dispatchers.Main) {
                locationDataCapturer.stopGpsUpdates()
                locationDataCapturer.turnOffTreeCaptureMode()
            }
        }
    }
}
