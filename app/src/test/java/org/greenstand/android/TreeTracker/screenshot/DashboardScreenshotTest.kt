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
package org.greenstand.android.TreeTracker.screenshot

import org.greenstand.android.TreeTracker.dashboard.Dashboard
import org.greenstand.android.TreeTracker.dashboard.DashboardState
import org.junit.Test

class DashboardScreenshotTest : ScreenshotTest() {

    @Test
    fun dashboard_default() = snapshot {
        Dashboard(state = DashboardState())
    }

    @Test
    fun dashboard_with_trees() = snapshot {
        Dashboard(
            state = DashboardState(
                treesRemainingToSync = 51,
                treesSynced = 146,
                totalTreesToSync = 200,
            ),
        )
    }

    @Test
    fun dashboard_with_notifications() = snapshot {
        Dashboard(
            state = DashboardState(
                treesRemainingToSync = 51,
                treesSynced = 146,
                totalTreesToSync = 200,
                showUnreadMessageNotification = true,
                isOrgButtonEnabled = true,
            ),
        )
    }
}
