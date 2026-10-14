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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlanterDaoTest {
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
        planterDao = database.planterDao()
    }

    @Test
    @Throws(Exception::class)
    fun `insert planterInfo to App Database, assert valid planter Info`() =
        runTest {
            planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            val planterInfo = planterDao.getAllPlanterInfo().first().first()
            assertEquals(FakeFileGenerator.fakePlanterInfo, planterInfo)
        }

    @Test
    @Throws(Exception::class)
    fun `insert Planter CheckIn Entity, returns valid data, querying local photo path `() =
        runTest {
            val planterInfoId = planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            FakeFileGenerator.fakePlanterInfo.id = planterInfoId
            val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
            val id = planterDao.insertPlanterCheckIn(newPlanterCheckIn)
            val getPlanterCheck = planterDao.getPlanterCheckInById(id)
            assertEquals("new", getPlanterCheck?.localPhotoPath)
        }

    @Test
    @Throws(Exception::class)
    fun `update Planter CheckIn Entity, assert fake planter checkIn not same as updated`() =
        runTest {
            val planterInfoId = planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            FakeFileGenerator.fakePlanterInfo.id = planterInfoId
            val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
            val fakePlanter = planterDao.insertPlanterCheckIn(newPlanterCheckIn)
            val updated = newPlanterCheckIn.copy(latitude = 9888.11)
            assertNotEquals(fakePlanter, updated)
        }

    @Test
    @Throws(Exception::class)
    fun `delete Planter CheckIn Entity, assert null`() =
        runTest {
            val planterInfoId = planterDao.insertPlanterInfo(FakeFileGenerator.fakePlanterInfo)
            FakeFileGenerator.fakePlanterInfo.id = planterInfoId
            val newPlanterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy(planterInfoId = planterInfoId)
            planterDao.deletePlanterCheckIn(newPlanterCheckIn)
            val planter = planterDao.getPlanterCheckInById(newPlanterCheckIn.id)
            assertNull(planter)
        }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }
}