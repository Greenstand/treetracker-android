package org.greenstand.android.TreeTracker.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
                leftAction = {
                    ArrowButton(isLeft = true,
                        onClick = { navController.navigate(NavRoute.Dashboard.route){
                            popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true

                        }},)
                },
                centerAction = {
                    DepthButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(height = 70.dp, width = 70.dp),
                        isEnabled = !state.isGettingLocation,
                        onClick = {
                            scope.launch {
                              viewModel.captureLocation()
                              cameraControl.captureImage()
                            }
                        },
                        shape = DepthSurfaceShape.Circle
                    ) {
                        ImageCaptureCircle(
                            modifier = Modifier
                                .size(72.dp, 72.dp),
                            color = AppColors.Green,
                            shadowColor = AppColors.Gray,
                        )
                    }
                }
            )
        }
    ) {
        Camera(
            isSelfieMode = false,
            cameraControl = cameraControl,
            onImageCaptured = {
                viewModel.onImageCaptured(it)
                navController.navigate(NavRoute.TreeImageReview.create(it.path))
            }
        )
        ActionBar(
            leftAction = {
                LocalImage(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                        .padding(15.dp, 10.dp, 10.dp, 10.dp)
                        .aspectRatio(1.0f)
                        .clip(RoundedCornerShape(percent = 10))
                        .clickable { navController.navigate(NavRoute.UserSelect.route) },
                    imagePath = state.profilePicUrl,
                    contentScale = ContentScale.Crop
                )
            },
            rightAction = {
                if (FeatureFlags.DEBUG_ENABLED) {
                    DepthButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(height = 70.dp, width = 70.dp),
                        isEnabled = !state.isCreatingFakeTrees,
                        onClick = {
                            scope.launch {
                                viewModel.createFakeTrees()
                            }
                        },
                        colors = AppButtonColors.UploadOrange,
                        shape = DepthSurfaceShape.Circle
                    ) { /* EMPTY */ }
                }
            }
        )
        showLoadingSpinner(state.isGettingLocation || state.isCreatingFakeTrees)
    }
}