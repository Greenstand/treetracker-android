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
package org.greenstand.android.TreeTracker.treeedit

import androidx.compose.runtime.Composable
import org.greenstand.android.TreeTracker.navigation.TreeListRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.utilities.throttledNavigate
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors

@Composable
fun TreeEditUserSelectScreen() {
    val navController = LocalNavHostController.current
    UserSelect(
        navigationButtonColors = AppButtonColors.ProgressGreen,
        isCreateUserEnabled = false,
        isNotificationEnabled = false,
        isFromSettings = true,
        selectedColor = AppColors.Green,
        onNavigateForward = { user ->
            navController.throttledNavigate(TreeListRoute(userWallet = user.wallet, userName = "${user.firstName} ${user.lastName}"))
        },
    )
}