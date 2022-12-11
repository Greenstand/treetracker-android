package org.greenstand.android.TreeTracker.treeheight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButton

@Composable
fun TreeHeightScreen(
) {
    val viewModel: TreeHeightSelectionViewModel = viewModel(factory = LocalViewModelFactory.current)
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(TreeHeightSelectionState())

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.select_tree_height_colour).uppercase(),
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.Center),
                )
            }
        },
        bottomBar = {
            ActionBar(
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedColour != null,
                        colors = AppButtonColors.ProgressGreen,
                        onClick = {
                            state.selectedColour?.let {
                                CaptureFlowScopeManager.nav.navForward(navController)
                            }
                        }
                    )
                },
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 30.dp)
        ) {
            items(state.colours) { color ->
                TreeTrackerButton(
                    colors = color,
                    isSelected = color == state.selectedColour,
                    onClick = { viewModel.selectColor(color) },
                    modifier = if (color == state.selectedColour) Modifier.size(
                        width = 350.dp,
                        height = 85.dp
                    ) else Modifier.size(width = 200.dp, height = 62.dp)
                ) {}
            }
        }
    }
}