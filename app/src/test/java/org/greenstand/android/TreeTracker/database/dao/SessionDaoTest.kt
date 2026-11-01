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
class SessionDaoTest : InMemoryDatabaseTest() {
    private val dao: SessionDAO get() = database.sessionDao()

    @Test
    fun `insertSession stores every field and returns the generated id`() =
        runTest {
            val deviceConfigId = database.deviceConfigDao().insertDeviceConfig(DbFixtures.deviceConfig())
            val session =
                DbFixtures.session(
                    deviceConfigId = deviceConfigId,
                    endTime = Instant.parse("2024-03-01T12:00:00Z"),
                    note = "planted near the river",
                )

            val id = dao.insertSession(session)

            val stored = dao.getSessionById(id)
            assertEquals(session, stored)
            assertEquals(id, stored.id)
        }

    @Test
    fun `insertSession keeps null optional fields`() =
        runTest {
            val id = dao.insertSession(DbFixtures.session(deviceConfigId = null, endTime = null, organization = null, note = null))

            val stored = dao.getSessionById(id)
            assertNull(stored.deviceConfigId)
            assertNull(stored.endTime)
            assertNull(stored.organization)
            assertNull(stored.note)
            assertNull(stored.bundleId)
        }

    @Test
    fun `insertSession fails when the device config does not exist`() =
        runTest {
            val result = runCatching { dao.insertSession(DbFixtures.session(deviceConfigId = 999)) }

            assertTrue(result.isFailure)
            assertTrue(dao.getSessionsToUpload().isEmpty())
        }

    @Test
    fun `updateSession updates the row with the same id`() =
        runTest {
            val id = dao.insertSession(DbFixtures.session())
            val updated =
                DbFixtures
                    .session(endTime = Instant.parse("2024-03-01T18:00:00Z"), note = "done")
                    .apply { this.id = id }

            dao.updateSession(updated)

            val stored = dao.getSessionById(id)
            assertEquals(Instant.parse("2024-03-01T18:00:00Z"), stored.endTime)
            assertEquals("done", stored.note)
        }

    @Test
    fun `getSessionsByUserWallet returns only sessions from that wallet`() =
        runTest {
            dao.insertSession(DbFixtures.session(uuid = "a1", originWallet = "a"))
            dao.insertSession(DbFixtures.session(uuid = "b1", originWallet = "b"))
            dao.insertSession(DbFixtures.session(uuid = "a2", originWallet = "a"))

            assertEquals(setOf("a1", "a2"), dao.getSessionsByUserWallet("a").map { it.uuid }.toSet())
        }

    @Test
    fun `getSessionsByUserWallet is empty for an unknown wallet`() =
        runTest {
            dao.insertSession(DbFixtures.session(originWallet = "a"))

            assertTrue(dao.getSessionsByUserWallet("missing").isEmpty())
        }

    @Test
    fun `getSessionsToUpload returns only sessions that are not uploaded`() =
        runTest {
            dao.insertSession(DbFixtures.session(uuid = "pending", isUploaded = false))
            dao.insertSession(DbFixtures.session(uuid = "done", isUploaded = true))

            assertEquals(listOf("pending"), dao.getSessionsToUpload().map { it.uuid })
        }

    @Test
    fun `updateSessionBundleIds sets the bundle id only on the given sessions`() =
        runTest {
            val first = dao.insertSession(DbFixtures.session(uuid = "a"))
            val second = dao.insertSession(DbFixtures.session(uuid = "b"))

            dao.updateSessionBundleIds(listOf(first), "bundle-1")

            assertEquals("bundle-1", dao.getSessionById(first).bundleId)
            assertNull(dao.getSessionById(second).bundleId)
        }

    @Test
    fun `updateSessionUploadStatus marks the given sessions as uploaded`() =
        runTest {
            val first = dao.insertSession(DbFixtures.session(uuid = "a"))
            val second = dao.insertSession(DbFixtures.session(uuid = "b"))

            dao.updateSessionUploadStatus(listOf(first), true)

            assertTrue(dao.getSessionById(first).isUploaded)
            assertFalse(dao.getSessionById(second).isUploaded)
            assertEquals(listOf("b"), dao.getSessionsToUpload().map { it.uuid })
        }

    @Test
    fun `updateSessionUploadStatus can reset sessions to not uploaded`() =
        runTest {
            val id = dao.insertSession(DbFixtures.session(isUploaded = true))

            dao.updateSessionUploadStatus(listOf(id), false)

            assertFalse(dao.getSessionById(id).isUploaded)
        }
}