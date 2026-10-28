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
package org.greenstand.android.TreeTracker.treeheight

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ButtonColors
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButton

/** Test tag of the colour button at [index] in [TreeHeightSelectionState.colors]. */
internal fun treeHeightColorTag(index: Int): String = "TreeHeightColor$index"

@Composable
fun TreeHeightScreen(
    viewModel: TreeHeightSelectionViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    TreeHeight(
        state = state,
        onColorSelected = { color -> viewModel.handleAction(TreeHeightAction.SelectColor(color)) },
        onNextClicked = {
            state.selectedColor?.let {
                scope.launch {
                    CaptureFlowScopeManager.nav.navForward(navigator)
                }
            }
        },
    )
}

@Composable
fun TreeHeight(
    state: TreeHeightSelectionState = TreeHeightSelectionState(),
    onColorSelected: (ButtonColors) -> Unit = {},
    onNextClicked: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.select_tree_height_colour).uppercase(),
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .align(Alignment.Center),
                )
            }
        },
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedColor != null,
                        colors = AppButtonColors.ProgressGreen,
                        onClick = onNextClicked,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 30.dp, bottom = padding.calculateBottomPadding()),
        ) {
            itemsIndexed(state.colors) { index, color ->
                val isSelected = color == state.selectedColor
                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) 350.dp else 200.dp,
                    animationSpec = tween(durationMillis = 300),
                )
                val animatedHeight by animateDpAsState(
                    targetValue = if (isSelected) 85.dp else 62.dp,
                    animationSpec = tween(durationMillis = 300),
                )

                TreeTrackerButton(
                    colors = color,
                    isSelected = isSelected,
                    onClick = { onColorSelected(color) },
                    modifier =
                        Modifier
                            .size(
                                width = animatedWidth,
                                height = animatedHeight,
                            ).testTag(treeHeightColorTag(index)),
                ) {}
            }
        }
    }
}