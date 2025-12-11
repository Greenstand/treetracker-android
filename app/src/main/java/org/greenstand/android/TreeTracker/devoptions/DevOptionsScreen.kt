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
package org.greenstand.android.TreeTracker.devoptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    onParamUpdated: (Config, Any) -> Unit,
    onBackPressed: () -> Unit,
) {
    TreeTrackerTheme {
        Scaffold(
            bottomBar = {
                ActionBar(
                    modifier = Modifier.navigationBarsPadding(),
                    leftAction = {
                        ArrowButton(isLeft = true) {
                            onBackPressed()
                        }
                    }
                )
            },
        ) {
            LazyColumn(modifier = Modifier.padding(it)) {
                items(state.params) { param ->
                    ParamListItem(param) { newValue ->
                        onParamUpdated(param, newValue)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamListItem(
    config: Config,
    onClicked: (Any) -> Unit,
) {
    when (config) {
        is BooleanConfig -> BooleanParamListItem(
            config = config,
            onClicked = { onClicked(it) }
        )
        is IntConfig -> IntParamListItem(
            config = config,
            onTextUpdated = { onClicked(it) }
        )
        is FloatConfig -> FloatParamListItem(
            config = config,
            onTextUpdated = { onClicked(it) }
        )
    }
}

@Composable
private fun BooleanParamListItem(
    config: BooleanConfig,
    onClicked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = config.name,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = config.defaultValue,
            onCheckedChange = onClicked,
        )
    }
}

@Composable
private fun IntParamListItem(
    config: IntConfig,
    onTextUpdated: (Int) -> Unit,
) {
    var text: String by remember { mutableStateOf(config.defaultValue.toString()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = config.name)
        TextField(
            value = text,
            onValueChange = {
                text = it
                text.toIntOrNull()?.let { value ->
                    onTextUpdated(value)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            )
        )
    }
}

@Composable
private fun FloatParamListItem(
    config: FloatConfig,
    onTextUpdated: (Float) -> Unit,
) {
    var text: String by remember { mutableStateOf(config.defaultValue.toString()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = config.name)
        TextField(
            value = text,
            onValueChange = {
                text = it
                text.toFloatOrNull()?.let { value ->
                    onTextUpdated(value)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            )
        )
    }
}