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
package org.greenstand.android.TreeTracker.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.navigation.ImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.signup.SignupAction
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.CaptureButton
import org.greenstand.android.TreeTracker.view.InfoButton
import org.greenstand.android.TreeTracker.view.SelfieTutorial
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.koin.androidx.compose.getViewModel

@Composable
fun SelfieScreen() {
    val navigator = LocalNavigator.current
    val cameraControl = remember { CameraControl() }
    val viewModel = getViewModel<SignupViewModel>()
    val state by viewModel.state.collectAsState()

    Selfie(
        showSelfieTutorial = state.showSelfieTutorial == true,
        onInfoClicked = {
            viewModel.handleAction(SignupAction.UpdateSelfieTutorialDialog(true))
        },
        onCaptureClicked = {
            cameraControl.captureImage()
        },
        onTutorialCompleted = {
            viewModel.handleAction(SignupAction.UpdateSelfieTutorialDialog(false))
        },
        cameraPreview = { modifier ->
            Camera(
                isSelfieMode = true,
                cameraControl = cameraControl,
                modifier = modifier,
                onImageCaptured = {
                    navigator.navigate(ImageReviewRoute(photoPath = it.path))
                },
            )
        },
    )
}

/**
 * Stateless selfie UI. The camera preview is a slot ([cameraPreview]) so the screen can be
 * rendered without CameraX; it receives the modifier that sizes the preview.
 */
@Composable
fun Selfie(
    showSelfieTutorial: Boolean,
    onInfoClicked: () -> Unit = {},
    onCaptureClicked: () -> Unit = {},
    onTutorialCompleted: () -> Unit = {},
    cameraPreview: @Composable (Modifier) -> Unit = {},
) {
    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = {
                    TopBarTitle()
                },
            )
        },
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                rightAction = {
                    InfoButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = onInfoClicked,
                    )
                },
                centerAction = {
                    CaptureButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = onCaptureClicked,
                        isEnabled = true,
                    )
                },
            )
        },
    ) { padding ->
        cameraPreview(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .aspectRatio(1.0f),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 10.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (showSelfieTutorial) {
                SelfieTutorial(
                    onCompleteClick = onTutorialCompleted,
                )
            }
        }
    }
}