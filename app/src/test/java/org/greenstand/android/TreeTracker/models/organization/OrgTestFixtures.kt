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
package org.greenstand.android.TreeTracker.models.organization

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.navigation.RouteRegistry

/** Same configuration as the app's Koin `Json` single. */
internal val orgTestJson =
    Json {
        explicitNulls = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/**
 * A [TreeTrackerDAO] whose org queries are backed by an in-memory table with
 * Room's REPLACE-on-conflict semantics. Every other DAO call is relaxed.
 */
internal class FakeOrgStore {
    val orgs = linkedMapOf<String, OrganizationEntity>()
    val inserted = mutableListOf<OrganizationEntity>()
    val dao: TreeTrackerDAO = mockk(relaxed = true)

    init {
        coEvery { dao.insertOrg(any()) } answers {
            val entity = firstArg<OrganizationEntity>()
            inserted.add(entity)
            orgs[entity.id] = entity
            1L
        }
        coEvery { dao.getOrg(any()) } answers {
            firstArg<String?>()?.let { id -> orgs[id] }
        }
        coEvery { dao.getAllOrgs() } answers { orgs.values.toList() }
    }

    fun put(entity: OrganizationEntity) {
        orgs[entity.id] = entity
    }
}

internal fun orgEntity(
    id: String,
    name: String,
    walletId: String = "wallet-$id",
    version: Int = 1,
    captureSetupFlow: List<Destination> = listOf(Destination(RouteRegistry.ROUTE_USER_SELECT)),
    captureFlow: List<Destination> = listOf(Destination(RouteRegistry.ROUTE_TREE_CAPTURE)),
): OrganizationEntity =
    OrganizationEntity(
        id = id,
        version = version,
        name = name,
        walletId = walletId,
        captureSetupFlowJson = orgTestJson.encodeToString(captureSetupFlow),
        captureFlowJson = orgTestJson.encodeToString(captureFlow),
    )