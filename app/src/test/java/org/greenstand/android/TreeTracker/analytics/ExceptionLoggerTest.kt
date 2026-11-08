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
package org.greenstand.android.TreeTracker.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class ExceptionLoggerTest {
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val logger = ExceptionLogger()

    @Before
    fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
    }

    @Test
    fun `verbose and debug logs are not reported`() {
        logger.v("verbose message")
        logger.d("debug message")

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `info logs are reported as exceptions with the message`() {
        val recorded = slot<Throwable>()
        every { crashlytics.recordException(capture(recorded)) } returns Unit

        logger.i("info message")

        assertEquals("info message", recorded.captured.message)
    }

    @Test
    fun `warning logs without a throwable are reported with the message`() {
        val recorded = slot<Throwable>()
        every { crashlytics.recordException(capture(recorded)) } returns Unit

        logger.w("something looks wrong")

        assertEquals(Exception::class.java, recorded.captured.javaClass)
        assertEquals("something looks wrong", recorded.captured.message)
    }

    @Test
    fun `error logs report the original throwable`() {
        val error = IllegalStateException("broken")

        logger.e(error, "failed to save")

        verify(exactly = 1) { crashlytics.recordException(error) }
    }

    @Test
    fun `throwable only error logs report the throwable`() {
        val error = RuntimeException("boom")
        val recorded = slot<Throwable>()
        every { crashlytics.recordException(capture(recorded)) } returns Unit

        logger.e(error)

        assertSame(error, recorded.captured)
    }

    @Test
    fun `wtf logs are reported`() {
        logger.wtf("terrible failure")

        verify(exactly = 1) { crashlytics.recordException(any()) }
    }
}