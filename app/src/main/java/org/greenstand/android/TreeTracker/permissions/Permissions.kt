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
package org.greenstand.android.TreeTracker.models

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.permissions.PermissionItemsState
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@ExperimentalPermissionsApi
@Composable
fun PermissionRequest(
    viewModel: PermissionViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(PermissionItemsState())
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
        )
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    permissionsState.permissions.forEach { perm ->
        when (perm.permission) {
            Manifest.permission.CAMERA -> {
                when {
                    perm.hasPermission -> {}
                    perm.shouldShowRationale -> {
                        CustomDialog(
                            title = stringResource(R.string.accept_camera_permission_header),
                            textContent = stringResource(R.string.accept_camera_permission_message),
                            onNegativeClick = {
                                navController.popBackStack()
                            },
                            onPositiveClick = {
                                perm.launchPermissionRequest()
                                navController.popBackStack()
                            }
                        )
                    }
                    else -> {
                        PermissionDeniedPermanentlyDialog(navController)
                    }
                }
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                when {
                    perm.hasPermission -> {
                        viewModel.isLocationEnabled()
                        if (state.isLocationEnabled == false) {
                            enableLocation()
                        }
                    }
                    perm.shouldShowRationale -> {
                        LocationRationaleDialog(navController = navController, perm = perm)
                    }
                    else -> {
                        PermissionDeniedPermanentlyDialog(navController)
                    }
                }
            }
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                when {
                    perm.hasPermission -> {
                        viewModel.isLocationEnabled()
                        if (state.isLocationEnabled == false) {
                            enableLocation()
                        }
                    }
                    perm.shouldShowRationale -> {
                        LocationRationaleDialog(navController = navController, perm = perm)
                    }
                    else -> {
                        PermissionDeniedPermanentlyDialog(navController)
                    }
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
fun LocationRationaleDialog(navController: NavHostController, perm: PermissionState) {
    CustomDialog(
        title = stringResource(R.string.accept_location_permission_header),
        textContent = stringResource(R.string.accept_location_permission_message),
        onNegativeClick = {
            navController.popBackStack()
        },
        onPositiveClick = {
            perm.launchPermissionRequest()
            navController.popBackStack()
        }
    )
}

@Composable
fun enableLocation() {
    val activity: Context = LocalContext.current as Activity
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    startActivity(activity, intent, null)
}

@Composable
fun PermissionDeniedPermanentlyDialog(navController: NavHostController) {
    val activity: Context = LocalContext.current as Activity

    CustomDialog(
        title = stringResource(R.string.open_settings_header),
        textContent = stringResource(R.string.open_settings_message),
        onNegativeClick = {
            navController.popBackStack()
        },
        onPositiveClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", "org.greenstand.android.TreeTracker.debug", null)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(activity, intent, null)
        }
    )
}