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
package org.greenstand.android.TreeTracker.models.organization

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class OrgRepoTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var prefs: Preferences

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var mockEditor: Preferences.Editor

    private lateinit var orgRepo: OrgRepo

    private fun createOrgEntity(
        id: String,
        name: String,
        walletId: String = "wallet",
        captureSetupFlowJson: String = json.encodeToString(listOf(Destination("user-select"))),
        captureFlowJson: String = json.encodeToString(listOf(Destination("capture/{profilePicUrl}"))),
    ): OrganizationEntity =
        OrganizationEntity(
            id = id,
            version = 1,
            name = name,
            walletId = walletId,
            captureSetupFlowJson = captureSetupFlowJson,
            captureFlowJson = captureFlowJson,
        )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockEditor = mockk(relaxed = true)
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { prefs.edit() } returns mockEditor
    }

    private fun createOrgRepo(): OrgRepo = OrgRepo(dao = dao, prefs = prefs, json = json)

    @Test
    fun `WHEN init called THEN inserts default org and loads current org from prefs`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg

            orgRepo = createOrgRepo()
            orgRepo.init()

            coVerify { dao.insertOrg(any()) }
            coVerify { dao.getOrg("-1") }
            val currentOrg = orgRepo.currentOrg()
            assertEquals("Greenstand", currentOrg.name)
        }

    @Test
    fun `WHEN init called with stored org not found THEN falls back to default`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "nonexistent-id"
            coEvery { dao.getOrg("nonexistent-id") } returns null
            coEvery { dao.getOrg("-1") } returns defaultOrg

            orgRepo = createOrgRepo()
            orgRepo.init()

            val currentOrg = orgRepo.currentOrg()
            assertEquals("Greenstand", currentOrg.name)
        }

    @Test
    fun `WHEN getOrgs called THEN returns list of Org domain objects`() =
        runTest {
            val entities =
                listOf(
                    createOrgEntity(id = "1", name = "Org1"),
                    createOrgEntity(id = "2", name = "Org2"),
                )
            coEvery { dao.getAllOrgs() } returns entities

            orgRepo = createOrgRepo()
            val result = orgRepo.getOrgs()

            assertEquals(2, result.size)
            assertEquals("Org1", result[0].name)
            assertEquals("Org2", result[1].name)
        }

    @Test
    fun `WHEN setOrg called THEN updates prefs and sets currentOrg`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            val targetOrg = createOrgEntity(id = "42", name = "TargetOrg")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("42") } returns targetOrg

            orgRepo = createOrgRepo()
            orgRepo.init()
            orgRepo.setOrg("42")

            verify { mockEditor.putString(any(), "42") }
            verify { mockEditor.commit() }
            assertEquals("TargetOrg", orgRepo.currentOrg().name)
        }

    @Test
    fun `WHEN setOrg called with nonexistent org THEN falls back to default`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("missing") } returns null

            orgRepo = createOrgRepo()
            orgRepo.init()
            orgRepo.setOrg("missing")

            assertEquals("Greenstand", orgRepo.currentOrg().name)
        }

    @Test(expected = IllegalStateException::class)
    fun `WHEN currentOrg called before init THEN throws`() {
        orgRepo = createOrgRepo()
        orgRepo.currentOrg()
    }

    // --- addOrgFromRemoteConfig tests ---

    @Test
    fun `WHEN addOrgFromRemoteConfig with valid JSON THEN parses all fields and inserts org`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("rc-org") } returns createOrgEntity(id = "rc-org", name = "Remote Org", walletId = "rc-wallet")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val configJson = """
                {
                    "version": "2",
                    "walletId": "rc-wallet",
                    "captureSetupFlow": [{"route": "user-select"}],
                    "captureFlow": [{"route": "capture/{profilePicUrl}"}]
                }
            """.trimIndent()

            val result = orgRepo.addOrgFromRemoteConfig("rc-org", "Remote Org", configJson)

            assertTrue(result)
            coVerify {
                dao.insertOrg(
                    match {
                        it.id == "rc-org" &&
                            it.name == "Remote Org" &&
                            it.walletId == "rc-wallet" &&
                            it.version == 2
                    },
                )
            }
        }

    @Test
    fun `WHEN addOrgFromRemoteConfig with missing walletId THEN defaults to empty`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("no-wallet") } returns createOrgEntity(id = "no-wallet", name = "NoWallet")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val configJson = """
                {
                    "version": "1",
                    "captureSetupFlow": [{"route": "user-select"}],
                    "captureFlow": [{"route": "capture/{profilePicUrl}"}]
                }
            """.trimIndent()

            val result = orgRepo.addOrgFromRemoteConfig("no-wallet", "NoWallet", configJson)

            assertTrue(result)
            coVerify { dao.insertOrg(match { it.walletId == "" }) }
        }

    @Test
    fun `WHEN addOrgFromRemoteConfig with invalid routes THEN filters them out`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("bad-routes") } returns createOrgEntity(id = "bad-routes", name = "BadRoutes")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val configJson = """
                {
                    "version": "1",
                    "walletId": "",
                    "captureSetupFlow": [{"route": "user-select"}, {"route": "nonexistent-screen"}],
                    "captureFlow": [{"route": "capture/{profilePicUrl}"}, {"route": "fake-route"}]
                }
            """.trimIndent()

            val result = orgRepo.addOrgFromRemoteConfig("bad-routes", "BadRoutes", configJson)

            assertTrue(result)
            coVerify {
                dao.insertOrg(
                    match {
                        // Invalid routes should be stripped; only valid ones remain
                        !it.captureSetupFlowJson.contains("nonexistent-screen") &&
                            it.captureSetupFlowJson.contains("user-select") &&
                            !it.captureFlowJson.contains("fake-route") &&
                            it.captureFlowJson.contains("capture/{profilePicUrl}")
                    },
                )
            }
        }

    @Test
    fun `WHEN addOrgFromRemoteConfig with malformed JSON THEN falls back to addMinimalOrg`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("test-id") } returns createOrgEntity(id = "test-id", name = "TestOrg")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val result = orgRepo.addOrgFromRemoteConfig("test-id", "TestOrg", "this is not valid json{{{")

            // Falls back to addMinimalOrg which still inserts with the correct id/name
            coVerify { dao.insertOrg(match { it.id == "test-id" && it.name == "TestOrg" }) }
        }

    @Test
    fun `WHEN addOrgFromRemoteConfig with features THEN preserves feature flags`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("feat-org") } returns createOrgEntity(id = "feat-org", name = "FeatOrg")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val configJson = """
                {
                    "version": "1",
                    "walletId": "w",
                    "captureSetupFlow": [{"route": "user-select"}],
                    "captureFlow": [{"route": "tree-image-review/{photoPath}", "features": ["forceNote"]}]
                }
            """.trimIndent()

            val result = orgRepo.addOrgFromRemoteConfig("feat-org", "FeatOrg", configJson)

            assertTrue(result)
            coVerify {
                dao.insertOrg(match { it.captureFlowJson.contains("forceNote") })
            }
        }

    // --- addMinimalOrg tests ---

    @Test
    fun `WHEN addMinimalOrg called THEN creates org with default flows and empty wallet`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("minimal-id") } returns createOrgEntity(id = "minimal-id", name = "MinimalOrg")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val result = orgRepo.addMinimalOrg("minimal-id", "MinimalOrg")

            assertTrue(result)
            coVerify {
                dao.insertOrg(
                    match {
                        it.id == "minimal-id" &&
                            it.name == "MinimalOrg" &&
                            it.walletId == "" &&
                            it.captureSetupFlowJson.contains("user-select") &&
                            it.captureFlowJson.contains("capture/{profilePicUrl}") &&
                            it.captureFlowJson.contains("tree-image-review/{photoPath}")
                    },
                )
            }
        }

    @Test
    fun `WHEN addMinimalOrg called THEN sets org as current`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.getOrg("set-current") } returns createOrgEntity(id = "set-current", name = "SetCurrent")

            orgRepo = createOrgRepo()
            orgRepo.init()
            orgRepo.addMinimalOrg("set-current", "SetCurrent")

            verify { mockEditor.putString(any(), "set-current") }
            assertEquals("SetCurrent", orgRepo.currentOrg().name)
        }

    @Test
    fun `WHEN addMinimalOrg and dao throws THEN returns false`() =
        runTest {
            val defaultOrg = createOrgEntity(id = "-1", name = "Greenstand")
            every { prefs.getString(any(), any()) } returns "-1"
            coEvery { dao.getOrg("-1") } returns defaultOrg
            coEvery { dao.insertOrg(match { it.id == "err-id" }) } throws RuntimeException("DB error")

            orgRepo = createOrgRepo()
            orgRepo.init()

            val result = orgRepo.addMinimalOrg("err-id", "ErrOrg")

            assertFalse(result)
        }
}