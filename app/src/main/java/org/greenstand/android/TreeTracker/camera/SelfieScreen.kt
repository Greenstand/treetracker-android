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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.signup.SignUpState
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.CaptureButton
import org.greenstand.android.TreeTracker.view.InfoButton
import org.greenstand.android.TreeTracker.view.SelfieTutorial
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.koin.androidx.compose.getViewModel

@Composable
fun SelfieScreen() {
    val navController = LocalNavHostController.current
    val cameraControl = remember { CameraControl() }
    val viewModel = getViewModel<SignupViewModel>()
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
                    InfoButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = {
                            viewModel.updateSelfieTutorialDialog(true)
                        }
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