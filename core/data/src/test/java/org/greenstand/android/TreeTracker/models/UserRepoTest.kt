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
package org.greenstand.android.TreeTracker.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class UserRepoTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var analytics: Analytics

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    @MockK(relaxed = true)
    private lateinit var messagesDao: MessagesDAO

    @MockK(relaxed = true)
    private lateinit var exceptionDataCollector: ExceptionDataCollector

    private lateinit var userRepo: UserRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        userRepo =
            UserRepo(
                locationUpdateManager = locationUpdateManager,
                dao = dao,
                analytics = analytics,
                timeProvider = timeProvider,
                messagesDao = messagesDao,
                exceptionDataCollector = exceptionDataCollector,
            )
    }

    private fun createFakeUserEntity(
        id: Long = 1L,
        wallet: String = "wallet",
        firstName: String = "Jay",
        lastName: String = "Ray",
        powerUser: Boolean = false,
        photoPath: String = "photo",
    ): UserEntity {
        val entity =
            UserEntity(
                uuid = "uuid-$id",
                wallet = wallet,
                firstName = firstName,
                lastName = lastName,
                phone = null,
                email = null,
                latitude = 0.0,
                longitude = 0.0,
                uploaded = false,
                createdAt = Instant.DISTANT_FUTURE,
                photoPath = photoPath,
                photoUrl = null,
                powerUser = powerUser,
            )
        entity.id = id
        return entity
    }

    private fun stubSessionsAndTreeCount(
        wallet: String,
        sessions: List<SessionEntity>,
        treeCounts: List<Int>,
    ) {
        coEvery { dao.getSessionsByUserWallet(wallet) } returns sessions
        sessions.forEachIndexed { index, session ->
            coEvery { dao.getTreeCountFromSessionId(session.id) } returns treeCounts[index]
        }
    }

    @Test
    fun `WHEN getUserList called THEN returns mapped list of Users`() =
        runTest {
            val entity1 = createFakeUserEntity(id = 1L, wallet = "wallet1", firstName = "Alice", lastName = "Smith")
            val entity2 = createFakeUserEntity(id = 2L, wallet = "wallet2", firstName = "Bob", lastName = "Jones")

            coEvery { dao.getAllUsersList() } returns listOf(entity1, entity2)
            stubSessionsAndTreeCount("wallet1", emptyList(), emptyList())
            stubSessionsAndTreeCount("wallet2", emptyList(), emptyList())
            coEvery { messagesDao.getUnreadMessageCountForWallet(any()) } returns 0

            val result = userRepo.getUserList()

            assertEquals(2, result.size)
            assertEquals("Alice", result[0].firstName)
            assertEquals("Bob", result[1].firstName)
        }

    @Test
    fun `WHEN getUser called with valid ID THEN returns User`() =
        runTest {
            val entity = createFakeUserEntity(id = 10L, wallet = "w10")
            coEvery { dao.getUserById(10L) } returns entity
            stubSessionsAndTreeCount("w10", emptyList(), emptyList())
            coEvery { messagesDao.getUnreadMessageCountForWallet("w10") } returns 0

            val result = userRepo.getUser(10L)

            assertNotNull(result)
            assertEquals(10L, result.id)
        }

    @Test
    fun `WHEN getUser called with nonexistent ID THEN returns null`() =
        runTest {
            coEvery { dao.getUserById(999L) } returns null

            val result = userRepo.getUser(999L)

            assertNull(result)
        }

    @Test
    fun `WHEN getUserWithWallet called THEN returns user`() =
        runTest {
            val entity = createFakeUserEntity(id = 5L, wallet = "my-wallet")
            coEvery { dao.getUserByWallet("my-wallet") } returns entity
            stubSessionsAndTreeCount("my-wallet", emptyList(), emptyList())
            coEvery { messagesDao.getUnreadMessageCountForWallet("my-wallet") } returns 0

            val result = userRepo.getUserWithWallet("my-wallet")

            assertNotNull(result)
            assertEquals("my-wallet", result.wallet)
        }

    @Test
    fun `WHEN deleteUser with rows deleted greater than 0 THEN returns true`() =
        runTest {
            coEvery { dao.deleteUserByWallet("wallet-to-delete") } returns 1

            val result = userRepo.deleteUser("wallet-to-delete")

            assertTrue(result)
        }

    @Test
    fun `WHEN deleteUser with 0 rows deleted THEN returns false`() =
        runTest {
            coEvery { dao.deleteUserByWallet("nonexistent") } returns 0

            val result = userRepo.deleteUser("nonexistent")

            assertFalse(result)
        }

    @Test
    fun `WHEN checkForUnreadMessagesPerUser with count gte 1 THEN returns true`() =
        runTest {
            coEvery { messagesDao.getUnreadMessageCountForWallet("wallet") } returns 3

            val result = userRepo.checkForUnreadMessagesPerUser("wallet")

            assertTrue(result)
        }

    @Test
    fun `WHEN checkForUnreadMessagesPerUser with count 0 THEN returns false`() =
        runTest {
            coEvery { messagesDao.getUnreadMessageCountForWallet("wallet") } returns 0

            val result = userRepo.checkForUnreadMessagesPerUser("wallet")

            assertFalse(result)
        }

    @Test
    fun `WHEN getPowerUser and user exists THEN returns user`() =
        runTest {
            val entity = createFakeUserEntity(id = 7L, wallet = "power-wallet", powerUser = true)
            coEvery { dao.getPowerUser() } returns entity
            stubSessionsAndTreeCount("power-wallet", emptyList(), emptyList())
            coEvery { messagesDao.getUnreadMessageCountForWallet("power-wallet") } returns 0

            val result = userRepo.getPowerUser()

            assertNotNull(result)
            assertTrue(result.isPowerUser)
        }

    @Test
    fun `WHEN getPowerUser and no power user exists THEN returns null`() =
        runTest {
            coEvery { dao.getPowerUser() } returns null

            val result = userRepo.getPowerUser()

            assertNull(result)
        }

    @Test
    fun `WHEN createUser called THEN inserts entity and calls analytics`() =
        runTest {
            every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000L)
            coEvery { dao.insertUser(any()) } returns 42L

            val result =
                userRepo.createUser(
                    firstName = "Test",
                    lastName = "User",
                    phone = "123",
                    email = "test@test.com",
                    wallet = "test-wallet",
                    photoPath = "path",
                    isPowerUser = false,
                )

            assertEquals(42L, result)
            coVerify { dao.insertUser(any()) }
            coVerify { analytics.userInfoCreated(phone = "123", email = "test@test.com") }
        }

    @Test
    fun `WHEN doesUserExists with existing user THEN returns true`() =
        runTest {
            val entity = createFakeUserEntity(id = 1L, wallet = "existing-wallet")
            coEvery { dao.getUserByWallet("existing-wallet") } returns entity
            stubSessionsAndTreeCount("existing-wallet", emptyList(), emptyList())
            coEvery { messagesDao.getUnreadMessageCountForWallet("existing-wallet") } returns 0

            val result = userRepo.doesUserExists("existing-wallet")

            assertTrue(result)
        }

    @Test
    fun `WHEN doesUserExists with nonexistent user THEN returns false`() =
        runTest {
            coEvery { dao.getUserByWallet("missing") } returns null

            val result = userRepo.doesUserExists("missing")

            assertFalse(result)
        }

    @Test
    fun `WHEN updateUser with existing user THEN updates entity via DAO`() =
        runTest {
            val existingEntity = createFakeUserEntity(id = 20L, wallet = "update-wallet")
            coEvery { dao.getUserById(20L) } returns existingEntity

            val user =
                User(
                    id = 20L,
                    wallet = "update-wallet",
                    numberOfTrees = 0,
                    firstName = "Updated",
                    lastName = "Name",
                    photoPath = "new-photo",
                    isPowerUser = false,
                    unreadMessagesAvailable = false,
                )

            userRepo.updateUser(user)

            coVerify { dao.updateUser(any()) }
        }

    @Test
    fun `WHEN updateUser with nonexistent user THEN no-ops`() =
        runTest {
            coEvery { dao.getUserById(999L) } returns null

            val user =
                User(
                    id = 999L,
                    wallet = "no-wallet",
                    numberOfTrees = 0,
                    firstName = "Ghost",
                    lastName = "User",
                    photoPath = "path",
                    isPowerUser = false,
                    unreadMessagesAvailable = false,
                )

            userRepo.updateUser(user)

            coVerify(exactly = 0) { dao.updateUser(any()) }
        }

    @Test
    fun `WHEN createUser maps tree count from sessions THEN numberOfTrees is correct`() =
        runTest {
            val entity = createFakeUserEntity(id = 3L, wallet = "count-wallet")
            val session1 = FakeFileGenerator.fakeSession.copy(originWallet = "count-wallet").also { it.id = 1L }
            val session2 = FakeFileGenerator.fakeSessionWithEndTime.copy(originWallet = "count-wallet").also { it.id = 2L }

            coEvery { dao.getUserById(3L) } returns entity
            coEvery { dao.getSessionsByUserWallet("count-wallet") } returns listOf(session1, session2)
            coEvery { dao.getTreeCountFromSessionId(1L) } returns 5
            coEvery { dao.getTreeCountFromSessionId(2L) } returns 3
            coEvery { messagesDao.getUnreadMessageCountForWallet("count-wallet") } returns 0

            val result = userRepo.getUser(3L)

            assertNotNull(result)
            assertEquals(8, result.numberOfTrees)
        }
}