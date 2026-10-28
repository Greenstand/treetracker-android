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
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocationDaoTest : InMemoryDatabaseTest() {
    private val dao: LocationDAO get() = database.locationDao()

    private suspend fun insertSession(uuid: String = "session"): Long = database.sessionDao().insertSession(DbFixtures.session(uuid = uuid))

    private suspend fun insertTree(sessionId: Long): Long = database.treeDao().insertTree(DbFixtures.tree(sessionId = sessionId))

    private fun legacyLocationCount(): Int =
        database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM location_data")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

    private fun legacyLocationUploaded(id: Long): Boolean =
        database.openHelper.readableDatabase
            .query("SELECT uploaded FROM location_data WHERE _id = $id")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0) == 1
            }

    private fun insertLegacyLocation(
        id: Long,
        uploaded: Boolean,
    ) {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO location_data (_id, json_value, uploaded, created_at) " +
                "VALUES ($id, '{}', ${if (uploaded) 1 else 0}, 1600000000)",
        )
    }

    @Test
    fun `insertLocationData stores the location with defaults`() =
        runTest {
            val sessionId = insertSession()
            val location = DbFixtures.location(sessionId = sessionId, json = "{\"lat\":2.0}")

            val id = dao.insertLocationData(location)

            val stored = dao.getLocationData().single()
            assertEquals(location, stored)
            assertEquals(id, stored.id)
            assertFalse(stored.uploaded)
            assertEquals(location.createdAt, stored.createdAt)
        }

    @Test
    fun `insertLocationData fails when the session does not exist`() =
        runTest {
            val result = runCatching { dao.insertLocationData(DbFixtures.location(sessionId = 404)) }

            assertTrue(result.isFailure)
            assertTrue(dao.getLocationData().isEmpty())
        }

    @Test
    fun `getLocationData returns only locations that are not uploaded`() =
        runTest {
            val sessionId = insertSession()
            val pending = dao.insertLocationData(DbFixtures.location(sessionId, json = "pending"))
            dao.insertLocationData(DbFixtures.location(sessionId, json = "done").apply { uploaded = true })

            assertEquals(listOf(pending), dao.getLocationData().map { it.id })
        }

    @Test
    fun `updateLocationDataUploadStatus marks the given locations as uploaded`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertLocationData(DbFixtures.location(sessionId, json = "a"))
            val second = dao.insertLocationData(DbFixtures.location(sessionId, json = "b"))

            dao.updateLocationDataUploadStatus(listOf(first), true)

            assertEquals(listOf(second), dao.getLocationData().map { it.id })
        }

    @Test
    fun `updateLocationDataUploadStatus can reset locations to not uploaded`() =
        runTest {
            val sessionId = insertSession()
            val id = dao.insertLocationData(DbFixtures.location(sessionId).apply { uploaded = true })

            dao.updateLocationDataUploadStatus(listOf(id), false)

            assertEquals(listOf(id), dao.getLocationData().map { it.id })
        }

    @Test
    fun `purgeUploadedLocations deletes only uploaded locations`() =
        runTest {
            val sessionId = insertSession()
            val pending = dao.insertLocationData(DbFixtures.location(sessionId, json = "pending"))
            val uploaded = dao.insertLocationData(DbFixtures.location(sessionId, json = "done"))
            dao.updateLocationDataUploadStatus(listOf(uploaded), true)
            insertTree(sessionId)

            dao.purgeUploadedLocations()

            assertEquals(listOf(pending), dao.getLocationsForTreeSessions().map { it.id })
        }

    @Test
    fun `getLocationsForTreeSessions returns only locations of sessions that have trees`() =
        runTest {
            val withTrees = insertSession("with-trees")
            val withoutTrees = insertSession("without-trees")
            insertTree(withTrees)
            val kept = dao.insertLocationData(DbFixtures.location(withTrees))
            dao.insertLocationData(DbFixtures.location(withoutTrees))

            assertEquals(listOf(kept), dao.getLocationsForTreeSessions().map { it.id })
        }

    @Test
    fun `getLocationsForTreeSessions returns each location once even when its session has many trees`() =
        runTest {
            val sessionId = insertSession()
            insertTree(sessionId)
            insertTree(sessionId)
            insertTree(sessionId)
            val locationId = dao.insertLocationData(DbFixtures.location(sessionId))

            assertEquals(listOf(locationId), dao.getLocationsForTreeSessions().map { it.id })
        }

    @Test
    fun `getLocationsForTreeSessions orders by newest id first and applies the limit`() =
        runTest {
            val sessionId = insertSession()
            insertTree(sessionId)
            val first = dao.insertLocationData(DbFixtures.location(sessionId, json = "1"))
            val second = dao.insertLocationData(DbFixtures.location(sessionId, json = "2"))
            val third = dao.insertLocationData(DbFixtures.location(sessionId, json = "3"))

            assertEquals(listOf(third, second, first), dao.getLocationsForTreeSessions().map { it.id })
            assertEquals(listOf(third, second), dao.getLocationsForTreeSessions(limit = 2).map { it.id })
        }

    @Test
    fun `getLocationsForTreeSessions includes uploaded locations`() =
        runTest {
            val sessionId = insertSession()
            insertTree(sessionId)
            val id = dao.insertLocationData(DbFixtures.location(sessionId).apply { uploaded = true })

            val locations = dao.getLocationsForTreeSessions()

            assertEquals(listOf(id), locations.map { it.id })
            assertTrue(locations.single().uploaded)
        }

    @Test
    fun `getLocationsForTreeSessions is empty when there are no trees`() =
        runTest {
            val sessionId = insertSession()
            dao.insertLocationData(DbFixtures.location(sessionId))

            assertTrue(dao.getLocationsForTreeSessions().isEmpty())
        }

    @Test
    fun `updateLegacyLocationDataUploadStatus updates only the given legacy rows`() =
        runTest {
            insertLegacyLocation(id = 1, uploaded = false)
            insertLegacyLocation(id = 2, uploaded = false)

            dao.updateLegacyLocationDataUploadStatus(listOf(1L), true)

            assertTrue(legacyLocationUploaded(1))
            assertFalse(legacyLocationUploaded(2))
        }

    @Test
    fun `purgeUploadedTreeLocations deletes only uploaded legacy rows`() =
        runTest {
            insertLegacyLocation(id = 1, uploaded = true)
            insertLegacyLocation(id = 2, uploaded = false)

            dao.purgeUploadedTreeLocations()

            assertEquals(1, legacyLocationCount())
            assertFalse(legacyLocationUploaded(2))
        }

    @Test
    fun `purgeUploadedTreeLocations leaves the new location table alone`() =
        runTest {
            val sessionId = insertSession()
            val id = dao.insertLocationData(DbFixtures.location(sessionId).apply { uploaded = true })
            insertTree(sessionId)

            dao.purgeUploadedTreeLocations()

            assertEquals(listOf(id), dao.getLocationsForTreeSessions().map { it.id })
        }
}