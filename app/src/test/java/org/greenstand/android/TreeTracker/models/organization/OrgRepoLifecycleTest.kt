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

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the default org, init/ensureInitialized, org switching and persistence of the
 * selected org, using real [Preferences] and an in-memory org table.
 */
@RunWith(RobolectricTestRunner::class)
class OrgRepoLifecycleTest {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var prefs: Preferences
    private lateinit var store: FakeOrgStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPrefs = context.getSharedPreferences("org-repo-lifecycle-test", Context.MODE_PRIVATE)
        prefs = Preferences(sharedPrefs)
        store = FakeOrgStore()
    }

    private fun createRepo(): OrgRepo = OrgRepo(dao = store.dao, prefs = prefs, json = orgTestJson)

    private fun persistCurrentOrgId(orgId: String) {
        prefs.edit().putString(CURRENT_ORG_KEY, orgId).commit()
    }

    @Test
    fun `init inserts the Greenstand default org`() =
        runTest {
            createRepo().init()

            val defaultEntity = store.orgs.getValue(OrgRepo.DEFAULT_ORG_ID)
            assertEquals("Greenstand", defaultEntity.name)
            assertEquals(1, defaultEntity.version)
            assertEquals("", defaultEntity.walletId)
        }

    @Test
    fun `default org uses user select then add org as setup flow`() =
        runTest {
            val repo = createRepo()
            repo.init()

            assertEquals(
                listOf(
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                    Destination(RouteRegistry.ROUTE_ADD_ORG),
                ),
                repo.currentOrg().captureSetupFlow,
            )
        }

    @Test
    fun `default org uses capture then image review as capture flow without features`() =
        runTest {
            val repo = createRepo()
            repo.init()

            assertEquals(
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
                ),
                repo.currentOrg().captureFlow,
            )
        }

    @Test
    fun `init with no stored org selects the default org`() =
        runTest {
            val repo = createRepo()
            repo.init()

            val org = repo.currentOrg()
            assertEquals(OrgRepo.DEFAULT_ORG_ID, org.id)
            assertEquals("Greenstand", org.name)
            assertEquals("", org.walletId)
            assertEquals("", org.logoPath)
        }

    @Test
    fun `init restores the org stored in preferences`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme", walletId = "acme-wallet"))
            persistCurrentOrgId("42")

            val repo = createRepo()
            repo.init()

            val org = repo.currentOrg()
            assertEquals("42", org.id)
            assertEquals("Acme", org.name)
            assertEquals("acme-wallet", org.walletId)
        }

    @Test
    fun `init falls back to the default org when the stored org was removed`() =
        runTest {
            persistCurrentOrgId("gone")

            val repo = createRepo()
            repo.init()

            assertEquals(OrgRepo.DEFAULT_ORG_ID, repo.currentOrg().id)
        }

    @Test
    fun `init still inserts the default org when another org is stored`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))
            persistCurrentOrgId("42")

            createRepo().init()

            assertTrue(store.orgs.containsKey(OrgRepo.DEFAULT_ORG_ID))
        }

    @Test(expected = IllegalStateException::class)
    fun `currentOrg throws when neither the stored nor the default org can be loaded`() =
        runTest {
            coEvery { store.dao.getOrg(any()) } returns null
            val repo = createRepo()
            repo.init()

            repo.currentOrg()
        }

    @Test
    fun `ensureInitialized loads the persisted org when the repo was never initialized`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))
            persistCurrentOrgId("42")
            val repo = createRepo()

            repo.ensureInitialized()

            assertEquals("Acme", repo.currentOrg().name)
        }

    @Test
    fun `ensureInitialized is a no-op once the repo is initialized`() =
        runTest {
            val repo = createRepo()
            repo.init()
            repo.ensureInitialized()
            repo.ensureInitialized()

            assertEquals(1, store.inserted.size)
        }

    @Test
    fun `ensureInitialized keeps an org selected with setOrg`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))
            val repo = createRepo()
            repo.setOrg("42")

            repo.ensureInitialized()

            assertEquals("Acme", repo.currentOrg().name)
            assertFalse(store.orgs.containsKey(OrgRepo.DEFAULT_ORG_ID))
        }

    @Test
    fun `ensureInitialized retries init when the previous init could not load an org`() =
        runTest {
            coEvery { store.dao.getOrg(any()) } returns null
            val repo = createRepo()
            repo.init()

            repo.ensureInitialized()

            assertEquals(2, store.inserted.size)
        }

    @Test
    fun `setOrg stores the org id under the system settings current-org key`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))

            createRepo().setOrg("42")

            assertEquals("42", sharedPrefs.getString("greenstand/system-settings/current-org", null))
        }

    @Test
    fun `setOrg works without init`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))
            val repo = createRepo()

            repo.setOrg("42")

            assertEquals("42", repo.currentOrg().id)
        }

    @Test
    fun `switching orgs replaces the current org each time`() =
        runTest {
            store.put(orgEntity(id = "1", name = "First"))
            store.put(orgEntity(id = "2", name = "Second"))
            val repo = createRepo()
            repo.init()

            repo.setOrg("1")
            assertEquals("First", repo.currentOrg().name)
            repo.setOrg("2")
            assertEquals("Second", repo.currentOrg().name)
            repo.setOrg(OrgRepo.DEFAULT_ORG_ID)
            assertEquals("Greenstand", repo.currentOrg().name)
        }

    @Test
    fun `selected org survives a new repo instance`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))
            val firstRepo = createRepo()
            firstRepo.init()
            firstRepo.setOrg("42")

            val restartedRepo = createRepo()
            restartedRepo.init()

            assertEquals("Acme", restartedRepo.currentOrg().name)
        }

    @Test
    fun `setOrg with an unknown id falls back to the default org but still persists the id`() =
        runTest {
            val repo = createRepo()
            repo.init()

            repo.setOrg("unknown")

            assertEquals(OrgRepo.DEFAULT_ORG_ID, repo.currentOrg().id)
            assertEquals("unknown", sharedPrefs.getString("greenstand/system-settings/current-org", null))
        }

    @Test(expected = IllegalStateException::class)
    fun `setOrg with an unknown id before the default org exists leaves no current org`() =
        runTest {
            val repo = createRepo()
            repo.setOrg("unknown")

            repo.currentOrg()
        }

    @Test
    fun `currentOrg maps flows and features from the stored entity`() =
        runTest {
            val setupFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                    Destination(RouteRegistry.ROUTE_SESSION_NOTE, listOf(OrgFeature.FORCE_NOTE.key)),
                )
            val captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                    Destination(RouteRegistry.ROUTE_TREE_HEIGHT, emptyList()),
                )
            store.put(orgEntity(id = "42", name = "Acme", captureSetupFlow = setupFlow, captureFlow = captureFlow))
            val repo = createRepo()

            repo.setOrg("42")

            assertEquals(setupFlow, repo.currentOrg().captureSetupFlow)
            assertEquals(captureFlow, repo.currentOrg().captureFlow)
        }

    @Test
    fun `getOrgs returns an empty list when no org is stored`() =
        runTest {
            assertTrue(createRepo().getOrgs().isEmpty())
        }

    @Test
    fun `getOrgs includes the default org after init`() =
        runTest {
            store.put(orgEntity(id = "42", name = "Acme"))
            val repo = createRepo()
            repo.init()

            val orgs = repo.getOrgs()

            assertEquals(setOf("42", OrgRepo.DEFAULT_ORG_ID), orgs.map { it.id }.toSet())
        }

    @Test
    fun `getOrgs maps every stored entity to an org with an empty logo path`() =
        runTest {
            store.put(orgEntity(id = "1", name = "First", walletId = "w1"))
            store.put(orgEntity(id = "2", name = "Second", walletId = "w2"))

            val orgs = createRepo().getOrgs()

            assertEquals(listOf("First", "Second"), orgs.map { it.name })
            assertEquals(listOf("w1", "w2"), orgs.map { it.walletId })
            assertTrue(orgs.all { it.logoPath.isEmpty() })
        }

    private companion object {
        val CURRENT_ORG_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("current-org")
    }
}