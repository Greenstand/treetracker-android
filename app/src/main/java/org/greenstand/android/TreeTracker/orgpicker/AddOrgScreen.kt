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
package org.greenstand.android.TreeTracker.orgpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.TreeTrackerButton

@Composable
fun AddOrgScreen(viewModel: AddOrgViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(AddOrgState())

    Scaffold(
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(isLeft = true) {
                        CaptureSetupScopeManager.nav.navBackward(navController)
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                    ) {
                        viewModel.setDefaultOrg()
                        CaptureSetupScopeManager.nav.navForward(navController)
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            BorderedTextField(
                value = state.orgName,
                modifier = Modifier.height(80.dp),
                padding = PaddingValues(4.dp),
                onValueChange = { updatedName -> viewModel.updateOrgName(updatedName) },
                placeholder = { Text(text = stringResource(id = R.string.organization), color = Color.White) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go,
                    autoCorrect = false,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        viewModel.setDefaultOrg()
                        CaptureSetupScopeManager.nav.navForward(navController)
                    }
                )
            )
            state.previousOrgName?.let { prevOrgName ->
                TreeTrackerButton(
                    onClick = { viewModel.applyOrgAutofill() },
                    modifier = Modifier
                        .padding(16.dp)
                        .size(height = 80.dp, width = 156.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = prevOrgName,
                        fontWeight = FontWeight.Bold,
                        color = CustomTheme.textColors.primaryText,
                        style = CustomTheme.typography.regular,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}