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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager

/** Setup steps after user select: their ViewModels read the selected user when they are created. */
private val SETUP_STEPS_NEEDING_USER: Set<NavKey> =
    setOf(WalletSelectRoute, AddWalletRoute, AddOrgRoute, SessionNoteRoute)

/**
 * Takes the setup steps that need the selected user off the top of a restored back stack when
 * capture setup has no user: the setup scope was not restored after process death, or its user
 * is no longer in the database. Showing such a step would crash, so the user lands on the screen
 * below it instead (user select, which starts setup again, or the Dashboard).
 *
 * Changes nothing in the normal case, where the setup scope is open with a user.
 */
fun MutableList<NavKey>.dropSetupStepsWithoutUser(
    hasSetupUser: Boolean = CaptureSetupScopeManager.isOpen && CaptureSetupScopeManager.getData().user != null,
) {
    if (hasSetupUser) return
    while (size > 1 && last() in SETUP_STEPS_NEEDING_USER) {
        removeAt(lastIndex)
    }
}