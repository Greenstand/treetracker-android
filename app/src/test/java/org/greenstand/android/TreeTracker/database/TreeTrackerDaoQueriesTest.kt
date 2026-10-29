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
package org.greenstand.android.TreeTracker.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers every [TreeTrackerDAO] query (the DAO the sync and capture use cases use). The per-table DAOs in
 * database/dao run the same SQL and have more edge-case tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TreeTrackerDaoQueriesTest : InMemoryDatabaseTest() {
    private val dao: TreeTrackerDAO get() = database.treeTrackerDao()

    private suspend fun insertSession(
        wallet: String = "wallet",
        uuid: String = "session",
        isUploaded: Boolean = false,
    ): Long = dao.insertSession(DbFixtures.session(uuid = uuid, originWallet = wallet, isUploaded = isUploaded))

    private suspend fun insertCheckIn(localPhotoPath: String? = "/check-in.jpg"): Long {
        val planterId = dao.getPlanterInfoIdByIdentifier("planter@example.com") ?: dao.insertPlanterInfo(DbFixtures.planterInfo())
        return dao.insertPlanterCheckIn(DbFixtures.planterCheckIn(planterId, localPhotoPath = localPhotoPath))
    }

    private fun legacyLocationUploadedFlags(): List<Boolean> =
        database.openHelper.readableDatabase
            .query("SELECT uploaded FROM location_data ORDER BY _id")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getInt(0) == 1)
                    }
                }
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

    // region users

    @Test
    fun `user queries filter by id, wallet, upload status and power user`() =
        runTest {
            val pending = dao.insertUser(DbFixtures.user(wallet = "pending", uploaded = false, powerUser = true))
            val uploaded = dao.insertUser(DbFixtures.user(wallet = "uploaded", uploaded = true))

            assertEquals("pending", dao.getUserById(pending)?.wallet)
            assertNull(dao.getUserById(999))
            assertEquals(uploaded, dao.getUserByWallet("uploaded")?.id)
            assertNull(dao.getUserByWallet("missing"))
            assertEquals(listOf(pending), dao.getAllUsersToUpload().map { it.id })
            assertEquals(pending, dao.getPowerUser()?.id)
            assertEquals(setOf(pending, uploaded), dao.getAllUsersList().map { it.id }.toSet())
            assertEquals(2, dao.getAllUsers().first().size)
        }

    @Test
    fun `getAllUsers flow emits again when a user is inserted`() =
        runTest {
            val user = DbFixtures.user()

            val (before, after) = dao.getAllUsers().valuesBeforeAndAfter { dao.insertUser(user) }

            assertEquals(emptyList<UserEntity>(), before)
            assertEquals(listOf(user), after)
        }

    @Test
    fun `deleteUserByWallet deletes matching users and returns the count`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "a"))
            dao.insertUser(DbFixtures.user(wallet = "b"))

            assertEquals(1, dao.deleteUserByWallet("a"))
            assertEquals(0, dao.deleteUserByWallet("a"))
            assertEquals(listOf("b"), dao.getAllUsersList().map { it.wallet })
        }

    @Test
    fun `user updates change only the targeted users`() =
        runTest {
            val first = dao.insertUser(DbFixtures.user(wallet = "a"))
            val second = dao.insertUser(DbFixtures.user(wallet = "b"))

            dao.updatePowerUserStatus(first, true)
            dao.updateUserBundleIds(listOf(first), "bundle")
            dao.updateUserUploadStatus(listOf(first), true)
            dao.updateUser(DbFixtures.user(wallet = "b").copy(firstName = "Edited").apply { id = second })

            val updatedFirst = dao.getUserById(first)!!
            assertTrue(updatedFirst.powerUser)
            assertEquals("bundle", updatedFirst.bundleId)
            assertTrue(updatedFirst.uploaded)
            val updatedSecond = dao.getUserById(second)!!
            assertFalse(updatedSecond.powerUser)
            assertNull(updatedSecond.bundleId)
            assertFalse(updatedSecond.uploaded)
            assertEquals("Edited", updatedSecond.firstName)
        }

    // endregion

    // region planters

    @Test
    fun `planter info queries filter by id, identifier and upload status`() =
        runTest {
            val pending = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "pending", uploaded = false))
            val uploaded = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "uploaded", uploaded = true))

            assertEquals(pending, dao.getPlanterInfoIdByIdentifier("pending"))
            assertNull(dao.getPlanterInfoIdByIdentifier("missing"))
            assertEquals("uploaded", dao.getPlanterInfoById(uploaded)?.identifier)
            assertNull(dao.getPlanterInfoById(999))
            assertEquals(pending, dao.getPlanterInfoByIdentifier("pending")?.id)
            assertNull(dao.getPlanterInfoByIdentifier("missing"))
            assertEquals(listOf(pending), dao.getAllPlanterInfoToUpload().map { it.id })
            assertEquals(setOf(pending, uploaded), dao.getAllPlanterInfoList().map { it.id }.toSet())
            assertEquals(2, dao.getAllPlanterInfo().first().size)
        }

    @Test
    fun `planter info updates change only the targeted planters`() =
        runTest {
            val first = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "a"))
            val second = dao.insertPlanterInfo(DbFixtures.planterInfo(identifier = "b"))

            dao.updatePlanterInfoBundleIds(listOf(first), "bundle")
            dao.updatePlanterInfoUploadStatus(listOf(first), true)
            dao.updatePlanterInfo(DbFixtures.planterInfo(identifier = "b").copy(lastName = "Edited").apply { id = second })

            assertEquals("bundle", dao.getPlanterInfoById(first)?.bundleId)
            assertTrue(dao.getPlanterInfoById(first)!!.uploaded)
            assertNull(dao.getPlanterInfoById(second)?.bundleId)
            assertFalse(dao.getPlanterInfoById(second)!!.uploaded)
            assertEquals("Edited", dao.getPlanterInfoById(second)?.lastName)
        }

    @Test
    fun `deletePlanterInfo removes a planter without check ins`() =
        runTest {
            val id = dao.insertPlanterInfo(DbFixtures.planterInfo())

            dao.deletePlanterInfo(dao.getPlanterInfoById(id)!!)

            assertTrue(dao.getAllPlanterInfoList().isEmpty())
        }

    @Test
    fun `planter check in queries filter by id, planter and local photo`() =
        runTest {
            val withPhoto = insertCheckIn(localPhotoPath = "/a.jpg")
            val withoutPhoto = insertCheckIn(localPhotoPath = null)
            val planterId = dao.getPlanterInfoIdByIdentifier("planter@example.com")!!

            assertEquals(listOf(withPhoto), dao.getPlanterCheckInsToUpload().map { it.id })
            assertEquals(listOf(withoutPhoto), dao.getPlanterCheckInsById(listOf(withoutPhoto)).map { it.id })
            assertEquals(setOf(withPhoto, withoutPhoto), dao.getAllPlanterCheckInsForPlanterInfoId(planterId).map { it.id }.toSet())
            assertEquals("/a.jpg", dao.getPlanterCheckInById(withPhoto)?.localPhotoPath)
            assertNull(dao.getPlanterCheckInById(999))
        }

    @Test
    fun `planter check in updates, image path removal and delete`() =
        runTest {
            val first = insertCheckIn(localPhotoPath = "/a.jpg")
            val second = insertCheckIn(localPhotoPath = "/b.jpg")
            val third = insertCheckIn(localPhotoPath = "/c.jpg")

            dao.updatePlanterCheckIn(dao.getPlanterCheckInById(first)!!.copy(photoUrl = "https://a").apply { id = first })
            dao.removePlanterCheckInLocalImagePaths(listOf(first))
            dao.deletePlanterCheckIn(dao.getPlanterCheckInById(third)!!)

            assertEquals("https://a", dao.getPlanterCheckInById(first)?.photoUrl)
            assertNull(dao.getPlanterCheckInById(first)?.localPhotoPath)
            assertEquals("/b.jpg", dao.getPlanterCheckInById(second)?.localPhotoPath)
            assertNull(dao.getPlanterCheckInById(third))
        }

    // endregion

    // region legacy tree captures

    @Test
    fun `tree capture queries and counts`() =
        runTest {
            val checkInId = insertCheckIn()
            val uploaded =
                dao.insertTreeCapture(
                    DbFixtures.treeCapture(checkInId, uuid = "uploaded", uploaded = true, photoUrl = "https://a", latitude = 1.0),
                )
            val pending = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, uuid = "pending", latitude = 2.0))

            assertEquals(1, dao.getUploadedTreeCaptureCount())
            assertEquals(1, dao.getNonUploadedTreeCaptureCount())
            assertEquals(1, dao.getUploadedLegacyTreeImageCount())
            assertEquals(1, dao.getUploadedLegacyTreeImageCountFlow().first())
            assertEquals(1, dao.getNonUploadedLegacyTreeCaptureImageCount())
            assertEquals(1, dao.getNonUploadedLegacyTreeCaptureImageCountFlow().first())
            assertEquals(listOf(pending), dao.getAllTreeCaptureIdsToUpload())
            assertEquals(setOf("uploaded", "pending"), dao.getAllTreeCaptures().map { it.uuid }.toSet())
            assertEquals("pending", dao.getTreeCaptureById(pending).uuid)
            assertEquals(listOf("uploaded"), dao.getTreeCapturesByIds(listOf(uploaded)).map { it.uuid })
            assertEquals(
                mapOf(uploaded to 1.0, pending to 2.0),
                dao.getTreeDataForMap().associate { it.treeCaptureId to it.latitude },
            )
        }

    @Test
    fun `tree capture updates change only the targeted captures`() =
        runTest {
            val checkInId = insertCheckIn()
            val first = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, localPhotoPath = "/a.jpg"))
            val second = dao.insertTreeCapture(DbFixtures.treeCapture(checkInId, localPhotoPath = "/b.jpg"))

            dao.updateTreeCapturesBundleIds(listOf(first), "bundle")
            dao.updateTreeCapturesUploadStatus(listOf(first), true)
            dao.removeTreeCapturesLocalImagePaths(listOf(first))
            dao.updateTreeCapture(DbFixtures.treeCapture(checkInId, localPhotoPath = "/b.jpg").copy(noteContent = "edited").apply { id = second })

            val updatedFirst = dao.getTreeCaptureById(first)
            assertEquals("bundle", updatedFirst.bundleId)
            assertTrue(updatedFirst.uploaded)
            assertNull(updatedFirst.localPhotoPath)
            val updatedSecond = dao.getTreeCaptureById(second)
            assertNull(updatedSecond.bundleId)
            assertFalse(updatedSecond.uploaded)
            assertEquals("/b.jpg", updatedSecond.localPhotoPath)
            assertEquals("edited", updatedSecond.noteContent)
        }

    @Test
    fun `insertTreeWithAttributes stores the capture with its attributes`() =
        runTest {
            val capture = DbFixtures.treeCapture(insertCheckIn())

            val captureId =
                dao.insertTreeWithAttributes(
                    capture,
                    listOf(DbFixtures.treeAttribute(0, key = "height", value = "4")),
                )
            val extraAttributeId = dao.insertTreeAttribute(DbFixtures.treeAttribute(captureId, key = "dbh", value = "9"))

            assertEquals(capture, dao.getTreeCaptureById(captureId))
            val attributes = dao.getTreeAttributeByTreeCaptureId(captureId)
            assertEquals(mapOf("height" to "4", "dbh" to "9"), attributes.associate { it.key to it.value })
            assertTrue(attributes.any { it.id == extraAttributeId })
        }

    @Test
    fun `deleteTreeCapture removes a capture without attributes`() =
        runTest {
            val captureId = dao.insertTreeCapture(DbFixtures.treeCapture(insertCheckIn()))

            dao.deleteTreeCapture(dao.getTreeCaptureById(captureId))

            assertTrue(dao.getAllTreeCaptures().isEmpty())
        }

    // endregion

    // region trees

    @Test
    fun `tree queries and counts`() =
        runTest {
            val mine = insertSession(wallet = "mine", uuid = "s1")
            val theirs = insertSession(wallet = "theirs", uuid = "s2")
            val uploaded = dao.insertTree(DbFixtures.tree(mine, uuid = "uploaded", uploaded = true, photoUrl = "https://a"))
            val pending = dao.insertTree(DbFixtures.tree(theirs, uuid = "pending"))

            assertEquals(1, dao.getUploadedTreeCount())
            assertEquals(1, dao.getNonUploadedTreeCount())
            assertEquals(1, dao.getUploadedTreeImageCount())
            assertEquals(1, dao.getUploadedTreeImageCountFlow().first())
            assertEquals(1, dao.getNonUploadedTreeImageCount())
            assertEquals(1, dao.getNonUploadedTreeImageCountFlow().first())
            assertEquals(listOf(pending), dao.getAllTreeIdsToUpload())
            assertEquals(1, dao.getTreeCountFromSessionId(mine))
            assertEquals(setOf("uploaded", "pending"), dao.getAllTrees().map { it.uuid }.toSet())
            assertEquals(listOf("uploaded"), dao.getTreesByIds(listOf(uploaded)).map { it.uuid })
            assertEquals(listOf("uploaded"), dao.getTreesByUserWallet("mine").first().map { it.uuid })
        }

    @Test
    fun `getTreesByUserWallet emits again when a tree is added`() =
        runTest {
            val sessionId = insertSession(wallet = "mine")
            val tree = DbFixtures.tree(sessionId)

            val (before, after) = dao.getTreesByUserWallet("mine").valuesBeforeAndAfter { dao.insertTree(tree) }

            assertEquals(emptyList<TreeEntity>(), before)
            assertEquals(listOf(tree), after)
        }

    @Test
    fun `tree updates change only the targeted trees`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId, photoPath = "/a.jpg"))
            val second = dao.insertTree(DbFixtures.tree(sessionId, photoPath = "/b.jpg"))

            dao.updateTreesBundleIds(listOf(first), "bundle")
            dao.updateTreesUploadStatus(listOf(first), true)
            dao.removeTreesLocalImagePaths(listOf(first))
            dao.updateTree(DbFixtures.tree(sessionId, photoPath = "/b.jpg").copy(note = "edited").apply { id = second })

            val updatedFirst = dao.getTreesByIds(listOf(first)).single()
            assertEquals("bundle", updatedFirst.bundleId)
            assertTrue(updatedFirst.uploaded)
            assertNull(updatedFirst.photoPath)
            val updatedSecond = dao.getTreesByIds(listOf(second)).single()
            assertNull(updatedSecond.bundleId)
            assertFalse(updatedSecond.uploaded)
            assertEquals("/b.jpg", updatedSecond.photoPath)
            assertEquals("edited", updatedSecond.note)
        }

    @Test
    fun `deleteTreeById deletes only that tree`() =
        runTest {
            val sessionId = insertSession()
            val first = dao.insertTree(DbFixtures.tree(sessionId, uuid = "a"))
            dao.insertTree(DbFixtures.tree(sessionId, uuid = "b"))

            dao.deleteTreeById(first)

            assertEquals(listOf("b"), dao.getAllTrees().map { it.uuid })
        }

    // endregion

    // region locations

    @Test
    fun `location queries filter by upload status and tree sessions`() =
        runTest {
            val withTrees = insertSession(uuid = "with-trees")
            val withoutTrees = insertSession(uuid = "without-trees")
            dao.insertTree(DbFixtures.tree(withTrees))
            dao.insertTree(DbFixtures.tree(withTrees))
            val first = dao.insertLocationData(DbFixtures.location(withTrees))
            val second = dao.insertLocationData(DbFixtures.location(withTrees))
            val orphan = dao.insertLocationData(DbFixtures.location(withoutTrees))

            dao.updateLocationDataUploadStatus(listOf(first), true)

            assertEquals(setOf(second, orphan), dao.getLocationData().map { it.id }.toSet())
            assertEquals(listOf(second, first), dao.getLocationsForTreeSessions().map { it.id })
            assertEquals(listOf(second), dao.getLocationsForTreeSessions(limit = 1).map { it.id })
        }

    @Test
    fun `purgeUploadedLocations deletes only uploaded locations`() =
        runTest {
            val sessionId = insertSession()
            val uploaded = dao.insertLocationData(DbFixtures.location(sessionId))
            val pending = dao.insertLocationData(DbFixtures.location(sessionId))
            dao.updateLocationDataUploadStatus(listOf(uploaded), true)

            dao.purgeUploadedLocations()

            assertEquals(listOf(pending), dao.getLocationData().map { it.id })
            dao.insertTree(DbFixtures.tree(sessionId))
            assertEquals(listOf(pending), dao.getLocationsForTreeSessions().map { it.id })
        }

    @Test
    fun `legacy location data upload status and purge`() =
        runTest {
            insertLegacyLocation(id = 1, uploaded = false)
            insertLegacyLocation(id = 2, uploaded = false)

            dao.updateLegacyLocationDataUploadStatus(listOf(1L), true)
            assertEquals(listOf(true, false), legacyLocationUploadedFlags())

            dao.purgeUploadedTreeLocations()
            assertEquals(listOf(false), legacyLocationUploadedFlags())
        }

    // endregion

    // region sessions

    @Test
    fun `session queries filter by wallet and upload status`() =
        runTest {
            val pending = insertSession(wallet = "a", uuid = "pending")
            val uploaded = insertSession(wallet = "b", uuid = "uploaded", isUploaded = true)

            assertEquals("pending", dao.getSessionById(pending).uuid)
            assertEquals(listOf(uploaded), dao.getSessionsByUserWallet("b").map { it.id })
            assertTrue(dao.getSessionsByUserWallet("missing").isEmpty())
            assertEquals(listOf(pending), dao.getSessionsToUpload().map { it.id })
        }

    @Test
    fun `session updates change only the targeted sessions`() =
        runTest {
            val first = insertSession(uuid = "a")
            val second = insertSession(uuid = "b")

            dao.updateSessionBundleIds(listOf(first), "bundle")
            dao.updateSessionUploadStatus(listOf(first), true)
            dao.updateSession(DbFixtures.session(uuid = "b", note = "note", endTime = Instant.parse("2024-03-01T20:00:00Z")).apply { id = second })

            assertEquals("bundle", dao.getSessionById(first).bundleId)
            assertTrue(dao.getSessionById(first).isUploaded)
            val updatedSecond = dao.getSessionById(second)
            assertNull(updatedSecond.bundleId)
            assertFalse(updatedSecond.isUploaded)
            assertEquals("note", updatedSecond.note)
            assertEquals(Instant.parse("2024-03-01T20:00:00Z"), updatedSecond.endTime)
        }

    // endregion

    // region device config

    @Test
    fun `device config queries and updates`() =
        runTest {
            val old = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "old", loggedAt = Instant.parse("2024-01-01T00:00:00Z")))
            val latest = dao.insertDeviceConfig(DbFixtures.deviceConfig(uuid = "latest", loggedAt = Instant.parse("2024-06-01T00:00:00Z")))

            dao.updateDeviceConfigUploadStatus(listOf(old), true)
            dao.updateDeviceConfigBundleIds(listOf(old), "bundle")

            assertEquals("latest", dao.getLatestDeviceConfig()?.uuid)
            assertEquals(listOf(latest), dao.getDeviceConfigsToUpload().map { it.id })
            assertEquals("bundle", dao.getDeviceConfigById(old)?.bundleId)
            assertTrue(dao.getDeviceConfigById(old)!!.isUploaded)
            assertNull(dao.getDeviceConfigById(latest)?.bundleId)
            assertNull(dao.getDeviceConfigById(999))
        }

    @Test
    fun `getLatestDeviceConfig returns null without configs`() =
        runTest {
            assertNull(dao.getLatestDeviceConfig())
        }

    // endregion

    // region organizations

    @Test
    fun `org queries find orgs by id and replace on conflict`() =
        runTest {
            dao.insertOrg(DbFixtures.org(id = "a", name = "Old"))
            dao.insertOrg(DbFixtures.org(id = "a", name = "New"))
            dao.insertOrg(DbFixtures.org(id = "b"))

            assertEquals("New", dao.getOrg("a")?.name)
            assertNull(dao.getOrg("missing"))
            assertNull(dao.getOrg(null))
            assertEquals(setOf("a", "b"), dao.getAllOrgs().map { it.id }.toSet())
        }

    // endregion

    @Test
    fun `rows written through the per-table DAOs are visible through TreeTrackerDAO`() =
        runTest {
            val userId = database.userDao().insertUser(DbFixtures.user())
            val sessionId = database.sessionDao().insertSession(DbFixtures.session())
            database.treeDao().insertTree(DbFixtures.tree(sessionId))

            assertNotNull(dao.getUserById(userId))
            assertEquals(1, dao.getTreeCountFromSessionId(sessionId))
        }
}