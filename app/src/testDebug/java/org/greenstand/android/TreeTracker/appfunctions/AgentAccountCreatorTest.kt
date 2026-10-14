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
package org.greenstand.android.TreeTracker.appfunctions

import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentAccountCreatorTest {
    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    private lateinit var creator: AgentAccountCreator

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(ImageUtils)
        every { ImageUtils.createTestImageFile(any(), any()) } returns File("/test/selfie.jpg")
        coEvery { userRepo.doesUserExists(any()) } returns false
        coEvery { userRepo.getPowerUser() } returns null
        coEvery { userRepo.createUser(any(), any(), any(), any(), any(), any(), any()) } returns 42L
        creator = AgentAccountCreator(context, userRepo)
    }

    @After
    fun tearDown() {
        unmockkObject(ImageUtils)
    }

    @Test
    fun `WHEN no account exists THEN creates a power user with defaults and a generated phone`() =
        runTest {
            val account = creator.createAccount(firstName = null, lastName = null, phone = null, email = null)

            assertEquals(42L, account.userId)
            assertEquals("Test", account.firstName)
            assertEquals("User", account.lastName)
            assertTrue(account.isPowerUser)
            assertTrue(Regex("555\\d{7}").matches(account.wallet), "generated wallet was ${account.wallet}")
            coVerify {
                userRepo.createUser(
                    firstName = "Test",
                    lastName = "User",
                    phone = account.wallet,
                    email = null,
                    wallet = account.wallet,
                    photoPath = "/test/selfie.jpg",
                    isPowerUser = true,
                )
            }
        }

    @Test
    fun `WHEN a power user exists THEN the new account is not a power user`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.fakeUsers.first()

            val account = creator.createAccount("Sam", "Second", phone = "5551234567", email = null)

            assertFalse(account.isPowerUser)
            assertEquals("5551234567", account.wallet)
            coVerify { userRepo.createUser("Sam", "Second", "5551234567", null, "5551234567", any(), false) }
        }

    @Test
    fun `WHEN email is passed THEN it is lowercased and used as the wallet`() =
        runTest {
            val account = creator.createAccount(null, null, phone = null, email = "Ada@Example.com")

            assertEquals("ada@example.com", account.wallet)
            coVerify { userRepo.createUser(any(), any(), null, "ada@example.com", "ada@example.com", any(), any()) }
        }

    @Test
    fun `WHEN an email contains an @ THEN it is accepted, as sign-up does`() =
        runTest {
            val account = creator.createAccount(null, null, phone = null, email = "qa+1@example.com")

            assertEquals("qa+1@example.com", account.wallet)
        }

    @Test
    fun `WHEN the wallet is taken THEN it fails without creating a user`() =
        runTest {
            coEvery { userRepo.doesUserExists("5551234567") } returns true

            assertFailsWith<IllegalArgumentException> {
                creator.createAccount(null, null, phone = "5551234567", email = null)
            }
            coVerify(exactly = 0) { userRepo.createUser(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `WHEN the generated phone is taken THEN another one is tried`() =
        runTest {
            coEvery { userRepo.doesUserExists(any()) } returnsMany listOf(true, false, false)

            val account = creator.createAccount(null, null, phone = null, email = null)

            assertTrue(Regex("555\\d{7}").matches(account.wallet))
            coVerify(exactly = 3) { userRepo.doesUserExists(any()) }
        }

    @Test
    fun `WHEN input is invalid THEN it fails`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { creator.createAccount(null, null, "5551234567", "a@b.com") }
            assertFailsWith<IllegalArgumentException> { creator.createAccount("X1", null, null, null) }
            assertFailsWith<IllegalArgumentException> { creator.createAccount(null, "Y", null, null) }
            assertFailsWith<IllegalArgumentException> { creator.createAccount(null, null, "123", null) }
            assertFailsWith<IllegalArgumentException> { creator.createAccount(null, null, null, "not-an-email") }
            coVerify(exactly = 0) { userRepo.createUser(any(), any(), any(), any(), any(), any(), any()) }
        }
}