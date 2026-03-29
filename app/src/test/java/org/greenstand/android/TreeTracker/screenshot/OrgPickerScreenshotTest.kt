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

import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.orgpicker.OrgPicker
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerState
import org.junit.Test

class OrgPickerScreenshotTest : ScreenshotTest() {

    private val sampleOrgs = listOf(
        Org(
            id = "1",
            name = "Greenstand",
            walletId = "wallet-1",
            logoPath = "",
            captureSetupFlow = emptyList(),
            captureFlow = emptyList(),
        ),
        Org(
            id = "2",
            name = "EcoRestore",
            walletId = "wallet-2",
            logoPath = "",
            captureSetupFlow = emptyList(),
            captureFlow = emptyList(),
        ),
        Org(
            id = "3",
            name = "TreeFund",
            walletId = "wallet-3",
            logoPath = "",
            captureSetupFlow = emptyList(),
            captureFlow = emptyList(),
        ),
    )

    @Test
    fun orgPicker_default() = snapshot {
        OrgPicker(state = OrgPickerState())
    }

    @Test
    fun orgPicker_with_orgs() = snapshot {
        OrgPicker(
            state = OrgPickerState(
                orgs = sampleOrgs,
                currentOrg = sampleOrgs[0],
            ),
        )
    }
}
