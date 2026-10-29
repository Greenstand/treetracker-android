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
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers parsing org JSON from Remote Config (valid, partial, malformed) and the
 * minimal-org fallback, asserting on what ends up stored and selected.
 */
@RunWith(RobolectricTestRunner::class)
class OrgRepoRemoteConfigTest {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var store: FakeOrgStore
    private lateinit var repo: OrgRepo

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPrefs = context.getSharedPreferences("org-repo-remote-config-test", Context.MODE_PRIVATE)
        store = FakeOrgStore()
        repo = OrgRepo(dao = store.dao, prefs = Preferences(sharedPrefs), json = orgTestJson)
    }

    private fun storedSetupFlow(orgId: String): List<Destination> = orgTestJson.decodeFromString(store.orgs.getValue(orgId).captureSetupFlowJson)

    private fun storedCaptureFlow(orgId: String): List<Destination> = orgTestJson.decodeFromString(store.orgs.getValue(orgId).captureFlowJson)

    private fun assertIsMinimalOrg(
        orgId: String,
        orgName: String,
    ) {
        val entity = store.orgs.getValue(orgId)
        assertEquals(orgName, entity.name)
        assertEquals(1, entity.version)
        assertEquals("", entity.walletId)
        assertEquals(MINIMAL_SETUP_FLOW, storedSetupFlow(orgId))
        assertEquals(MINIMAL_CAPTURE_FLOW, storedCaptureFlow(orgId))
    }

    @Test
    fun `valid config is stored with its wallet, version and flows`() =
        runTest {
            val config =
                """
                {
                  "version": "3",
                  "walletId": "acme-wallet",
                  "captureSetupFlow": [{"route": "user-select"}, {"route": "session-note"}],
                  "captureFlow": [
                    {"route": "capture/{profilePicUrl}"},
                    {"route": "tree-image-review/{photoPath}", "features": ["forceNote"]},
                    {"route": "tree-height-selection"}
                  ]
                }
                """.trimIndent()

            val added = repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertTrue(added)
            val entity = store.orgs.getValue("42")
            assertEquals("Acme", entity.name)
            assertEquals(3, entity.version)
            assertEquals("acme-wallet", entity.walletId)
            assertEquals(
                listOf(
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                    Destination(RouteRegistry.ROUTE_SESSION_NOTE),
                ),
                storedSetupFlow("42"),
            )
            assertEquals(
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                    Destination(RouteRegistry.ROUTE_TREE_HEIGHT),
                ),
                storedCaptureFlow("42"),
            )
        }

    @Test
    fun `valid config becomes the current org with its flows`() =
        runTest {
            val config =
                """{"walletId":"w","captureSetupFlow":[{"route":"user-select"}],""" +
                    """"captureFlow":[{"route":"tree-image-review/{photoPath}","features":["forceNote"]}]}"""

            repo.addOrgFromRemoteConfig("42", "Acme", config)

            val org = repo.currentOrg()
            assertEquals("42", org.id)
            assertEquals("Acme", org.name)
            assertEquals("w", org.walletId)
            assertEquals(listOf(Destination(RouteRegistry.ROUTE_USER_SELECT)), org.captureSetupFlow)
            assertEquals(
                listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key))),
                org.captureFlow,
            )
            assertEquals("42", sharedPrefs.getString("greenstand/system-settings/current-org", null))
        }

    @Test
    fun `org id and name come from the link not from the config body`() =
        runTest {
            val config = """{"id":"999","name":"Other","captureSetupFlow":[],"captureFlow":[]}"""

            repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertFalse(store.orgs.containsKey("999"))
            assertEquals("Acme", store.orgs.getValue("42").name)
        }

    @Test
    fun `missing version defaults to 1`() =
        runTest {
            repo.addOrgFromRemoteConfig("42", "Acme", """{"captureSetupFlow":[],"captureFlow":[]}""")

            assertEquals(1, store.orgs.getValue("42").version)
        }

    @Test
    fun `non numeric version defaults to 1`() =
        runTest {
            repo.addOrgFromRemoteConfig("42", "Acme", """{"version":"v2","captureSetupFlow":[],"captureFlow":[]}""")

            assertEquals(1, store.orgs.getValue("42").version)
        }

    @Test
    fun `numeric json version is accepted`() =
        runTest {
            repo.addOrgFromRemoteConfig("42", "Acme", """{"version":5,"captureSetupFlow":[],"captureFlow":[]}""")

            assertEquals(5, store.orgs.getValue("42").version)
        }

    @Test
    fun `missing wallet id is stored as empty`() =
        runTest {
            repo.addOrgFromRemoteConfig("42", "Acme", """{"captureSetupFlow":[],"captureFlow":[]}""")

            assertEquals("", store.orgs.getValue("42").walletId)
        }

    @Test
    fun `empty flows are kept empty`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"captureSetupFlow":[],"captureFlow":[]}""")

            assertTrue(added)
            assertTrue(repo.currentOrg().captureSetupFlow.isEmpty())
            assertTrue(repo.currentOrg().captureFlow.isEmpty())
        }

    @Test
    fun `unknown routes are dropped and known routes keep their order`() =
        runTest {
            val config =
                """{"captureSetupFlow":[{"route":"bogus"},{"route":"session-note"},{"route":"user-select"}],""" +
                    """"captureFlow":[{"route":"tree-height-selection"},{"route":"nope"},{"route":"capture/{profilePicUrl}"}]}"""

            repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertEquals(
                listOf(
                    Destination(RouteRegistry.ROUTE_SESSION_NOTE),
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                ),
                storedSetupFlow("42"),
            )
            assertEquals(
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_HEIGHT),
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                ),
                storedCaptureFlow("42"),
            )
        }

    @Test
    fun `a config whose routes are all unknown is stored with empty flows`() =
        runTest {
            val config = """{"captureSetupFlow":[{"route":"x"}],"captureFlow":[{"route":"y"}]}"""

            val added = repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertTrue(added)
            assertTrue(storedSetupFlow("42").isEmpty())
            assertTrue(storedCaptureFlow("42").isEmpty())
        }

    @Test
    fun `route aliases are accepted and stored as written`() =
        runTest {
            val config = """{"captureSetupFlow":[],"captureFlow":[{"route":"tree-capture"},{"route":"image-review"}]}"""

            repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertEquals(
                listOf(Destination("tree-capture"), Destination("image-review")),
                storedCaptureFlow("42"),
            )
        }

    @Test
    fun `unknown destination fields are ignored`() =
        runTest {
            val config = """{"captureSetupFlow":[{"route":"user-select","color":"green"}],"captureFlow":[]}"""

            val added = repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertTrue(added)
            assertEquals(listOf(Destination(RouteRegistry.ROUTE_USER_SELECT)), storedSetupFlow("42"))
        }

    @Test
    fun `unknown feature keys are kept in the stored flow`() =
        runTest {
            val config =
                """{"captureSetupFlow":[],"captureFlow":[{"route":"tree-image-review/{photoPath}","features":["forceNote","later"]}]}"""

            repo.addOrgFromRemoteConfig("42", "Acme", config)

            assertEquals(
                listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf("forceNote", "later"))),
                storedCaptureFlow("42"),
            )
        }

    @Test
    fun `adding an org again replaces its previous config`() =
        runTest {
            repo.addOrgFromRemoteConfig("42", "Acme", """{"walletId":"old","captureSetupFlow":[],"captureFlow":[]}""")
            repo.addOrgFromRemoteConfig("42", "Acme Trees", """{"walletId":"new","captureSetupFlow":[],"captureFlow":[]}""")

            assertEquals("Acme Trees", store.orgs.getValue("42").name)
            assertEquals("new", repo.currentOrg().walletId)
        }

    @Test
    fun `missing capture flow falls back to the minimal org`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"walletId":"w","captureSetupFlow":[]}""")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `missing setup flow falls back to the minimal org`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"walletId":"w","captureFlow":[]}""")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `a flow that is not an array falls back to the minimal org`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"captureSetupFlow":"user-select","captureFlow":[]}""")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `a destination without a route falls back to the minimal org`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"captureSetupFlow":[{"features":[]}],"captureFlow":[]}""")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `a json array instead of an object falls back to the minimal org`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", """[{"route":"user-select"}]""")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `an empty config falls back to the minimal org`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", "")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `malformed json falls back to the minimal org and selects it`() =
        runTest {
            val added = repo.addOrgFromRemoteConfig("42", "Acme", "{not json")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
            assertEquals("42", repo.currentOrg().id)
        }

    @Test
    fun `a failed insert of the parsed org falls back to the minimal org`() =
        runTest {
            coEvery { store.dao.insertOrg(match { it.walletId == "w" }) } throws IllegalStateException("db")

            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"walletId":"w","captureSetupFlow":[],"captureFlow":[]}""")

            assertTrue(added)
            assertIsMinimalOrg("42", "Acme")
        }

    @Test
    fun `returns false when both the parsed and the minimal org fail to insert`() =
        runTest {
            coEvery { store.dao.insertOrg(any()) } throws IllegalStateException("db")

            val added = repo.addOrgFromRemoteConfig("42", "Acme", """{"captureSetupFlow":[],"captureFlow":[]}""")

            assertFalse(added)
            assertTrue(store.orgs.isEmpty())
        }

    @Test
    fun `addMinimalOrg stores the minimal flows and selects the org`() =
        runTest {
            val added = repo.addMinimalOrg("7", "Seven")

            assertTrue(added)
            assertIsMinimalOrg("7", "Seven")
            assertEquals("Seven", repo.currentOrg().name)
            assertEquals(MINIMAL_SETUP_FLOW, repo.currentOrg().captureSetupFlow)
            assertEquals(MINIMAL_CAPTURE_FLOW, repo.currentOrg().captureFlow)
        }

    @Test
    fun `addMinimalOrg accepts an empty org name`() =
        runTest {
            val added = repo.addMinimalOrg("7", "")

            assertTrue(added)
            assertEquals("", repo.currentOrg().name)
        }

    @Test
    fun `addMinimalOrg returns false and keeps the selection when the insert fails`() =
        runTest {
            coEvery { store.dao.insertOrg(match { it.id == "7" }) } throws IllegalStateException("db")
            repo.init()

            val added = repo.addMinimalOrg("7", "Seven")

            assertFalse(added)
            assertEquals(OrgRepo.DEFAULT_ORG_ID, repo.currentOrg().id)
            assertNull(sharedPrefs.getString("greenstand/system-settings/current-org", null))
        }

    private companion object {
        val MINIMAL_SETUP_FLOW = listOf(Destination(RouteRegistry.ROUTE_USER_SELECT))
        val MINIMAL_CAPTURE_FLOW =
            listOf(
                Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
            )
    }
}