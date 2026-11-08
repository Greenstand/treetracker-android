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
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.valuesBeforeAndAfter
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
class UserDaoTest : InMemoryDatabaseTest() {
    private val dao: UserDAO get() = database.userDao()

    @Test
    fun `insertUser returns a generated id and stores every field`() =
        runTest {
            val user = DbFixtures.user(email = "ada@example.com", photoUrl = "https://photo", bundleId = "bundle")

            val id = dao.insertUser(user)

            val stored = dao.getUserById(id)
            assertEquals(user, stored)
            assertEquals(id, stored?.id)
        }

    @Test
    fun `insertUser keeps null optional fields`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user(phone = null, email = null, photoUrl = null, bundleId = null))

            val stored = dao.getUserById(id)!!
            assertNull(stored.phone)
            assertNull(stored.email)
            assertNull(stored.photoUrl)
            assertNull(stored.bundleId)
        }

    @Test
    fun `insertUser assigns increasing ids`() =
        runTest {
            val first = dao.insertUser(DbFixtures.user(wallet = "a"))
            val second = dao.insertUser(DbFixtures.user(wallet = "b"))

            assertTrue(second > first)
        }

    @Test
    fun `insertUser with an existing id replaces the row`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user(wallet = "old"))

            dao.insertUser(DbFixtures.user(wallet = "new").apply { this.id = id })

            val users = dao.getAllUsersList()
            assertEquals(1, users.size)
            assertEquals("new", users.single().wallet)
        }

    @Test
    fun `getUserById returns null for an unknown id`() =
        runTest {
            assertNull(dao.getUserById(42))
        }

    @Test
    fun `getAllUsersList returns every user`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "a"))
            dao.insertUser(DbFixtures.user(wallet = "b"))

            assertEquals(setOf("a", "b"), dao.getAllUsersList().map { it.wallet }.toSet())
        }

    @Test
    fun `getAllUsersList is empty when there are no users`() =
        runTest {
            assertTrue(dao.getAllUsersList().isEmpty())
        }

    @Test
    fun `getAllUsers flow returns the current users`() =
        runTest {
            val user = DbFixtures.user()
            dao.insertUser(user)

            assertEquals(listOf(user), dao.getAllUsers().first())
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
    fun `getAllUsersToUpload returns only users that are not uploaded`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "pending", uploaded = false))
            dao.insertUser(DbFixtures.user(wallet = "done", uploaded = true))

            assertEquals(listOf("pending"), dao.getAllUsersToUpload().map { it.wallet })
        }

    @Test
    fun `getUserByWallet returns the matching user`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "a", uuid = "uuid-a"))
            dao.insertUser(DbFixtures.user(wallet = "b", uuid = "uuid-b"))

            assertEquals("uuid-b", dao.getUserByWallet("b")?.uuid)
        }

    @Test
    fun `getUserByWallet returns null when no user has the wallet`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "a"))

            assertNull(dao.getUserByWallet("missing"))
        }

    @Test
    fun `getPowerUser returns the power user`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "regular", powerUser = false))
            dao.insertUser(DbFixtures.user(wallet = "power", powerUser = true))

            assertEquals("power", dao.getPowerUser()?.wallet)
        }

    @Test
    fun `getPowerUser returns null when nobody is a power user`() =
        runTest {
            dao.insertUser(DbFixtures.user(powerUser = false))

            assertNull(dao.getPowerUser())
        }

    @Test
    fun `deleteUserByWallet deletes matching users and returns the count`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "a"))
            dao.insertUser(DbFixtures.user(wallet = "b"))

            val deleted = dao.deleteUserByWallet("a")

            assertEquals(1, deleted)
            assertEquals(listOf("b"), dao.getAllUsersList().map { it.wallet })
        }

    @Test
    fun `deleteUserByWallet returns zero when nothing matches`() =
        runTest {
            dao.insertUser(DbFixtures.user(wallet = "a"))

            assertEquals(0, dao.deleteUserByWallet("missing"))
            assertEquals(1, dao.getAllUsersList().size)
        }

    @Test
    fun `updatePowerUserStatus sets and clears the flag for one user`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user(wallet = "a", powerUser = false))
            val otherId = dao.insertUser(DbFixtures.user(wallet = "b", powerUser = false))

            dao.updatePowerUserStatus(id, true)
            assertTrue(dao.getUserById(id)!!.powerUser)
            assertFalse(dao.getUserById(otherId)!!.powerUser)

            dao.updatePowerUserStatus(id, false)
            assertFalse(dao.getUserById(id)!!.powerUser)
        }

    @Test
    fun `updateUser updates the row with the same id`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user())
            val updated = DbFixtures.user().copy(firstName = "Augusta", photoUrl = "https://new").apply { this.id = id }

            dao.updateUser(updated)

            assertEquals(updated, dao.getUserById(id))
        }

    @Test
    fun `updateUser does nothing for a user that is not stored`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user())
            val stranger = DbFixtures.user(wallet = "stranger").apply { this.id = id + 100 }

            dao.updateUser(stranger)

            assertEquals(listOf(DbFixtures.user()), dao.getAllUsersList())
        }

    @Test
    fun `updateUserBundleIds sets the bundle id only on the given users`() =
        runTest {
            val first = dao.insertUser(DbFixtures.user(wallet = "a"))
            val second = dao.insertUser(DbFixtures.user(wallet = "b"))
            val third = dao.insertUser(DbFixtures.user(wallet = "c"))

            dao.updateUserBundleIds(listOf(first, third), "bundle-1")

            assertEquals("bundle-1", dao.getUserById(first)?.bundleId)
            assertNull(dao.getUserById(second)?.bundleId)
            assertEquals("bundle-1", dao.getUserById(third)?.bundleId)
        }

    @Test
    fun `updateUserUploadStatus marks the given users as uploaded`() =
        runTest {
            val first = dao.insertUser(DbFixtures.user(wallet = "a"))
            val second = dao.insertUser(DbFixtures.user(wallet = "b"))

            dao.updateUserUploadStatus(listOf(first), true)

            assertTrue(dao.getUserById(first)!!.uploaded)
            assertFalse(dao.getUserById(second)!!.uploaded)
            assertEquals(listOf(second), dao.getAllUsersToUpload().map { it.id })
        }

    @Test
    fun `updateUserUploadStatus can reset users to not uploaded`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user(uploaded = true))

            dao.updateUserUploadStatus(listOf(id), false)

            assertFalse(dao.getUserById(id)!!.uploaded)
        }

    @Test
    fun `updateUserBundleIds with no ids changes nothing`() =
        runTest {
            val id = dao.insertUser(DbFixtures.user(bundleId = "original"))

            dao.updateUserBundleIds(emptyList(), "bundle")

            assertEquals("original", dao.getUserById(id)?.bundleId)
        }
}