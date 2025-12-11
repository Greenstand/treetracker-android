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
package org.greenstand.android.TreeTracker.userselect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.greenstand.android.TreeTracker.view.UserButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserSelect(
    navigationButtonColors: DepthButtonColors,
    isCreateUserEnabled: Boolean,
    isNotificationEnabled: Boolean,
    isFromSettings: Boolean = false,
    selectedColor: Color = AppColors.Green,
    onNavigateForward: (User) -> Unit,
) {
    val viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
    val navController = LocalNavHostController.current
    val state by viewModel.state.collectAsState(UserSelectState())

    Scaffold(
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                centerAction = {
                    if (isCreateUserEnabled) {
                        OrangeAddButton(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = { navController.navigate(NavRoute.SignupFlow.route) }
                        )
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedUser != null,
                        colors = navigationButtonColors,
                        onClick = {
                            state.selectedUser?.let {
                                onNavigateForward(it)
                            }
                        }
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = navigationButtonColors,
                        onClick = {
                            if(!isFromSettings) {
                                navController.navigate(NavRoute.Dashboard.route) {
                                    popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                }
            )
        }
    ) {
        LazyVerticalGrid(
            modifier = Modifier.statusBarsPadding(),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = it.calculateBottomPadding())
        ) {
            items(state.users) { user ->
                UserButton(
                    user = user,
                    isSelected = state.selectedUser?.id == user.id,
                    buttonColors = AppButtonColors.Default,
                    selectedColor = selectedColor,
                    onClick = { viewModel.selectUser(user) },
                    isNotificationEnabled = isNotificationEnabled && user.unreadMessagesAvailable
                )
            }
        }
    }
}