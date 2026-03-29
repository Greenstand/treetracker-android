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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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

    TreeImageReview(
        state = state,
        onNoteClicked = { viewModel.setDialogState(true) },
        onRejectClicked = {
            CaptureFlowScopeManager.nav.navBackward(navController)
        },
        onApproveClicked = {
            viewModel.checkIfCanNavigateForward {
                scope.launch {
                    CaptureFlowScopeManager.nav.navForward(navController)
                }
            }
        },
        onInfoClicked = { viewModel.updateReviewTutorialDialog(true) },
        onNoteUpdated = { viewModel.updateNote(it) },
        onNoteAdded = { viewModel.addNote() },
        onDialogDismissed = { viewModel.setDialogState(false) },
        onTutorialCompleted = { viewModel.updateReviewTutorialDialog(false) },
    )
}

@Composable
fun TreeImageReview(
    state: TreeImageReviewState = TreeImageReviewState(),
    onNoteClicked: () -> Unit = { },
    onRejectClicked: () -> Unit = { },
    onApproveClicked: () -> Unit = { },
    onInfoClicked: () -> Unit = { },
    onNoteUpdated: (String) -> Unit = { },
    onNoteAdded: () -> Unit = { },
    onDialogDismissed: () -> Unit = { },
    onTutorialCompleted: () -> Unit = { },
) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = {
                    TreeTrackerButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 100.dp, 60.dp),
                        onClick = onNoteClicked,
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
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ApprovalButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = onRejectClicked,
                    approval = false
                )
                ApprovalButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = onApproveClicked,
                    approval = true
                )
                InfoButton(
                    modifier = Modifier
                        .size(60.dp),
                    onClick = onInfoClicked,
                )
            }
        }
    ) {
        if (state.isDialogOpen) {
            NoteDialog(
                state = state,
                onNoteUpdated = onNoteUpdated,
                onNoteAdded = onNoteAdded,
                onDialogDismissed = onDialogDismissed,
            )
        }
        if (state.showReviewTutorial == true) {
            TreeCaptureReviewTutorial(
                onCompleteClick = onTutorialCompleted,
            )
        }
        LocalImage(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            imagePath = state.treeImagePath ?: "",
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun NoteDialog(
    state: TreeImageReviewState,
    onNoteUpdated: (String) -> Unit = { },
    onNoteAdded: () -> Unit = { },
    onDialogDismissed: () -> Unit = { },
) {
    CustomDialog(
        dialogIcon = painterResource(id = R.drawable.note),
        title = stringResource(R.string.add_note_to_tree),
        textInputValue = state.note,
        onTextInputValueChange = { text -> onNoteUpdated(text) },
        onPositiveClick = onNoteAdded,
        onNegativeClick = onDialogDismissed,
    )
}