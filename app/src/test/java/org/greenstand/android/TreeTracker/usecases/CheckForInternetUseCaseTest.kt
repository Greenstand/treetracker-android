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
package org.greenstand.android.TreeTracker.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CheckForInternetUseCaseTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var checkForInternetUseCase: CheckForInternetUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        checkForInternetUseCase = CheckForInternetUseCase()
    }

    @After
    fun tearDown() {
        try {
            unmockkStatic(Runtime::class)
        } catch (_: Exception) {
            // no-op if not mocked
        }
    }

    @Test
    fun `WHEN execute called and exception occurs THEN returns false`() =
        runTest {
            mockkStatic(Runtime::class)
            every { Runtime.getRuntime() } throws RuntimeException("Mocked runtime failure")

            val result = checkForInternetUseCase.execute(Unit)

            assertFalse(result)
        }

    @Test
    fun `WHEN ping exits with code zero THEN returns true`() =
        runTest {
            val runtime = mockRuntimeWithPingExitCode(0)

            val result = checkForInternetUseCase.execute(Unit)

            assertTrue(result)
            verify { runtime.exec(PING_COMMAND) }
        }

    @Test
    fun `WHEN ping exits with non zero code THEN returns false`() =
        runTest {
            mockRuntimeWithPingExitCode(2)

            val result = checkForInternetUseCase.execute(Unit)

            assertFalse(result)
        }

    @Test
    fun `WHEN ping command cannot be started THEN returns false`() =
        runTest {
            val runtime = mockk<Runtime>()
            every { runtime.exec(PING_COMMAND) } throws IOException("ping not found")
            mockkStatic(Runtime::class)
            every { Runtime.getRuntime() } returns runtime

            val result = checkForInternetUseCase.execute(Unit)

            assertFalse(result)
        }

    private fun mockRuntimeWithPingExitCode(exitCode: Int): Runtime {
        val process = mockk<Process>()
        every { process.waitFor() } returns exitCode
        val runtime = mockk<Runtime>()
        every { runtime.exec(PING_COMMAND) } returns process
        mockkStatic(Runtime::class)
        every { Runtime.getRuntime() } returns runtime
        return runtime
    }

    private companion object {
        const val PING_COMMAND = "ping -c 1 google.com"
    }
}