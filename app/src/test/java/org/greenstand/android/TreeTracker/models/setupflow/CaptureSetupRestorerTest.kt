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
package org.greenstand.android.TreeTracker.models.setupflow

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureSetupRestorerTest {
    private val preferences = mockk<Preferences>(relaxed = true)
    private val editor = mockk<Preferences.Editor>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val setupData = mockk<CaptureSetupData>(relaxed = true)
    private val user = FakeFileGenerator.fakeUsers.first()
    private val restorer = CaptureSetupRestorer(preferences, userRepo)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { CaptureSetupScopeManager.getData() } returns setupData
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `an open scope is left alone`() {
        every { CaptureSetupScopeManager.isOpen } returns true

        assertFalse(restorer.reopenIfClosed())

        verify(exactly = 0) { CaptureSetupScopeManager.open() }
    }

    @Test
    fun `a closed scope is reopened`() {
        every { CaptureSetupScopeManager.isOpen } returns false

        assertTrue(restorer.reopenIfClosed())

        verify { CaptureSetupScopeManager.open() }
    }

    @Test
    fun `the user of an open setup is saved for a later restore`() =
        runTest {
            every { setupData.user } returns user

            assertEquals(user, restorer.loadUser(restored = false))

            verify { editor.putLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, user.id) }
            coVerify(exactly = 0) { userRepo.getUser(any()) }
        }

    @Test
    fun `a restored setup gets the saved user back`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, any()) } returns user.id
            coEvery { userRepo.getUser(user.id) } returns user

            assertEquals(user, restorer.loadUser(restored = true))

            verify { setupData.user = user }
            verify(exactly = 0) { CaptureSetupScopeManager.close() }
        }

    @Test
    fun `a restored setup whose user is gone is closed`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, any()) } returns user.id
            coEvery { userRepo.getUser(any()) } returns null

            assertNull(restorer.loadUser(restored = true))

            verify { CaptureSetupScopeManager.close() }
            verify(exactly = 0) { editor.putLong(any(), any()) }
        }

    @Test
    fun `an open setup without a user is closed`() =
        runTest {
            every { setupData.user } returns null

            assertNull(restorer.loadUser(restored = false))

            verify { CaptureSetupScopeManager.close() }
        }

    // --- The user leaves a restored step (system back) while its user still reloads ---

    /** Starts a restored [CaptureSetupRestorer.loadUser] that waits on [reload], and returns its outcome. */
    private fun TestScope.restoreWaitingOn(reload: CompletableDeferred<User?>): Deferred<Result<User?>> {
        coEvery { userRepo.getUser(any()) } coAnswers { reload.await() }
        val loading = async { runCatching { restorer.loadUser(restored = true) } }
        runCurrent()
        return loading
    }

    @Test
    fun `a restore whose scope is replaced while the user reloads leaves the new scope alone`() =
        runTest {
            val reload = CompletableDeferred<User?>()
            val loading = restoreWaitingOn(reload)
            // The step underneath (e.g. user select) opened a new setup scope.
            val newData = mockk<CaptureSetupData>(relaxed = true)
            every { CaptureSetupScopeManager.getData() } returns newData

            reload.complete(user)
            val result = loading.await()

            assertTrue(
                result.exceptionOrNull() is CancellationException,
                "loadUser kept going after its setup scope was replaced: $result",
            )
            verify(exactly = 0) { newData.user = any() }
            verify(exactly = 0) { CaptureSetupScopeManager.close() }
        }

    @Test
    fun `a restore whose scope is closed while the user reloads gives up instead of crashing`() =
        runTest {
            val reload = CompletableDeferred<User?>()
            val loading = restoreWaitingOn(reload)
            // The step underneath (e.g. Add org without a user) closed the setup scope.
            every { CaptureSetupScopeManager.getData() } throws
                IllegalStateException("CaptureSetupScope not open. Call open() before getData().")

            reload.complete(user)
            val result = loading.await()

            assertTrue(
                result.exceptionOrNull() is CancellationException,
                "loadUser crashed after its setup scope was closed: $result",
            )
            verify(exactly = 0) { editor.putLong(any(), any()) }
        }
}