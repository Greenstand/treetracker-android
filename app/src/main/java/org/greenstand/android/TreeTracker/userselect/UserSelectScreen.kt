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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.AppButtonColors

@Composable
fun UserSelectScreen(
    viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    UserSelect(
        navigationButtonColors = AppButtonColors.ProgressGreen,
        isCreateUserEnabled = true,
        isNotificationEnabled = true,
        viewModel = viewModel,
        onNavigateForward = {
            scope.launch { CaptureSetupScopeManager.nav.navForward(navigator) }
        },
    )
}