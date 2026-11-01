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
package org.greenstand.android.TreeTracker.database.dao

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DeviceConfigDaoTest : InMemoryDatabaseTest() {
    private val dao: DeviceConfigDAO get() = database.deviceConfigDao()

    @Test
    fun `insertDeviceConfig stores every field and returns the generated id`() =
        runTest {
            val config = DbFixtures.deviceConfig(bundleId = "bundle", isUploaded = true)

            val id = dao.insertDeviceConfig(config)

            val stored = dao.getDeviceConfigById(id)
            assertEquals(config, stored)
            assertEquals(id, stored?.id)
        }

    @Test
    fun `getDeviceConfigById returns null for an unknown id`() =
        runTest {
            assertNull(dao.getDeviceConfigById(7))
        }

    @Test
    fun `insertDeviceConfig with an existing id replaces the row`() =
        runTest {
            val id = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "old"))

            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "new").apply { this.id = id })

            assertEquals("new", dao.getDeviceConfigById(id)?.uuid)
            assertEquals(1, dao.getDeviceConfigsToUpload().size)
        }

    @Test
    fun `getDeviceConfigsToUpload returns only configs that are not uploaded`() =
        runTest {
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "pending", isUploaded = false))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "done", isUploaded = true))

            assertEquals(listOf("pending"), dao.getDeviceConfigsToUpload().map { it.uuid })
        }

    @Test
    fun `getDeviceConfigsToUpload is empty when everything is uploaded`() =
        runTest {
            dao.insertDeviceConfig(DbFixtures.deviceConfig(isUploaded = true))

            assertTrue(dao.getDeviceConfigsToUpload().isEmpty())
        }

    @Test
    fun `updateDeviceConfigBundleIds sets the bundle id only on the given configs`() =
        runTest {
            val first = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "a"))
            val second = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "b"))

            dao.updateDeviceConfigBundleIds(listOf(second), "bundle-2")

            assertNull(dao.getDeviceConfigById(first)?.bundleId)
            assertEquals("bundle-2", dao.getDeviceConfigById(second)?.bundleId)
        }

    @Test
    fun `updateDeviceConfigUploadStatus marks the given configs as uploaded`() =
        runTest {
            val first = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "a"))
            val second = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "b"))

            dao.updateDeviceConfigUploadStatus(listOf(first), true)

            assertTrue(dao.getDeviceConfigById(first)!!.isUploaded)
            assertFalse(dao.getDeviceConfigById(second)!!.isUploaded)
            assertEquals(listOf("b"), dao.getDeviceConfigsToUpload().map { it.uuid })
        }

    @Test
    fun `updateDeviceConfigUploadStatus can reset configs to not uploaded`() =
        runTest {
            val id = dao.insertDeviceConfig(DbFixtures.deviceConfig(isUploaded = true))

            dao.updateDeviceConfigUploadStatus(listOf(id), false)

            assertFalse(dao.getDeviceConfigById(id)!!.isUploaded)
        }

    @Test
    fun `getLatestDeviceConfig returns null when there are no configs`() =
        runTest {
            assertNull(dao.getLatestDeviceConfig())
        }

    @Test
    fun `getLatestDeviceConfig returns the config with the latest logged_at regardless of insert order`() =
        runTest {
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "middle", loggedAt = Instant.parse("2024-02-01T00:00:00Z")))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "latest", loggedAt = Instant.parse("2024-05-01T00:00:00Z")))
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "oldest", loggedAt = Instant.parse("2023-12-01T00:00:00Z")))

            assertEquals("latest", dao.getLatestDeviceConfig()?.uuid)
        }

    @Test
    fun `getLatestDeviceConfig ignores the upload status`() =
        runTest {
            dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "old", loggedAt = Instant.parse("2024-01-01T00:00:00Z")))
            dao.insertDeviceConfig(
                DbFixtures.deviceConfig(uuid = "new", loggedAt = Instant.parse("2024-06-01T00:00:00Z"), isUploaded = true),
            )

            assertEquals("new", dao.getLatestDeviceConfig()?.uuid)
        }
}