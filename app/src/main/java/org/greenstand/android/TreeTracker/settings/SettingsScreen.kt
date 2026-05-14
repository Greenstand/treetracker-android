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
package org.greenstand.android.TreeTracker.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.navigation.DeleteProfileRoute
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.ProfileSelectRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.navigateSafely
import org.greenstand.android.TreeTracker.utilities.popBackStackSafely
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.AppColors.Red
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.UserButton
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog
import org.greenstand.android.TreeTracker.view.dialogs.PrivacyPolicyDialog
import org.maplibre.android.MapLibre

@Composable
fun SettingsScreen() {
    val navController = LocalNavHostController.current
    val viewModel: SettingsViewModel = viewModel(factory = LocalViewModelFactory.current)
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    Settings(
        state = state,
        onHandleAction = { action ->
            when (action) {
                is SettingsAction.NavigateToProfile -> navController.navigateSafely(ProfileSelectRoute)
                is SettingsAction.NavigateToEditTrees -> navController.navigateSafely(TreeEditUserSelectRoute)
                is SettingsAction.NavigateToMap -> navController.navigateSafely(MapRoute)
                is SettingsAction.NavigateToDeleteAccount -> navController.navigateSafely(DeleteProfileRoute)
                is SettingsAction.NavigateBack -> navController.popBackStackSafely()
                is SettingsAction.LogoutConfirmed -> {
                    viewModel.handleAction(SettingsAction.Logout)
                    navController.navigateSafely(SignupFlowRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                else -> viewModel.handleAction(action)
            }
        },
    )
}

@Composable
fun Settings(
    state: SettingsState = SettingsState(),
    onHandleAction: (SettingsAction) -> Unit = {},
) {
    Scaffold(
        topBar = {
            ActionBar(
                modifier = Modifier.statusBarsPadding(),
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
                modifier = Modifier.navigationBarsPadding(),
                leftAction = {
                    ArrowButton(isLeft = true) {
                        onHandleAction(SettingsAction.NavigateBack)
                    }
                },
            )
        },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(it),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp),
            ) {
                SettingsItem(
                    iconResId = R.drawable.account,
                    titleResId = R.string.profile_title,
                    descriptionResId = R.string.profile_description,
                    onClick = { onHandleAction(SettingsAction.NavigateToProfile) },
                )
                Divider(color = Color.White)

                SettingsItem(
                    iconResId = R.drawable.note,
                    titleResId = R.string.edit_trees_title,
                    descriptionResId = R.string.edit_trees_description,
                    iconTint = Color.White,
                    onClick = { onHandleAction(SettingsAction.NavigateToEditTrees) },
                )
                Divider(color = Color.White)

                if (FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA) {
                    SettingsItem(
                        iconResId = R.drawable.map_icon,
                        titleResId = R.string.map_title,
                        descriptionResId = R.string.map_description,
                        onClick = { onHandleAction(SettingsAction.NavigateToMap) },
                    )
                    Divider(color = Color.White)
                }

                SettingsItem(
                    iconResId = R.drawable.privacy_policy,
                    titleResId = R.string.privacy_title,
                    descriptionResId = R.string.privacy_description,
                    onClick = { onHandleAction(SettingsAction.SetPrivacyDialogVisibility(true)) },
                )

                if (FeatureFlags.DEBUG_ENABLED) {
                    Divider(color = Color.White)

                    SettingsItem(
                        iconResId = R.drawable.logout,
                        titleResId = R.string.logout_title,
                        descriptionResId = R.string.logout_description,
                        onClick = { onHandleAction(SettingsAction.UpdateLogoutDialogVisibility(true)) },
                    )
                    Divider(color = Color.White)

                    SettingsItem(
                        iconResId = R.drawable.delete,
                        titleResId = R.string.delete_account_title,
                        descriptionResId = R.string.delete_account_description,
                        onClick = { onHandleAction(SettingsAction.NavigateToDeleteAccount) },
                    )
                }

                Text(
                    text =
                        stringResource(
                            id = R.string.app_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }
            if (state.showPrivacyPolicyDialog == true) {
                PrivacyPolicyDialog(onDismiss = { onHandleAction(SettingsAction.SetPrivacyDialogVisibility(false)) })
            }
            if (state.showLogoutDialog == true) {
                CustomDialog(
                    title = stringResource(R.string.logout_dialog_title),
                    textContent = stringResource(R.string.logout_dialog_message),
                    onPositiveClick = { onHandleAction(SettingsAction.LogoutConfirmed) },
                    onNegativeClick = { onHandleAction(SettingsAction.UpdateLogoutDialogVisibility(false)) },
                    content = {
                        state.powerUser?.let { user ->
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
}

@Composable
fun SettingsItem(
    iconResId: Int,
    titleResId: Int,
    descriptionResId: Int,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null, // decorative element
            modifier = Modifier.size(24.dp),
            colorFilter = iconTint?.let { ColorFilter.tint(it) },
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(id = titleResId),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
            )
            Text(
                text = stringResource(id = descriptionResId),
                fontSize = 14.sp,
                color = Color.Gray,
            )
        }
    }
}