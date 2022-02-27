package org.greenstand.android.TreeTracker.camera

import android.content.res.Resources
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.signup.SignUpState
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.signup.getComposeViewModelOwner
import org.greenstand.android.TreeTracker.view.*
import org.koin.androidx.compose.getKoin
import org.koin.androidx.compose.getViewModel
import org.koin.core.qualifier.named

@Composable
fun SelfieScreen() {
    val navController = LocalNavHostController.current
    val cameraControl = remember { CameraControl() }
    val scope = getKoin().getOrCreateScope("SIGN_UP_SCOPE", named("SIGN_UP"));
    val viewModel = getViewModel<SignupViewModel>(
        owner = getComposeViewModelOwner(),
        scope = scope
    )
    val state by viewModel.state.observeAsState(SignUpState())

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    TopBarTitle()
                },
            )
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    Image(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1.0f)
                            .clickable {
                                viewModel.updateSelfieTutorialDialog(true)
                            },
                        painter = painterResource(id = R.drawable.info_icon),
                        contentDescription = null,
                    )
                }
            )
        }
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
        ) {
            if (state.showSelfieTutorial == true) {
                SelfieTutorial(
                    onCompleteClick = {
                        viewModel.updateSelfieTutorialDialog(false)
                    }
                )
            }
            CaptureButton(
                onClick = {
                    cameraControl.captureImage()
                },
                isEnabled = true
            )
        }
    }
}