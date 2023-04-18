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
package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.utils.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreeTrackerDaoTest {

    private lateinit var treeTrackerDAO: TreeTrackerDAO
    private lateinit var database: AppDatabase

    @Before
    fun createDb(){
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        treeTrackerDAO = database.treeTrackerDao()
    }

    @Test
    @Throws(Exception::class)
    fun `insert planterInfo to App Database, assert valid planter Info`() = runBlocking {
        treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        val planterInfo = treeTrackerDAO.getAllPlanterInfo().first().first()
        assertEquals(FakeFileGenerator.fakePlanterInfo, planterInfo)
    }

    @Test
    @Throws(Exception::class)
    fun `insert userInfo to App Database, assert valid User`() = runBlocking {
        treeTrackerDAO.insertUser(FakeFileGenerator.fakeUser)
        val userInfo = treeTrackerDAO.getAllUsers().first().first()
        assertEquals(FakeFileGenerator.fakeUser, userInfo)
    }

    @Test
    @Throws(Exception::class)
    fun `update user UUID, assert fake UUID not equal to updated user,`() = runBlocking {
        treeTrackerDAO.insertUser(FakeFileGenerator.fakeUser)
        val userUpdate = treeTrackerDAO.updateUser(FakeFileGenerator.fakeUser.copy(uuid = "newStringUpdate"))
        assertNotEquals(FakeFileGenerator.fakeUser.uuid, userUpdate)
    }

    @Test
    @Throws(Exception::class)
    fun `update user Bundle Id, assert fake Bundle, different from Updated,`() = runBlocking {
        treeTrackerDAO.insertUser(FakeFileGenerator.fakeUser)
        val userUpdate = treeTrackerDAO.updateUserBundleIds(listOf(12, 344), bundleId = "newString")
        assertNotEquals(FakeFileGenerator.fakeUser.bundleId, userUpdate)
    }

    @Test
    fun `insert org to App Database, returns valid org when querying id and name`() = runBlocking {
        treeTrackerDAO.insertOrg(FakeFileGenerator.fakeOrg.first())
        val organization = treeTrackerDAO.getOrg("new")
        assertEquals(organization?.id, FakeFileGenerator.fakeOrg.first().id)
        assertEquals(FakeFileGenerator.fakeOrg.first().name, organization?.name)
    }

    @Test
    @Throws(Exception::class)
    fun `saving org to App Database, returns valid data querying name`() = runBlocking {
        treeTrackerDAO.insertOrg(FakeFileGenerator.fakeOrg.first())
        val organization = treeTrackerDAO.getOrg("new")
        assertEquals("GreenStand",organization?.name)
    }

    @Test
    @Throws(Exception::class)
    fun `insert fake Device Config to App Database, returns valid data querying uuid`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.getLatestDeviceConfig()
        assertNotNull(deviceConfig)
        assertEquals(FakeFileGenerator.fakeDeviceConfig.uuid, deviceConfig?.uuid)
    }

    @Test
    @Throws(Exception::class)
    fun `updated device config upload status, assert fake upload status not same as updated status`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.updateDeviceConfigUploadStatus(ids = listOf(12, 12), isUploaded = true)
        assertNotEquals(FakeFileGenerator.fakeDeviceConfig.isUploaded, deviceConfig)
    }

    @Test
    @Throws(Exception::class)
    fun `updated device config bundle ID, assert fake bundleId not same as updated bundleID,`() = runBlocking {
        treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        val deviceConfig = treeTrackerDAO.updateDeviceConfigBundleIds(ids = listOf(15, 16),
            bundleId = "newRandomString")
        assertNotEquals(FakeFileGenerator.fakeDeviceConfig.bundleId, deviceConfig)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Session, returns valid querying UUID `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId
        assertEquals(deviceConfigId, FakeFileGenerator.fakeDeviceConfig.id)
        val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)

        val id = treeTrackerDAO.insertSession(newSession)
        val getSession = treeTrackerDAO.getSessionById(id)
        assertEquals("uuid", getSession.uuid)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Entity, returns valid data, querying bundle Id`() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId

        val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)
        val newSessionId = treeTrackerDAO.insertSession(newSession)
        newSession.id = newSessionId

        assertEquals(newSessionId, newSession.id)
        val newTree = FakeFileGenerator.fakeTree.first().copy(sessionId = newSessionId)
        val id = treeTrackerDAO.insertTree(newTree)
        val getTreeEntity = treeTrackerDAO.getTreesByIds(listOf(id))
        assertEquals("bundled", getTreeEntity.first().bundleId)
    }

    @Test
    @Throws(Exception::class)
    fun `update Tree Entity, assert fake tree not same as updated `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId

        val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)
        val newSessionId = treeTrackerDAO.insertSession(newSession)
        newSession.id = newSessionId

        assertEquals(newSessionId, newSession.id)
        val newTree = FakeFileGenerator.fakeTree.first().copy(sessionId = newSessionId)
        val fakeTree = treeTrackerDAO.insertTree(newTree)
        val updated = newTree.copy(uuid = "testing")
        assertNotEquals(fakeTree, updated)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Planter CheckIn Entity, returns valid data, querying local photo path `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        FakeFileGenerator.fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val id = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        val getPlanterCheck = treeTrackerDAO.getPlanterCheckInById(id)
        assertEquals("new", getPlanterCheck.localPhotoPath)
    }

    @Test
    @Throws(Exception::class)
    fun `update Planter CheckIn Entity, assert fake planter checkIn not same as updated`() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        FakeFileGenerator.fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val fakePlanter = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        val updated = newPlanterCheckIn.copy(latitude = 9888.11)
        assertNotEquals(fakePlanter, updated)
    }

    @Test
    @Throws(Exception::class)
    fun `delete Planter CheckIn Entity, assert null`() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        FakeFileGenerator.fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        treeTrackerDAO.deletePlanterCheckIn(newPlanterCheckIn)
        val planter = treeTrackerDAO.getPlanterCheckInById(newPlanterCheckIn.id)
        assertNull(planter)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Location Entity, returns valid with non null data `() = runBlocking{
        val deviceConfigId = treeTrackerDAO.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
        FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId

        val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)
        val newSessionId = treeTrackerDAO.insertSession(newSession)
        newSession.id = newSessionId

        assertEquals(newSessionId, newSession.id)
        val newLocation = FakeFileGenerator.fakeLocation.copy(sessionId = newSessionId)
        treeTrackerDAO.insertLocationData(newLocation)
        val getLocationEntity = treeTrackerDAO.getLocationData()
        assertEquals("location",getLocationEntity.first().locationDataJson)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Capture Entity, returns valid data querying UUID`() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        FakeFileGenerator.fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val planterCheckId = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        newPlanterCheckIn.id = planterCheckId
        val newTreeCapture = FakeFileGenerator.fakeTreeCapture.copy(planterCheckInId = planterCheckId)
        val id = treeTrackerDAO.insertTreeCapture(newTreeCapture)
        val getTreeCapture = treeTrackerDAO.getTreeCaptureById(id)
        assertEquals("uuid",getTreeCapture.uuid)
    }

    @Test
    @Throws(Exception::class)
    fun `update Tree Capture Entity, assert fake tree capture not same as updated `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        FakeFileGenerator.fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val planterCheckId = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        newPlanterCheckIn.id = planterCheckId
        val newTreeCapture = FakeFileGenerator.fakeTreeCapture.copy(planterCheckInId = planterCheckId)
        val fakeCapture = treeTrackerDAO.insertTreeCapture(newTreeCapture)
        val updated = newTreeCapture.copy(uuid = "testing")
        assertNotEquals(fakeCapture, updated)
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Attribute Entity, returns valid with data `() = runBlocking{
        val planterInfoId = treeTrackerDAO.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
        FakeFileGenerator.fakePlanterInfo.id = planterInfoId
        val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
        val planterCheckId = treeTrackerDAO.insertPlanterCheckIn(newPlanterCheckIn)
        newPlanterCheckIn.id = planterCheckId
        val newTreeCapture = FakeFileGenerator.fakeTreeCapture.copy(planterCheckInId = planterCheckId)
        val newTreeCaptureId = treeTrackerDAO.insertTreeCapture(newTreeCapture)
        newTreeCapture.id = newTreeCaptureId
        val newTreeAttribute = FakeFileGenerator.fakeTreeAttribute.copy(treeCaptureId = newTreeCaptureId)
        treeTrackerDAO.insertTreeAttribute(newTreeAttribute)
        val size = treeTrackerDAO.getTreeAttributeByTreeCaptureId(newTreeCaptureId).size
        assertEquals(1, size)
    }
    @After
        @Throws(IOException::class)
        fun tearDown(){
            database.close()
        }
    }
