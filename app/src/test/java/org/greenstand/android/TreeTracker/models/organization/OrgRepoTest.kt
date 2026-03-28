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
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    private val gson = Gson()

    private lateinit var mockEditor: Preferences.Editor

    private lateinit var orgRepo: OrgRepo

    private fun createOrgEntity(
        id: String,
        name: String,
        walletId: String = "wallet",
        captureSetupFlowJson: String = gson.toJson(listOf(Destination("user-select"))),
        captureFlowJson: String = gson.toJson(listOf(Destination("capture/{profilePicUrl}")))
    ): OrganizationEntity {
        return OrganizationEntity(
            id = id,
            version = 1,
            name = name,
            walletId = walletId,
            captureSetupFlowJson = captureSetupFlowJson,
            captureFlowJson = captureFlowJson,
        )
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockEditor = mockk(relaxed = true)
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { prefs.edit() } returns mockEditor
    }

    private fun createOrgRepo(): OrgRepo {
        return OrgRepo(dao = dao, prefs = prefs, gson = gson)
    }

    @Test
    fun `WHEN init called THEN inserts default org and loads current org from prefs`() = runTest {
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
    fun `WHEN init called with stored org not found THEN falls back to default`() = runTest {
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
    fun `WHEN getOrgs called THEN returns list of Org domain objects`() = runTest {
        val entities = listOf(
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
    fun `WHEN setOrg called THEN updates prefs and sets currentOrg`() = runTest {
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
    fun `WHEN setOrg called with nonexistent org THEN falls back to default`() = runTest {
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

    @Test
    fun `WHEN addOrgFromJsonString with malformed JSON THEN returns false`() = runTest {
        orgRepo = createOrgRepo()

        val result = orgRepo.addOrgFromJsonString("this is not valid json{{{")

        assertFalse(result)
    }
}
