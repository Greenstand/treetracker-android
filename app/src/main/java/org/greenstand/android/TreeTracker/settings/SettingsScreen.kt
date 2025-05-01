package org.greenstand.android.TreeTracker.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.signup.Credential
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.AppColors.Green
import org.greenstand.android.TreeTracker.view.AppColors.Red
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.dialogs.PrivacyPolicyDialog
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun SettingsScreen() {
    val navController = LocalNavHostController.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel(factory = LocalViewModelFactory.current)

    val state by viewModel.state.collectAsState(SettingsState())


    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = AppColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = CustomTheme.typography.large,
                        textAlign = TextAlign.Center,
                    )
                },

                )
        },
        bottomBar = {
            ActionBar(
                leftAction = {
                    ArrowButton(isLeft = true) {
                        navController.popBackStack()
                    }
                },

                )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                SettingsItem(
                    iconResId = R.drawable.account, // Replace with your profile icon
                    titleResId = R.string.profile_title,
                    descriptionResId = R.string.profile_description,
                    onClick = {
                        navController.navigate(NavRoute.ProfileSelect.route)
                    }
                )
                Divider(color = Color.White)

                SettingsItem(
                    iconResId = R.drawable.privacy_policy, // Replace with your privacy icon
                    titleResId = R.string.privacy_title,
                    descriptionResId = R.string.privacy_description,
                    onClick = {
                        viewModel.setPrivacyDialogVisibility(true)
                    }
                )
                Divider(color = Color.White)

                SettingsItem(
                    iconResId = R.drawable.logout, // Replace with your logout icon
                    titleResId = R.string.logout_title,
                    descriptionResId = R.string.logout_description,
                    onClick = {
                        viewModel.updateLogoutDialogVisibility(true)
                    }
                )
                Divider(color = Color.White)

                SettingsItem(
                    iconResId = R.drawable.delete, // Replace with your delete icon
                    titleResId = R.string.delete_account_title,
                    descriptionResId = R.string.delete_account_description,
                    onClick = {
                        navController.navigate(NavRoute.DeleteProfile.route)
                    }
                )
            }
            if (state.showPrivacyPolicyDialog == true) {
                PrivacyPolicyDialog(settingsViewModel = viewModel)
            }
            if(state.showLogoutDialog == true){
                CustomDialog(
                    title = stringResource(R.string.logout_dialog_title),
                    textContent = stringResource(R.string.logout_dialog_message),
                    onPositiveClick = {
                        viewModel.logout()
                        navController.navigate(NavRoute.SignupFlow.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNegativeClick = {
                        viewModel.updateLogoutDialogVisibility(false)
                    },
                    content = {
                        state.powerUser?.let { user ->
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
}

@Composable
fun SettingsItem(
    iconResId: Int,
    titleResId: Int,
    descriptionResId: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null, // decorative element
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(id = titleResId),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White

            )
            Text(
                text = stringResource(id = descriptionResId),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}