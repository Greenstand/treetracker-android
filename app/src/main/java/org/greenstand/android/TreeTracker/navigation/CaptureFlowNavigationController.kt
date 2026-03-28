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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import timber.log.Timber

class CaptureFlowNavigationController(
    orgRepo: OrgRepo,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
    private val treeCapturer: TreeCapturer,
) : FlowNavigationController(orgRepo.currentOrg().captureFlow) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Navigate forward in the capture flow. Suspend because at the end of the flow
     * it must save the tree to the database before looping back to capture.
     */
    suspend fun navForward(navController: NavHostController) {
        incrementIndex()

        // If navigating past last screen, save tree then loop back to capture
        if (currentNavPathIndex >= navPath.size) {
            resetIndex()
            val saved = treeCapturer.saveTree()
            if (!saved) {
                Timber.tag("CaptureFlowNav").w("Tree save failed or no tree to save")
            }
            withContext(Dispatchers.Main) {
                navController.popBackStack<TreeCaptureRoute>(inclusive = false)
            }
            return
        }

        val destination = navPath[currentNavPathIndex]
        val route = resolveDestinationRoute(destination)
        withContext(Dispatchers.Main) {
            navController.navigate(route)
        }
    }

    fun navBackward(navController: NavHostController) {
        if (!decrementIndex()) {
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

    private fun resolveDestinationRoute(destination: org.greenstand.android.TreeTracker.models.organization.Destination): Any {
        resolveNoArgDestination(destination)?.let { return it }

        val normalized = RouteRegistry.normalize(destination.route)
        return when {
            normalized.startsWith("tree-image-review") -> {
                TreeImageReviewRoute(photoPath = treeCapturer.currentTree?.photoPath ?: "")
            }
            normalized.startsWith("capture") -> {
                TreeCaptureRoute(profilePicUrl = "")
            }
            else -> error("Unknown capture flow destination: ${destination.route}")
        }
    }

    private fun endSession() {
        CaptureFlowScopeManager.close()
        scope.launch {
            try {
                sessionTracker.endSession()
                stepCounter.disable()
                withContext(Dispatchers.Main) {
                    locationDataCapturer.stopGpsUpdates()
                    locationDataCapturer.turnOffTreeCaptureMode()
                }
            } catch (e: Exception) {
                Timber.tag("CaptureFlowNav").e(e, "Error ending session")
            }
        }
    }
}
