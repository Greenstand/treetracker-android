package org.greenstand.android.TreeTracker.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.camera.Camera
import org.greenstand.android.TreeTracker.camera.CameraControl
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.PermissionRequest
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.CaptureButton
import org.greenstand.android.TreeTracker.view.InfoButton
import org.greenstand.android.TreeTracker.view.TreeCaptureTutorial
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButtonShape
import org.greenstand.android.TreeTracker.view.UserImageButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

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

    BackHandler(enabled = true) {
        scope.launch {
            CaptureFlowScopeManager.nav.goToDashboard(navController)
        }
    }

    Scaffold(
        bottomBar = {
            ActionBar(
                modifier = Modifier.background(color = AppColors.Gray),
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        onClick = {
                            scope.launch {
                                CaptureFlowScopeManager.nav.goToDashboard(navController)
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
                },
                rightAction = {
                    InfoButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = {
                            viewModel.updateCaptureTutorialDialog(true)
                        }
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
                onPositiveClick = {
                    viewModel.updateBadGpsDialogState(null)
                },
                onNegativeClick = {
                    CaptureFlowScopeManager.nav.goToDashboard(navController)
                }
            )
        }
        if (state.showCaptureTutorial == true) {
            TreeCaptureTutorial(
                onCompleteClick = {
                    viewModel.updateCaptureTutorialDialog(false)
                }
            )
        }

        Camera(
            isSelfieMode = false,
            cameraControl = cameraControl,
            onImageCaptured = {
                viewModel.onImageCaptured(it)
                if (state.isLocationAvailable == true) {
                    CaptureFlowScopeManager.nav.navForward(navController)
                }
            }
        )
        ActionBar(
            leftAction = {
                UserImageButton(
                    onClick = {
                        scope.launch {
                            CaptureFlowScopeManager.nav.goToUserSelect(navController)
                        }
                    },
                    imagePath = state.profilePicUrl
                )
            },
            rightAction = {
                if (FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA) {
                    TreeTrackerButton(
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
                        shape = TreeTrackerButtonShape.Circle
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
        CaptureCustomLoading(isLoading = state.isGettingLocation || state.isCreatingFakeTrees, progress = state.convergencePercentage )
    }
}

@Composable
fun CaptureCustomLoading(isLoading: Boolean, progress: Float) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .padding(bottom = 90.dp)
                .fillMaxSize(), contentAlignment = Alignment.BottomCenter
        ) {

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(20.dp)
                    .background(color = AppColors.Gray, shape = RoundedCornerShape(percent = 10))
                    .alpha(0.7f)
                    .border(1.dp, color = AppColors.Green, shape = RoundedCornerShape(percent = 10))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = stringResource(R.string.tracking_progress_header),
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = stringResource(R.string.tracking_progress_message),
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                )
                Spacer(modifier = Modifier.height(15.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(height = 80.dp, width = 80.dp)
                        .padding(top = 10.dp),
                    color = AppColors.Green,
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "${progress.times(100).toInt()} ${"%"}",
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}