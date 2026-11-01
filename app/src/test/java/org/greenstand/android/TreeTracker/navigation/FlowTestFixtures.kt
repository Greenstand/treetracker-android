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

import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo

/** Shortest gap between two throttled navigations on an unscoped [Navigator]. */
internal const val THROTTLE_WINDOW_MS = 300L

internal fun destinations(vararg routes: String): List<Destination> = routes.map { Destination(it) }

/** An [OrgRepo] mock whose current org has the given setup and capture flows. */
internal fun orgRepoWith(
    setupFlow: List<Destination> = emptyList(),
    captureFlow: List<Destination> = emptyList(),
): OrgRepo {
    val orgRepo = mockk<OrgRepo>()
    every { orgRepo.currentOrg() } returns
        Org(
            id = "org-1",
            name = "Test Org",
            walletId = "org-wallet",
            logoPath = "",
            captureSetupFlow = setupFlow,
            captureFlow = captureFlow,
        )
    return orgRepo
}