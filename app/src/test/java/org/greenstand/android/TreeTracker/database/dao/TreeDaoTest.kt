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
package org.greenstand.android.TreeTracker.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.AppDatabase
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
class TreeDaoTest {
    private lateinit var treeDao: TreeDAO
    private lateinit var deviceConfigDao: DeviceConfigDAO
    private lateinit var sessionDao: SessionDAO
    private lateinit var planterDao: PlanterDAO
    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    AppDatabase::class.java,
                ).build()
        treeDao = database.treeDao()
        deviceConfigDao = database.deviceConfigDao()
        sessionDao = database.sessionDao()
        planterDao = database.planterDao()
    }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Entity, returns valid data, querying bundle Id`() =
        runTest {
            val deviceConfigId = deviceConfigDao.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
            FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId

            val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)
            val newSessionId = sessionDao.insertSession(newSession)
            newSession.id = newSessionId

            assertEquals(newSessionId, newSession.id)
            val newTree = FakeFileGenerator.fakeTree.first().copy(sessionId = newSessionId)
            val id = treeDao.insertTree(newTree)
            val getTreeEntity = treeDao.getTreesByIds(listOf(id))
            assertEquals("bundled", getTreeEntity.first().bundleId)
        }

    @Test
    @Throws(Exception::class)
    fun `update Tree Entity, assert fake tree not same as updated `() =
        runTest {
            val deviceConfigId = deviceConfigDao.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
            FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId

            val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)
            val newSessionId = sessionDao.insertSession(newSession)
            newSession.id = newSessionId

            assertEquals(newSessionId, newSession.id)
            val newTree = FakeFileGenerator.fakeTree.first().copy(sessionId = newSessionId)
            val fakeTree = treeDao.insertTree(newTree)
            val updated = newTree.copy(uuid = "testing")
            assertNotEquals(fakeTree, updated)
        }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Capture Entity, returns valid data querying UUID`() =
        runTest {
            val planterInfoId = planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            FakeFileGenerator.fakePlanterInfo.id = planterInfoId
            val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
            val planterCheckId = planterDao.insertPlanterCheckIn(newPlanterCheckIn)
            newPlanterCheckIn.id = planterCheckId
            val newTreeCapture = FakeFileGenerator.fakeTreeCapture.copy(planterCheckInId = planterCheckId)
            val id = treeDao.insertTreeCapture(newTreeCapture)
            val getTreeCapture = treeDao.getTreeCaptureById(id)
            assertEquals("uuid", getTreeCapture.uuid)
        }

    @Test
    @Throws(Exception::class)
    fun `update Tree Capture Entity, assert fake tree capture not same as updated `() =
        runTest {
            val planterInfoId = planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            FakeFileGenerator.fakePlanterInfo.id = planterInfoId
            val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
            val planterCheckId = planterDao.insertPlanterCheckIn(newPlanterCheckIn)
            newPlanterCheckIn.id = planterCheckId
            val newTreeCapture = FakeFileGenerator.fakeTreeCapture.copy(planterCheckInId = planterCheckId)
            val fakeCapture = treeDao.insertTreeCapture(newTreeCapture)
            val updated = newTreeCapture.copy(uuid = "testing")
            assertNotEquals(fakeCapture, updated)
        }

    @Test
    @Throws(Exception::class)
    fun `insert Tree Attribute Entity, returns valid with data `() =
        runTest {
            val planterInfoId = planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            FakeFileGenerator.fakePlanterInfo.id = planterInfoId
            val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
            val planterCheckId = planterDao.insertPlanterCheckIn(newPlanterCheckIn)
            newPlanterCheckIn.id = planterCheckId
            val newTreeCapture = FakeFileGenerator.fakeTreeCapture.copy(planterCheckInId = planterCheckId)
            val newTreeCaptureId = treeDao.insertTreeCapture(newTreeCapture)
            newTreeCapture.id = newTreeCaptureId
            val newTreeAttribute = FakeFileGenerator.fakeTreeAttribute.copy(treeCaptureId = newTreeCaptureId)
            treeDao.insertTreeAttribute(newTreeAttribute)
            val size = treeDao.getTreeAttributeByTreeCaptureId(newTreeCaptureId).size
            assertEquals(1, size)
        }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }
}