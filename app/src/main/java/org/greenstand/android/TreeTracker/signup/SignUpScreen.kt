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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.Navigator
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.koin.compose.viewmodel.koinViewModel

/**
 * Sign-up for a second or later user was opened from a user picker: go back to it and hand it the
 * user, so the screen that opened sign-up decides what happens next (capture setup continues with
 * that user; the profile list just shows them). Falls back to the dashboard if sign-up is the only
 * screen on the back stack.
 */
private fun returnUser(
    navigator: Navigator,
    userId: Long,
) {
    if (!navigator.popBackStackWithResult(SignupFlowRoute.NewUserId, userId)) {
        navigator.navigate(DashboardRoute) { popUpToRoot() }
    }
}

@Composable
fun SignUpScreen(viewModel: SignupViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    val cameraLauncher =
        rememberLauncherForActivityResult(contract = CaptureImageContract()) { photoPath ->
            scope.launch {
                viewModel.createUser(photoPath)?.let { user ->
                    if (user.isPowerUser) {
                        navigator.navigate(DashboardRoute) {
                            popUpTo<LanguageRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        returnUser(navigator, user.id)
                    }
                }
            }
        }

    SignUp(
        state = state,
        onHandleAction = { action ->
            when (action) {
                is SignupAction.NavigateBack -> navigator.popBackStack()
                is SignupAction.LaunchCamera -> cameraLauncher.launch(true)
                is SignupAction.ExistingUserSelected -> {
                    val user = action.user
                    if (state.isTherePowerUser == false) {
                        viewModel.handleAction(SignupAction.SetExistingUserAsPowerUser(user.id))
                        navigator.navigate(DashboardRoute) {
                            popUpTo<LanguageRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        returnUser(navigator, user.id)
                    }
                }
                else -> viewModel.handleAction(action)
            }
        },
        isFormValid = { viewModel.isFormValid() },
    )
}

@Composable
fun SignUp(
    state: SignUpState = SignUpState(),
    onHandleAction: (SignupAction) -> Unit = {},
    isFormValid: () -> Boolean = { false },
) {
    if (state.isCredentialView) {
        CredentialEntryView(
            state = state,
            onHandleAction = onHandleAction,
        )
    } else {
        NameEntryView(
            state = state,
            onHandleAction = onHandleAction,
            isFormValid = isFormValid,
        )
    }
}