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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.navigation.TreeDetailRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.SelectableImageDetail

@Composable
fun TreeListScreen(
    userWallet: String,
    userName: String,
) {
    val viewModel: TreeListViewModel = viewModel(factory = TreeListViewModelFactory(userWallet))
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState(TreeListState())

    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
                centerAction = {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = userName,
                            color = AppColors.Green,
                            fontWeight = FontWeight.Bold,
                            style = CustomTheme.typography.regular,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.tree_list_subtitle),
                            color = AppColors.Green,
                            fontWeight = FontWeight.Bold,
                            style = CustomTheme.typography.large,
                            textAlign = TextAlign.Center,
                        )
                    }
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
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedTree != null,
                        colors = AppButtonColors.ProgressGreen,
                        onClick = {
                            state.selectedTree?.let {
                                navController.navigate(TreeDetailRoute(treeId = it.id))
                            }
                        },
                    )
                },
            )
        },
    ) { paddingValues ->
        if (!state.isLoading && state.trees.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.open_mailbox_with_lowered_flag),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.tree_list_empty),
                    color = CustomTheme.textColors.lightText,
                    textAlign = TextAlign.Center,
                    style = CustomTheme.typography.large,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = paddingValues.calculateBottomPadding()),
            ) {
                items(state.trees) { tree ->
                    TreeButton(
                        tree = tree,
                        isSelected = state.selectedTree?.id == tree.id,
                        onClick = { viewModel.handleAction(TreeListAction.SelectTree(tree)) },
                    )
                }
            }
        }
    }
}

@Composable
fun TreeButton(
    tree: TreeEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    SelectableImageDetail(
        photoPath = tree.photoPath,
        isSelected = isSelected,
        buttonColors = AppButtonColors.Default,
        selectedColor = AppColors.Green,
        onClick = onClick,
        placeholderResId = R.drawable.tree_capturing_illustration,
    ) {
        Text(
            text = tree.createdAt.toString().take(10),
            color = CustomTheme.textColors.lightText,
            style = CustomTheme.typography.small,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (tree.note.isNotBlank()) {
            Text(
                text = tree.note,
                color = CustomTheme.textColors.lightText,
                style = CustomTheme.typography.small,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}