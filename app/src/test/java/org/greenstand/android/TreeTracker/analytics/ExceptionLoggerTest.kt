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

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseApp.initializeApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
class ExceptionLoggerTest {
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    @Before
    fun setUp() {
        Timber.uprootAll()
        Timber.plant(ExceptionLogger())

        // Needed to mock Firebase and Crashlytics for testing
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { initializeApp(any()) } returns mockFirebaseApp

        mockkObject(Firebase)
        every { Firebase.crashlytics } returns crashlytics
        clearMocks(crashlytics)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        unmockkObject(Firebase)
        unmockkStatic(FirebaseApp::class)
    }

    @Test
    fun `Timber INFO should breadcrumb but not record to crashlytics`() {
        Timber.tag("ExampleTag").i("Message Body")

        verify(exactly = 1) { crashlytics.log("[ExampleTag]: Message Body") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Timber ERROR with throwable should breadcrumb and record to crashlytics`() {
        val throwable = Throwable("Example Exception")
        Timber.tag("ExampleTag").e(throwable, "Message Body")

        verify(exactly = 1) {
            // The throwable seems to append to the message as well.
            crashlytics.log(
                match {
                    it.contains("[ExampleTag]: Message Body") &&
                        it.contains("java.lang.Throwable: Example Exception")
                },
            )
        }
        verify(exactly = 1) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Timber ERROR without throwable should breadcrumb but not record to crashlytics`() {
        Timber.tag("ExampleTag").e("Message Body")

        verify(exactly = 1) { crashlytics.log("[ExampleTag]: Message Body") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Timber ASSERT should breadcrumb but not record to crashlytics`() {
        Timber.tag("ExampleTag").wtf("Message Body")

        verify(exactly = 1) { crashlytics.log("[ExampleTag]: Message Body") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Timber VERBOSE should not breadcrumb or record to crashlytics`() {
        Timber.tag("ExampleTag").v("Message Body")

        verify(exactly = 0) { crashlytics.log(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Timber DEBUG should not breadcrumb or record to crashlytics`() {
        Timber.tag("ExampleTag").d("Message Body")

        verify(exactly = 0) { crashlytics.log(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }
}