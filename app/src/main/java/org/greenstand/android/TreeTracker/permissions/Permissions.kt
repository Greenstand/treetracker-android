package org.greenstand.android.TreeTracker.models

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.provider.Settings
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.permissions.PermissionItemsState
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory

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
            Manifest.permission.ACCESS_COARSE_LOCATION,
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
                    perm.hasPermission -> {
                        return
                    }
                    perm.shouldShowRationale -> {
                        AlertDialog(
                            onDismissRequest = { navController.popBackStack() },
                            title = {
                                Text(text = stringResource(R.string.accept_camera_permission_header))
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.accept_camera_permission_message),
                                    color = Color.Green
                                )
                            },
                            buttons = {
                                Button(
                                    modifier = Modifier.wrapContentSize(),
                                    onClick = {
                                        perm.launchPermissionRequest()
                                        navController.popBackStack()
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.accept_permission)
                                    )
                                }
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
                        if(state.isLocationEnabled == false){
                            enableLocation()
                        }
                        return
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
                        if(state.isLocationEnabled == false){
                            enableLocation()
                        }
                        return
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
    AlertDialog(
        onDismissRequest = { navController.popBackStack() },
        title = {
            Text(text = stringResource(R.string.accept_location_permission_header))
        },
        text = {
            Text(
                text = stringResource(R.string.accept_location_permission_message),
                color = Color.Green
            )
        },
        buttons = {
            Button(
                modifier = Modifier.wrapContentSize(),
                onClick = {
                    perm.launchPermissionRequest()
                    navController.popBackStack()
                }
            ) {
                Text(
                    text = stringResource(R.string.accept_permission)
                )
            }
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

    AlertDialog(
        onDismissRequest = { navController.popBackStack() },
        title = {
            Text(text = stringResource(R.string.open_settings_header))
        },
        text = {
            Text(
                text = stringResource(R.string.open_settings_message),
                color = Color.Green
            )
        },
        buttons = {
            Button(
                modifier = Modifier.wrapContentSize(),
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", "org.greenstand.android.TreeTracker.debug", null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(activity, intent, null)
                }
            ) {
                Text(
                    text = stringResource(R.string.open_settings)
                )
            }
        }
    )
}