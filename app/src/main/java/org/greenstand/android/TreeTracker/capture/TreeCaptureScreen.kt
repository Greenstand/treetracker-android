package org.greenstand.android.TreeTracker.capture

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.camera.Camera
import org.greenstand.android.TreeTracker.camera.CameraControl
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LocalImage

@Composable
fun TreeCaptureScreen(
    profilePicUrl: String,
) {
    val viewModel: TreeCaptureViewModel = viewModel(factory = TreeCaptureViewModelFactory(profilePicUrl))
    val state by viewModel.state.observeAsState(TreeCaptureState(profilePicUrl))
    val navController = LocalNavHostController.current
    val cameraControl = remember { CameraControl() }

    Scaffold(
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                    ) {

                    }
                }
            )
        }
    ) {
        Camera(
            isSelfieMode = false,
            cameraControl = cameraControl,
            onImageCaptured = {

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
                        .clip(RoundedCornerShape(percent = 10)),
                    imagePath = state.profilePicUrl,
                    contentScale = ContentScale.Crop
                )
            }
        )
    }
}