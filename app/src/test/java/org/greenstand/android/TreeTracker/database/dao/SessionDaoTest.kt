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
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SessionDaoTest {
    private lateinit var deviceConfigDao: DeviceConfigDAO
    private lateinit var sessionDao: SessionDAO
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
        sessionDao = database.sessionDao()
        deviceConfigDao = database.deviceConfigDao()
    }

    @Test
    @Throws(Exception::class)
    fun `insert Session, returns valid querying UUID `() =
        runTest {
            val deviceConfigId = deviceConfigDao.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
            FakeFileGenerator.fakeDeviceConfig.id = deviceConfigId
            assertEquals(deviceConfigId, FakeFileGenerator.fakeDeviceConfig.id)
            val newSession = FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId)

            val id = sessionDao.insertSession(newSession)
            val getSession = sessionDao.getSessionById(id)
            assertEquals("uuid", getSession.uuid)
        }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }
}