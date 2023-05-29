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
package org.greenstand.android.TreeTracker.signup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun NameEntryView(viewModel: SignupViewModel, state: SignUpState) {
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val cameraLauncher = rememberLauncherForActivityResult(contract = CaptureImageContract()) { photoPath ->
        scope.launch {
            viewModel.createUser(photoPath)?.let { user ->
                if (user.isPowerUser) {
                    // In initial signup flow, clear stack and go to dashboard
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.Language.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    // In tracking setup flow, clear login stack and go to wallet selection flow
                    CaptureSetupScopeManager.getData().user = user
                    CaptureSetupScopeManager.nav.navFromNewUserCreation(navController)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    TopBarTitle()
                },
                rightAction = {
                    LanguageButton()
                }
            )
        },
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(isLeft = true) {
                        viewModel.goToCredentialEntry()
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = !state.firstName.isNullOrBlank() &&
                            !state.lastName.isNullOrBlank()
                    ) {
                        cameraLauncher.launch(true)
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BorderedTextField(
                value = state.firstName ?: "",
                padding = PaddingValues(4.dp),
                onValueChange = { updatedName -> viewModel.updateFirstName(updatedName) },
                placeholder = { Text(text = stringResource(id = R.string.first_name_hint), color = Color.White) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    autoCorrect = false,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            )
            BorderedTextField(
                value = state.lastName ?: "",
                padding = PaddingValues(4.dp),
                onValueChange = { updatedName -> viewModel.updateLastName(updatedName) },
                placeholder = { Text(text = stringResource(id = R.string.last_name_hint), color = Color.White) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go,
                    autoCorrect = false,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (!state.firstName.isNullOrBlank() && !state.lastName.isNullOrBlank()) {
                            cameraLauncher.launch(true)
                        }
                    }
                )
            )
        }
    }
}