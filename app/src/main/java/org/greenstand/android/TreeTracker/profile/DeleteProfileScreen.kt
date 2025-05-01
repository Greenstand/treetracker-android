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
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.userselect.DeleteProfileState
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors.Red
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun DeleteProfileScreen() {
    val navController = LocalNavHostController.current
    val viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
    val state by viewModel.state.collectAsState(UserSelectState())
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        UserSelect(
            navigationButtonColors = AppButtonColors.ProgressGreen,
            isCreateUserEnabled = true,
            isNotificationEnabled = true,
            onNavigateForward = { user ->
                viewModel.selectUser(user)
                viewModel.updateDeleteProfileState(DeleteProfileState.SHOWDIALOG)
            }
        )

        if (state.deleteProfileState == DeleteProfileState.SHOWDIALOG) {
            CustomDialog(
                title = stringResource(R.string.delete_account_title),
                textContent = stringResource(R.string.delete_account_dialog_message),
                onPositiveClick = {
                    viewModel.deleteUser()
                },
                onNegativeClick = {
                    viewModel.updateDeleteProfileState(DeleteProfileState.DISMISSDIALOG)
                },
                content = {
                    state.selectedUser?.let { user ->
                        UserButton(
                            user = user,
                            isSelected = false,
                            buttonColors = AppButtonColors.Default,
                            selectedColor = Red,
                            onClick = {
                            }
                        )
                    }

                }

            )
        }
        if ((state.deleteProfileState == DeleteProfileState.ACCOUNTDELETEDLOCALLY ) || (state.deleteProfileState == DeleteProfileState.ACCOUNTDELETEDANDADMINREQUESTED )) {
            CustomDialog(
                title = stringResource(R.string.account_deleted),
                textContent = stringResource(R.string.account_deleted_message),
                onPositiveClick = {
                    viewModel.deleteUser()
                    if(state.selectedUser?.isPowerUser == true){
                        navController.navigate(NavRoute.SignupFlow.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }

                    } else {
                        navController.navigate(NavRoute.Settings.route) {
                            popUpTo(NavRoute.Settings.route) { inclusive = true }
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
                            }
                        )
                    }

                }

            )
        }

    }
}