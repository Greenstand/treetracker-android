package org.greenstand.android.TreeTracker.navigation

import androidx.navigation.NavHostController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.NavRoute
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
        // If we're done navigating inside the setup flow, go the the capture flow
        if (currentNavPathIndex == navPath.size - 1) {
            GlobalScope.launch {
                stepCounter.enable()
                sessionTracker.startSession()
                locationDataCapturer.startGpsUpdates()
            }

                val userPhotoPath = CaptureSetupScopeManager.getData().user!!.photoPath
                navController.navigate(NavRoute.TreeCapture.create(userPhotoPath))
                CaptureSetupScopeManager.close()
        } else {
            currentNavPathIndex++
            navController.navigate(navPath[currentNavPathIndex].route)
        }
    }

    fun navBackward(navController: NavHostController) {
        currentNavPathIndex--
        navController.popBackStack()
    }

    fun navToUserSelect(navController: NavHostController) {
        currentNavPathIndex = 0
        navController.navigate(NavRoute.UserSelect.route) {
            popUpTo(NavRoute.Dashboard.route)
            launchSingleTop = true
        }
    }

    fun navFromNewUserCreation(navController: NavHostController) {
        currentNavPathIndex++
        navController.navigate(navPath[currentNavPathIndex].route) {
            popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
            launchSingleTop = true
        }
    }

}