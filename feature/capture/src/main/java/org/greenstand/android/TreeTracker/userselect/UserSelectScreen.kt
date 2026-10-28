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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.Navigator
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.koin.compose.koinInject
import org.greenstand.android.TreeTracker.view.AppButtonColors

/**
 * The first step of capture setup: pick who is planting. Picking a user (or signing up a new one,
 * which comes back here with the new user) starts a fresh capture setup with that user.
 */
@Composable
fun UserSelectScreen(userRepo: UserRepo = koinInject()) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val newUserId = navigator.takeResult(SignupFlowRoute.NewUserId) ?: return@LaunchedEffect
        userRepo.getUser(newUserId)?.let { startCaptureSetup(it, navigator) }
    }

    UserSelect(
        navigationButtonColors = AppButtonColors.ProgressGreen,
        isCreateUserEnabled = true,
        isNotificationEnabled = true,
        onNavigateForward = { user -> scope.launch { startCaptureSetup(user, navigator) } },
    )
}

/** Starts a fresh capture setup for [user] and moves to the org's next setup step. */
internal suspend fun startCaptureSetup(
    user: User,
    navigator: Navigator,
) {
    CaptureSetupScopeManager.open()
    CaptureSetupScopeManager.getData().user = user
    CaptureSetupScopeManager.nav.navForward(navigator)
}