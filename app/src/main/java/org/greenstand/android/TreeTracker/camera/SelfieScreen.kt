package org.greenstand.android.TreeTracker.camera

import android.content.res.Resources
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.Size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.*

@Composable
fun SelfieScreen() {
    val navController = LocalNavHostController.current
    val cameraControl = remember { CameraControl() }
    val displayMetrics = Resources.getSystem().displayMetrics

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    TopBarTitle()
                   },
            ) }
    ) {
            Camera(
                isSelfieMode = true,
                cameraControl = cameraControl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f),
                onImageCaptured = {
                    navController.navigate(NavRoute.ImageReview.create(it.path))
                }
            )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ){
            DepthButton(
                modifier = Modifier
                    .size(72.dp, 72.dp)
                    .clickable { cameraControl.captureImage() },
                onClick = { cameraControl.captureImage() },
                colors = DepthButtonColors(
                    color = AppColors.Green,
                    shadowColor =   AppColors.GreenShadow,
                    disabledColor = AppColors.GrayShadow,
                    disabledShadowColor = AppColors.GrayShadow
                ),
                shape = DepthSurfaceShape.Circle,
            ) {
                ImageCaptureCircle(
                    modifier = Modifier
                        .size(72.dp, 72.dp),
                    color = AppColors.Green,
                    shadowColor = AppColors.Gray,
                )
            }
        }
    }
}