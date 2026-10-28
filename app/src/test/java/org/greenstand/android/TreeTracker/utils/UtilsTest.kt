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
package org.greenstand.android.TreeTracker.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
class UtilsTest {
    @Test
    fun `runInParallel runs the action for every item`() =
        runTest {
            val processed = Collections.synchronizedList(mutableListOf<Int>())

            listOf(1, 2, 3).runInParallel(StandardTestDispatcher(testScheduler)) { processed.add(it) }

            assertEquals(listOf(1, 2, 3), processed.sorted())
        }

    @Test
    fun `runInParallel on an empty list does nothing`() =
        runTest {
            var calls = 0

            emptyList<Int>().runInParallel(StandardTestDispatcher(testScheduler)) { calls++ }

            assertEquals(0, calls)
        }

    @Test
    fun `runInParallel runs the actions concurrently`() =
        runTest {
            listOf(1, 2, 3).runInParallel(StandardTestDispatcher(testScheduler)) { delay(1000) }

            assertEquals(1000L, testScheduler.currentTime)
        }

    @Test
    fun `runInParallel waits for every action before returning`() =
        runTest {
            val finished = Collections.synchronizedList(mutableListOf<Long>())

            listOf(300L, 100L, 200L).runInParallel(StandardTestDispatcher(testScheduler)) {
                delay(it)
                finished.add(it)
            }

            assertEquals(listOf(100L, 200L, 300L), finished.toList())
        }

    @Test
    fun `runInParallel rethrows a failing action`() =
        runTest {
            val result =
                runCatching {
                    listOf(1, 2, 3).runInParallel(StandardTestDispatcher(testScheduler)) {
                        if (it == 2) error("boom")
                    }
                }

            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertEquals("boom", result.exceptionOrNull()?.message)
        }
}