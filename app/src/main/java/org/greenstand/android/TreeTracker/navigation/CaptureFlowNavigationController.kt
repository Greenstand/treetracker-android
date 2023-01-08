package org.greenstand.android.TreeTracker.navigation

import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.OrgRepo

class CaptureFlowNavigationController(
    orgRepo: OrgRepo,
    private val stepCounter: StepCounter,
    private val sessionTracker: SessionTracker,
    private val locationDataCapturer: LocationDataCapturer,
) {
    private var navPath = emptyList<Destination>()
    private var currentNavPathIndex = 0

    init {
        navPath = orgRepo.currentOrg().captureFlow
    }

    fun navForward(navController: NavHostController) {
        currentNavPathIndex++

        // If navigating from last screen, pop to the first
        if(currentNavPathIndex >= navPath.size) {
            currentNavPathIndex = 0
            navController.popBackStack(NavRoute.TreeCapture.route, false)
            return
        }

        navController.navigate(navPath[currentNavPathIndex].route)
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
        navController.navigate(NavRoute.Dashboard.route) {
            popUpTo(NavRoute.Dashboard.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun goToUserSelect(navController: NavHostController) {
        endSession()
        navController.navigate(NavRoute.UserSelect.route) {
            popUpTo(NavRoute.Dashboard.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    private fun endSession() {
        CaptureFlowScopeManager.close()
        GlobalScope.launch {
            sessionTracker.endSession()
            stepCounter.disable()
            withContext(Dispatchers.Main) {
                locationDataCapturer.stopGpsUpdates()
            }
        }
    }

}