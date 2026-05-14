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
package org.greenstand.android.TreeTracker.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.userselect.DeleteProfileState
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.userselect.UserSelectAction
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.utilities.navigateSafely
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors.Red
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun DeleteProfileScreen() {
    val navController = LocalNavHostController.current
    val viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by viewModel.state.collectAsState()
    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        UserSelect(
            navigationButtonColors = AppButtonColors.ProgressGreen,
            isCreateUserEnabled = true,
            isNotificationEnabled = true,
            onNavigateForward = { user ->
                viewModel.handleAction(UserSelectAction.SelectUser(user))
                viewModel.handleAction(UserSelectAction.UpdateDeleteProfileState(DeleteProfileState.SHOWDIALOG))
            },
        )

        if (state.deleteProfileState == DeleteProfileState.SHOWDIALOG) {
            CustomDialog(
                title = stringResource(R.string.delete_account_title),
                textContent = stringResource(R.string.delete_account_dialog_message),
                onPositiveClick = {
                    viewModel.handleAction(UserSelectAction.DeleteUser)
                },
                onNegativeClick = {
                    viewModel.handleAction(UserSelectAction.UpdateDeleteProfileState(DeleteProfileState.DISMISSDIALOG))
                },
                content = {
                    state.selectedUser?.let { user ->
                        UserButton(
                            user = user,
                            isSelected = false,
                            buttonColors = AppButtonColors.Default,
                            selectedColor = Red,
                            onClick = {
                            },
                        )
                    }
                },
            )
        }
        if ((state.deleteProfileState == DeleteProfileState.ACCOUNTDELETEDLOCALLY) || (state.deleteProfileState == DeleteProfileState.ACCOUNTDELETEDANDADMINREQUESTED)) {
            CustomDialog(
                title = stringResource(R.string.account_deleted),
                textContent = stringResource(R.string.account_deleted_message),
                onPositiveClick = {
                    viewModel.handleAction(UserSelectAction.DeleteUser)
                    if (state.selectedUser?.isPowerUser == true) {
                        navController.navigateSafely(SignupFlowRoute) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigateSafely(SettingsRoute) {
                            popUpTo<SettingsRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                content = {
                    state.selectedUser?.let { user ->
                        UserButton(
                            user = user,
                            isSelected = false,
                            buttonColors = AppButtonColors.Default,
                            selectedColor = Red,
                            onClick = {
                            },
                        )
                    }
                },
            )
        }
    }
}