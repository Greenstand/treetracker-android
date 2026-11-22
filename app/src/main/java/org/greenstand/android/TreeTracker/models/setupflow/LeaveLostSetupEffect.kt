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
package org.greenstand.android.TreeTracker.models.setupflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavigator

/**
 * Sends a setup step to the Dashboard once its setup is lost: it was restored after process death
 * and [CaptureSetupRestorer.loadUser] found no user to set up for. Pops back to the Dashboard, or
 * opens it if the restored back stack has none.
 */
@Composable
fun LeaveLostSetupEffect(setupLost: Boolean) {
    val navigator = LocalNavigator.current
    LaunchedEffect(setupLost) {
        if (setupLost && !navigator.popBackStackTo<DashboardRoute>()) {
            navigator.navigate(DashboardRoute) { popUpToRoot() }
        }
    }
}