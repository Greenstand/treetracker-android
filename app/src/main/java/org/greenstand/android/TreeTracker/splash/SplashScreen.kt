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
package org.greenstand.android.TreeTracker.splash

import android.Manifest
import android.os.Build
import android.util.TypedValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.SplashRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.AppColors
import timber.log.Timber
import org.greenstand.android.TreeTracker.utilities.throttledNavigate

@Composable
fun SplashScreen(
    orgId: String?,
    orgName: String? = null,
    viewModel: SplashScreenViewModel = viewModel(factory = SplashScreenViewModelFactory(orgId, orgName)),
    navController: NavHostController = LocalNavHostController.current,
) {
    val permissions =
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val scope = rememberCoroutineScope()

    val permissionRequester =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { result ->
                scope.launch {
                    if (isLocationPermissionGranted(result)) {
                        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}")

                        viewModel.bootstrap()

                        delay(1000)

                        if (viewModel.isInitialSetupRequired()) {
                            viewModel.handleAction(SplashAction.StartGPSUpdatesForSignup)
                            navigateToLanguageScreen(navController)
                        } else {
                            navigateToDashboardScreen(navController)
                        }
                    }
                }
            },
        )
    LaunchedEffect(true) {
        permissionRequester.launch(permissions.toTypedArray())
    }

    Splash()
}

@Composable
fun Splash() {
    val resources = LocalContext.current.resources
    val splashAvailable =
        remember {
            runCatching { resources.getValue(R.drawable.splash, TypedValue(), true) }
                .onFailure { Timber.e(it, "Splash drawable failed to resolve at runtime") }
                .isSuccess
        }

    if (splashAvailable) {
        Image(
            painter = painterResource(id = R.drawable.splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AppColors.Gray),
        )
    }
}

private fun isLocationPermissionGranted(result: Map<String, Boolean>): Boolean =
    result[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

private fun navigateToLanguageScreen(navController: NavHostController) {
    navController.throttledNavigate(LanguageRoute(isFromTopBar = false)) {
        popUpTo<SplashRoute> { inclusive = true }
        launchSingleTop = true
    }
}

private fun navigateToDashboardScreen(navController: NavHostController) {
    navController.throttledNavigate(DashboardRoute) {
        popUpTo<SplashRoute> { inclusive = true }
        launchSingleTop = true
    }
}