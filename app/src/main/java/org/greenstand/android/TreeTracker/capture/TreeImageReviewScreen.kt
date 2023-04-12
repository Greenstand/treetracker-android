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
package org.greenstand.android.TreeTracker.capture


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ApprovalButton
import org.greenstand.android.TreeTracker.view.InfoButton
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.TreeCaptureReviewTutorial
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun TreeImageReviewScreen(
    viewModel: TreeImageReviewViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val state by viewModel.state.observeAsState(TreeImageReviewState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    TreeTrackerButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 100.dp, 60.dp),
                        onClick = {
                            viewModel.setDialogState(true)
                        }
                    ) {
                        Text(stringResource(R.string.note))
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ApprovalButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = {
                        CaptureFlowScopeManager.nav.navBackward(navController)
                    },
                    approval = false
                )
                ApprovalButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = {
                        scope.launch {
                            CaptureFlowScopeManager.nav.navForward(navController)
                        }
                    },
                    approval = true
                )
                InfoButton(
                    modifier = Modifier
                        .size(60.dp),
                    onClick = { viewModel.updateReviewTutorialDialog(true) }
                )
            }
        }
    ) {
        if (state.isDialogOpen) {
            NoteDialog(state = state, viewModel = viewModel)
        }
        if (state.showReviewTutorial == true) {
            TreeCaptureReviewTutorial(
                onCompleteClick = {
                    viewModel.updateReviewTutorialDialog(false)
                }
            )
        }
        LocalImage(
            modifier = Modifier.fillMaxSize(),
            imagePath = state.treeImagePath ?: "",
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun NoteDialog(state: TreeImageReviewState, viewModel: TreeImageReviewViewModel) {
    CustomDialog(
        dialogIcon = painterResource(id = R.drawable.note),
        title = stringResource(R.string.add_note_to_tree),
        textInputValue = state.note,
        onTextInputValueChange = { text -> viewModel.updateNote(text) },
        onPositiveClick = {
            viewModel.addNote()
        },
        onNegativeClick = {
            viewModel.setDialogState(false)
        }
    )
}