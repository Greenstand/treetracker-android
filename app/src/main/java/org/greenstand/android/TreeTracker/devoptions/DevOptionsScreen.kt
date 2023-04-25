package org.greenstand.android.TreeTracker.devoptions

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme

@Composable
fun DevOptionsRoot() {
    val viewModel: DevOptionsViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by viewModel.state.collectAsState(DevOptionsState())
    val navController = LocalNavHostController.current

    DevOptionsScreen(
        state = state,
        onParamUpdated = { param, newValue ->
            viewModel.updateParam(param, newValue)
        },
        onBackPressed = {
            navController.popBackStack()
        },
    )
}

@Composable
fun DevOptionsScreen(
    state: DevOptionsState,
    onParamUpdated: (Config, Boolean) -> Unit,
    onBackPressed: () -> Unit,
) {
    TreeTrackerTheme {
        Scaffold(
            bottomBar = {
                ActionBar(
                    leftAction = {
                        ArrowButton(isLeft = true) {
                            onBackPressed()
                        }
                    }
                )
            },
        ) {
            LazyColumn {
                items(state.params) { param ->
                    ParamListItem(param) { newValue ->
                        onParamUpdated(param, newValue)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ParamListItem(
    config: Config,
    onClicked: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.padding(16.dp),
        text = { Text(text = config.name) },
        trailing = {
            Checkbox(
                checked = config.defaultValue,
                onCheckedChange = onClicked,
            )
        }
    )
}