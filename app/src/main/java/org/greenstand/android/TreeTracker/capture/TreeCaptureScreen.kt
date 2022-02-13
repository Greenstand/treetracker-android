package org.greenstand.android.TreeTracker.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.greenstand.android.TreeTracker.R
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.camera.Camera
import org.greenstand.android.TreeTracker.camera.CameraControl
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.PermissionRequest
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.*

@ExperimentalPermissionsApi
@Composable
fun TreeCaptureScreen(
    profilePicUrl: String,
) {
    val viewModel: TreeCaptureViewModel = viewModel(factory = TreeCaptureViewModelFactory(profilePicUrl))
    val state by viewModel.state.observeAsState(TreeCaptureState(profilePicUrl))
    val navController = LocalNavHostController.current
    val cameraControl = remember { CameraControl() }
    val scope = rememberCoroutineScope()

    PermissionRequest()

    Scaffold(
        bottomBar = {
            ActionBar(
                modifier = Modifier.background(color = AppColors.Gray),
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        onClick = {
                            navController.navigate(NavRoute.Dashboard.route) {
                                popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                },
                centerAction = {
                    CaptureButton(
                        modifier =  Modifier
                            .align(Alignment.Center)
                        ,
                        onClick = {
                            scope.launch {
                                viewModel.captureLocation()
                                cameraControl.captureImage()
                            }
                        },
                        isEnabled = !state.isGettingLocation,
                    )
                }
            )
        }
    ) {
        if (state.isLocationAvailable == false) {
            CustomDialog(
                dialogIcon = painterResource(id = R.drawable.error_outline),
                title = stringResource(R.string.poor_gps_header),
                textContent = stringResource(R.string.poor_gps_message),
                onPositiveClick = { navController.popBackStack() },
                onNegativeClick = {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        Camera(
            isSelfieMode = false,
            cameraControl = cameraControl,
            onImageCaptured = {
                viewModel.onImageCaptured(it)
                if (state.isLocationAvailable == true) {
                    navController.navigate(NavRoute.TreeImageReview.create(it.path))
                }
            }
        )
        ActionBar(
            leftAction = {
                UserImageButton(
                    onClick = {
                        navController.navigate(NavRoute.UserSelect.route) {
                            popUpTo(NavRoute.Dashboard.route)
                            launchSingleTop = true
                        }
                    },
                    imagePath = state.profilePicUrl
                )
            },
            rightAction = {
                if (FeatureFlags.DEBUG_ENABLED) {
                    DepthButton(
                        modifier = Modifier
                            .size(height = 70.dp, width = 70.dp)
                            .align(Alignment.Center),
                        isEnabled = !state.isCreatingFakeTrees,
                        onClick = {
                            scope.launch {
                                viewModel.createFakeTrees()
                            }
                        },
                        colors = AppButtonColors.ProgressGreen,
                        shape = DepthSurfaceShape.Circle
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.yellow_leafs_placeholder),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(30.dp, 30.dp),
                            colorFilter = ColorFilter.tint(Color.Black)
                        )
                    }
                }
            }
        )
        showLoadingSpinner(state.isGettingLocation || state.isCreatingFakeTrees)
    }
}