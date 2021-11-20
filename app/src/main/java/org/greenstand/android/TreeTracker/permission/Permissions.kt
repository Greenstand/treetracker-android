package org.greenstand.android.TreeTracker.models

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.root.LocalNavHostController

@ExperimentalPermissionsApi
@Composable
fun PermissionRequest() {

    val navController = LocalNavHostController.current
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
                        //Check if location is enabled from the LocationUpdateManager before calling enableLocation
                        return
                    }
                    (perm.shouldShowRationale || !perm.permissionRequested) -> {
                        AlertDialog(
                            onDismissRequest = { navController.popBackStack() },
                            title = {
                                Text(text = stringResource(R.string.accept_camera_permission_header))
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
                            })
                    }

                }
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                when {
                    perm.hasPermission -> {
                        // TODO Check if location is enabled from the LocationUpdateManager before calling enableLocation
                        return
                    }
                    (perm.shouldShowRationale || !perm.permissionRequested) -> {
                        AlertDialog(
                            onDismissRequest = { navController.popBackStack() },
                            title = {
                                Text(text = stringResource(R.string.accept_location_permission_header)
                                )
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
                            })
                    }

                }
            }
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                when {
                    perm.hasPermission -> {
                        // TODO Check if location is enabled from the LocationUpdateManager before calling enableLocation
                        return
                    }
                    (perm.shouldShowRationale || !perm.permissionRequested) -> {
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
                            })
                    }
                }
            }
        }
    }
}

fun isLocationEnabledCheck() {

}

@Composable
fun enableLocation() {
    val activity: Context = LocalContext.current as Activity
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    startActivity(activity, intent, null)
}