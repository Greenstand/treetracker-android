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
package org.greenstand.android.TreeTracker.devoptions

import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.models.organization.OrgOverrideStore
import org.greenstand.android.TreeTracker.models.organization.OrgRepo

/** An [OrgRepo] with no current org, so dev options hides the org link overrides section. */
internal fun orgRepoWithoutOrg(): OrgRepo =
    mockk {
        every { baseCurrentOrg() } throws IllegalStateException("OrgRepo not initialized")
    }

/** An [OrgOverrideStore] for tests that don't look at org link overrides. */
internal fun unusedOrgOverrideStore(): OrgOverrideStore = mockk(relaxed = true)