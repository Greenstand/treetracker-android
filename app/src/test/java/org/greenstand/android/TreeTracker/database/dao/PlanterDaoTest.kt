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

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.database.DbFixtures
import org.greenstand.android.TreeTracker.database.InMemoryDatabaseTest
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.valuesBeforeAndAfter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlanterDaoTest : InMemoryDatabaseTest() {
    private val dao: PlanterDAO get() = database.planterDao()

    // region planter_info

    @Test
    fun `insertPlanterInfo stores every field and returns the generated id`() =
        runTest {
            val planter = DbFixtures.planterInfo(bundleId = "bundle", uploaded = true)

            val id = dao.insertPlanterInfo(planter)

            val stored = dao.getPlanterInfoById(id)
            assertEquals(planter, stored)
            assertEquals(id, stored?.id)
        }

    @Test
    fun `insertPlanterInfo keeps null optional fields`() =
        runTest {
            val id = dao.insertPlanterInfo(DbFixtures.planterInfo(organization = null, bundleId = null))

            val stored = dao.getPlanterInfoById(id)!!
            assertNull(stored.organization)
            assertNull(stored.phone)
            assertNull(stored.bundleId)
        }

    @Test
    fun `getPlanterInfoById returns null for an unknown id`() =
        runTest {
            assertNull(dao.getPlanterInfoById(3))
        }

    @Test
    fun `getPlanterInfoIdByIdentifier returns the id of the matching planter`() =
        runTest {
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a@example.com"))
            val id = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b@example.com"))

            assertEquals(id, dao.getPlanterInfoIdByIdentifier("b@example.com"))
        }

    @Test
    fun `getPlanterInfoIdByIdentifier returns null for an unknown identifier`() =
        runTest {
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a@example.com"))

            assertNull(dao.getPlanterInfoIdByIdentifier("missing"))
        }

    @Test
    fun `getPlanterInfoByIdentifier returns the matching planter`() =
        runTest {
            val planter = DbFixtures.planterInfo(identifier = "a@example.com")
            dao.insertPlanterInfo(planter)
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b@example.com"))

            assertEquals(planter, dao.getPlanterInfoByIdentifier("a@example.com"))
        }

    @Test
    fun `getPlanterInfoByIdentifier returns null for an unknown identifier`() =
        runTest {
            assertNull(dao.getPlanterInfoByIdentifier("missing"))
        }

    @Test
    fun `getAllPlanterInfoList returns every planter`() =
        runTest {
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a"))
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b"))

            assertEquals(setOf("a", "b"), dao.getAllPlanterInfoList().map { it.identifier }.toSet())
        }

    @Test
    fun `getAllPlanterInfo flow returns the current planters`() =
        runTest {
            val planter = DbFixtures.planterInfo()
            dao.insertPlanterInfo(planter)

            assertEquals(listOf(planter), dao.getAllPlanterInfo().first())
        }

    @Test
    fun `getAllPlanterInfo flow emits again when a planter is inserted`() =
        runTest {
            val planter = DbFixtures.planterInfo()

            val (before, after) = dao.getAllPlanterInfo().valuesBeforeAndAfter { dao.insertPlanterInfo(planter) }

            assertEquals(emptyList<PlanterInfoEntity>(), before)
            assertEquals(listOf(planter), after)
        }

    @Test
    fun `getAllPlanterInfoToUpload returns only planters that are not uploaded`() =
        runTest {
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "pending", uploaded = false))
            dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "done", uploaded = true))

            assertEquals(listOf("pending"), dao.getAllPlanterInfoToUpload().map { it.identifier })
        }

    @Test
    fun `updatePlanterInfo updates the row with the same id`() =
        runTest {
            val id = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val updated = DbFixtures.planterInfo().copy(firstName = "Rear Admiral").apply { this.id = id }

            dao.updatePlanterInfo(updated)

            assertEquals("Rear Admiral", dao.getPlanterInfoById(id)?.firstName)
        }

    @Test
    fun `deletePlanterInfo removes the planter`() =
        runTest {
            val id = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val stored = dao.getPlanterInfoById(id)!!

            dao.deletePlanterInfo(stored)

            assertNull(dao.getPlanterInfoById(id))
        }

    @Test
    fun `deletePlanterInfo fails while check ins still reference the planter`() =
        runTest {
            val id = dao.insertPlanterInfo(DbFixtures.planterInfo())
            dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = id))
            val stored = dao.getPlanterInfoById(id)!!

            val result = runCatching { dao.deletePlanterInfo(stored) }

            assertTrue(result.isFailure)
            assertNotNull(dao.getPlanterInfoById(id))
        }

    @Test
    fun `updatePlanterInfoBundleIds sets the bundle id only on the given planters`() =
        runTest {
            val first = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a"))
            val second = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b"))

            dao.updatePlanterInfoBundleIds(listOf(second), "bundle-2")

            assertNull(dao.getPlanterInfoById(first)?.bundleId)
            assertEquals("bundle-2", dao.getPlanterInfoById(second)?.bundleId)
        }

    @Test
    fun `updatePlanterInfoUploadStatus marks the given planters as uploaded`() =
        runTest {
            val first = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a"))
            val second = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b"))

            dao.updatePlanterInfoUploadStatus(listOf(first), true)

            assertTrue(dao.getPlanterInfoById(first)!!.uploaded)
            assertFalse(dao.getPlanterInfoById(second)!!.uploaded)
            assertEquals(listOf("b"), dao.getAllPlanterInfoToUpload().map { it.identifier })
        }

    // endregion

    // region planter_check_in

    @Test
    fun `insertPlanterCheckIn stores every field and returns the generated id`() =
        runTest {
            val planterId = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val checkIn = DbFixtures.planterCheckIn(planterInfoId = planterId, photoUrl = "https://photo")

            val id = dao.insertPlanterCheckIn(checkIn)

            val stored = dao.getPlanterCheckInById(id)
            assertEquals(checkIn, stored)
            assertEquals(id, stored?.id)
        }

    @Test
    fun `insertPlanterCheckIn fails when the planter does not exist`() =
        runTest {
            val result = runCatching { dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterInfoId = 99)) }

            assertTrue(result.isFailure)
            assertTrue(dao.getPlanterCheckInsToUpload().isEmpty())
        }

    @Test
    fun `getPlanterCheckInById returns null for an unknown id`() =
        runTest {
            assertNull(dao.getPlanterCheckInById(5))
        }

    @Test
    fun `getPlanterCheckInsToUpload returns only check ins that still have a local photo`() =
        runTest {
            val planterId = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val withPhoto = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId, localPhotoPath = "/a.jpg"))
            dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId, localPhotoPath = null))

            assertEquals(listOf(withPhoto), dao.getPlanterCheckInsToUpload().map { it.id })
        }

    @Test
    fun `getPlanterCheckInsById returns only the requested check ins`() =
        runTest {
            val planterId = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val first = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId))
            val second = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId))
            val third = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId))

            assertEquals(setOf(first, third), dao.getPlanterCheckInsById(listOf(first, third)).map { it.id }.toSet())
            assertEquals(listOf(second), dao.getPlanterCheckInsById(listOf(second, 1000L)).map { it.id })
        }

    @Test
    fun `getAllPlanterCheckInsForPlanterInfoId returns only that planter's check ins`() =
        runTest {
            val planterA = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a"))
            val planterB = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b"))
            val checkInA = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterA))
            dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterB))

            assertEquals(listOf(checkInA), dao.getAllPlanterCheckInsForPlanterInfoId(planterA).map { it.id })
            assertTrue(dao.getAllPlanterCheckInsForPlanterInfoId(1000).isEmpty())
        }

    @Test
    fun `updatePlanterCheckIn updates the row with the same id`() =
        runTest {
            val planterId = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val id = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId))
            val updated = DbFixtures.planterCheckIn(planterId, photoUrl = "https://uploaded").apply { this.id = id }

            dao.updatePlanterCheckIn(updated)

            assertEquals("https://uploaded", dao.getPlanterCheckInById(id)?.photoUrl)
        }

    @Test
    fun `deletePlanterCheckIn removes the check in`() =
        runTest {
            val planterId = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val id = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId))

            dao.deletePlanterCheckIn(dao.getPlanterCheckInById(id)!!)

            assertNull(dao.getPlanterCheckInById(id))
        }

    @Test
    fun `removePlanterCheckInLocalImagePaths clears the local photo only for the given check ins`() =
        runTest {
            val planterId = dao.insertPlanterInfo(DbFixtures.planterInfo())
            val first = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId, localPhotoPath = "/a.jpg"))
            val second = dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId, localPhotoPath = "/b.jpg"))

            dao.removePlanterCheckInLocalImagePaths(listOf(first))

            assertNull(dao.getPlanterCheckInById(first)?.localPhotoPath)
            assertEquals("/b.jpg", dao.getPlanterCheckInById(second)?.localPhotoPath)
            assertEquals(listOf(second), dao.getPlanterCheckInsToUpload().map { it.id })
        }

    // endregion
}