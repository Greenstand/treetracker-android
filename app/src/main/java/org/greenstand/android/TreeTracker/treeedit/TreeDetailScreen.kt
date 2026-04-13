/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.treeedit

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun TreeDetailScreen(treeId: Long) {
    val viewModel: TreeDetailViewModel = viewModel(factory = TreeDetailViewModelFactory(treeId))
    val navController = LocalNavHostController.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState(TreeDetailState())
    val noteSavedMessage = stringResource(R.string.tree_note_saved)

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(state.noteSaved) {
        if (state.noteSaved) {
            Toast.makeText(context, noteSavedMessage, Toast.LENGTH_SHORT).show()
            viewModel.handleAction(TreeDetailAction.NoteSavedShown)
        }
    }

    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = {
                    Text(
                        text = stringResource(id = R.string.tree_detail_title),
                        color = AppColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = CustomTheme.typography.large,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = AppButtonColors.ProgressGreen,
                        onClick = { navController.popBackStack() },
                    )
                },
            )
        },
    ) { paddingValues ->
        state.tree?.let { tree ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (tree.photoPath != null) {
                    LocalImage(
                        imagePath = tree.photoPath!!,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Gray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.tree_capturing_illustration),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.5f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailField(stringResource(R.string.tree_uuid_label), tree.uuid)
                DetailField(stringResource(R.string.tree_latitude_label), tree.latitude.toString())
                DetailField(stringResource(R.string.tree_longitude_label), tree.longitude.toString())
                DetailField(stringResource(R.string.tree_created_at_label), tree.createdAt.toString())
                DetailField(stringResource(R.string.tree_uploaded_label), if (tree.uploaded) "Yes" else "No")

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.tree_note_label),
                    color = AppColors.Green,
                    fontWeight = FontWeight.Bold,
                    style = CustomTheme.typography.regular,
                )
                OutlinedTextField(
                    value = state.editedNote,
                    onValueChange = { viewModel.handleAction(TreeDetailAction.UpdateNote(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    enabled = !tree.uploaded,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        disabledTextColor = Color.Gray,
                        cursorColor = AppColors.Green,
                        focusedBorderColor = AppColors.Green,
                        unfocusedBorderColor = Color.Gray,
                        disabledBorderColor = Color.DarkGray,
                    ),
                )

                if (!tree.uploaded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TreeTrackerButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = AppButtonColors.ProgressGreen,
                        onClick = { viewModel.handleAction(TreeDetailAction.SaveNote) },
                    ) {
                        Text(
                            text = stringResource(R.string.tree_save_note),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = CustomTheme.typography.regular,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TreeTrackerButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = AppButtonColors.DeclineRed,
                        onClick = { viewModel.handleAction(TreeDetailAction.SetDeleteDialogVisibility(true)) },
                    ) {
                        Text(
                            text = stringResource(R.string.tree_delete),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = CustomTheme.typography.regular,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (state.showDeleteConfirmation) {
            CustomDialog(
                title = stringResource(R.string.tree_delete_confirm_title),
                textContent = stringResource(R.string.tree_delete_confirm_message),
                onPositiveClick = { viewModel.handleAction(TreeDetailAction.DeleteTree) },
                onNegativeClick = { viewModel.handleAction(TreeDetailAction.SetDeleteDialogVisibility(false)) },
            )
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            color = Color.Gray,
            style = CustomTheme.typography.small,
        )
        Text(
            text = value,
            color = Color.White,
            style = CustomTheme.typography.regular,
        )
    }
}
