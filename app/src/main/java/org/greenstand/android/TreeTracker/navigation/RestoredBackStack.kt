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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey

/** Setup screens that read the capture setup scope as soon as they are shown. */
private val SETUP_SCREENS_NEEDING_SCOPE: Set<NavKey> =
    setOf(AddOrgRoute, WalletSelectRoute, AddWalletRoute, SessionNoteRoute)

/**
 * Pops setup screens off the top of a restored back stack when the capture setup they belong to
 * could not be restored ([isSetupOpen] is false), so the user lands on the screen under them
 * (user select, which starts a new setup) and not on a screen that would crash (#1418).
 * Never pops the last entry.
 */
fun MutableList<NavKey>.dropSetupScreensWithoutSetup(isSetupOpen: Boolean) {
    if (isSetupOpen) return
    while (size > 1 && last() in SETUP_SCREENS_NEEDING_SCOPE) {
        removeAt(lastIndex)
    }
}
