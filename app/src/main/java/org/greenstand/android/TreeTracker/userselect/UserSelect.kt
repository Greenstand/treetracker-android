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

import android.os.SystemClock
import android.view.ViewConfiguration
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.greenstand.android.TreeTracker.view.UserButton

@Composable
fun UserSelect(
    navigationButtonColors: DepthButtonColors,
    isCreateUserEnabled: Boolean,
    isNotificationEnabled: Boolean,
    isFromSettings: Boolean = false,
    selectedColor: Color = AppColors.Green,
    viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current),
    onNavigateForward: (User) -> Unit,
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsState(UserSelectState())
    val shownAtMs = remember { SystemClock.uptimeMillis() }

    UserSelectContent(
        state = state,
        navigationButtonColors = navigationButtonColors,
        isCreateUserEnabled = isCreateUserEnabled,
        isNotificationEnabled = isNotificationEnabled,
        selectedColor = selectedColor,
        onUserClicked = { user -> viewModel.handleAction(UserSelectAction.SelectUser(user)) },
        onCreateUserClicked = {
            // On small screens the add-user button sits where Dashboard's Track button was, so
            // the second tap of a double tap on Track lands here (#1423). Drop it.
            if (!isSecondTapOfDoubleTap(shownAtMs, SystemClock.uptimeMillis())) {
                navigator.throttledNavigate(SignupFlowRoute)
            }
        },
        onBackClicked = {
            if (!isFromSettings) {
                navigator.throttledNavigate(DashboardRoute) {
                    popUpTo<DashboardRoute> { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navigator.throttledPopBackStack()
            }
        },
        onNavigateForward = onNavigateForward,
    )
}

/**
 * Whether a tap at [nowMs] on a screen shown at [shownAtMs] comes too soon after the screen
 * appeared to be meant for it: within the system double-tap timeout, it is the second half of
 * a double tap on the previous screen.
 */
internal fun isSecondTapOfDoubleTap(
    shownAtMs: Long,
    nowMs: Long,
    doubleTapTimeoutMs: Long = ViewConfiguration.getDoubleTapTimeout().toLong(),
): Boolean = nowMs - shownAtMs < doubleTapTimeoutMs

/**
 * Stateless user grid shared by every "pick a planter" screen. [onNavigateForward] is only
 * called with the selected user, while one is selected.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserSelectContent(
    state: UserSelectState,
    navigationButtonColors: DepthButtonColors,
    isCreateUserEnabled: Boolean,
    isNotificationEnabled: Boolean,
    selectedColor: Color = AppColors.Green,
    onUserClicked: (User) -> Unit = {},
    onCreateUserClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {},
    onNavigateForward: (User) -> Unit = {},
) {
    Scaffold(
        bottomBar = {
            ActionBar(
                modifier = Modifier.navigationBarsPadding(),
                centerAction = {
                    if (isCreateUserEnabled) {
                        OrangeAddButton(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = onCreateUserClicked,
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
                        },
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = navigationButtonColors,
                        onClick = onBackClicked,
                    )
                },
            )
        },
    ) {
        LazyVerticalGrid(
            modifier = Modifier.statusBarsPadding(),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = it.calculateBottomPadding()),
        ) {
            items(state.users) { user ->
                UserButton(
                    user = user,
                    isSelected = state.selectedUser?.id == user.id,
                    buttonColors = AppButtonColors.Default,
                    selectedColor = selectedColor,
                    onClick = { onUserClicked(user) },
                    isNotificationEnabled = isNotificationEnabled && user.unreadMessagesAvailable,
                )
            }
        }
    }
}