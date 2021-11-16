package org.greenstand.android.TreeTracker.models
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import android.location.*
import android.provider.Settings
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat.startActivity



@ExperimentalPermissionsApi
@Composable
fun PermissionRequest(){

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
                    if(event == Lifecycle.Event.ON_START) {
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
                when(perm.permission) {
                    Manifest.permission.CAMERA -> {
                        when {
                            perm.hasPermission -> {
                                Text(text = "Camera permission accepted")

                            }
                            perm.shouldShowRationale -> {
                                Text(text = "Camera permission is needed" +
                                        "to access the camera")
                            }
                            perm.hasBeenDeniedForever() -> {
                                Text(text = "Camera permission was permanently" +
                                        "denied. You can enable it in the app" +
                                        "settings.")
                            }
                        }
                    }
                    Manifest.permission.ACCESS_COARSE_LOCATION -> {
                        when {
                            perm.hasPermission  -> {
                                    //call function to check if location is enabled
                                    enableLocation()
                            }
                            (perm.shouldShowRationale || !perm.permissionRequested) -> {
                                AlertDialog(
                                    onDismissRequest = {},
                                    title = {
                                           Text(text = "Accept Location permission")
                                    },
                                    text = {
                                           "Location is needed for this app to allow it track trees, accept this permission to use the tree tracking feature"
                                    },
                                    buttons = {
                                        Button(
                                            modifier = Modifier.wrapContentSize(),
                                            onClick = {  perm.launchPermissionRequest()}
                                        ) {
                                            Text(
                                                text = "Accept permission"
                                            )
                                        }
                                    })}
                            perm.hasBeenDeniedForever() -> {
                                Text(text = "Record audio permission was permanently" +
                                        "denied. You can enable it in the app" +
                                        "settings.")
                            }
                        }
                    }
                }
            }



}
@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.hasBeenDeniedForever(): Boolean {
    return this.permissionRequested && !this.shouldShowRationale
}

fun isLocationEnabledCheck(){

}
@Composable
fun enableLocation(){
    val activity: Context = LocalContext.current as Activity
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    startActivity( activity,intent,null)

}
