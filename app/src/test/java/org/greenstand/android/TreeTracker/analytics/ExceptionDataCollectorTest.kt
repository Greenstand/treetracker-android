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
package org.greenstand.android.TreeTracker.analytics

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExceptionDataCollectorTest {
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    private val analytics = mockk<Analytics>(relaxed = true)
    private val collector = ExceptionDataCollector(crashlytics, analytics)

    @Test
    fun `WHEN IOException THEN network failure`() {
        assertEquals(ExceptionDataCollector.TYPE_NETWORK, ExceptionDataCollector.classify(IOException("No internet")))
    }

    @Test
    fun `WHEN AmazonClientException is caused by an IOException THEN network failure`() {
        // This is what the AWS SDK throws when S3 can't be reached.
        val e = AmazonClientException("Unable to execute HTTP request", UnknownHostException("s3.amazonaws.com"))

        assertEquals(ExceptionDataCollector.TYPE_NETWORK, ExceptionDataCollector.classify(e))
    }

    @Test
    fun `WHEN AmazonServiceException THEN server failure`() {
        assertEquals(ExceptionDataCollector.TYPE_SERVER, ExceptionDataCollector.classify(AmazonServiceException("Access Denied")))
    }

    @Test
    fun `WHEN AmazonClientException has no IOException cause THEN unknown failure`() {
        assertEquals(ExceptionDataCollector.TYPE_UNKNOWN, ExceptionDataCollector.classify(AmazonClientException("Bad credentials")))
    }

    @Test
    fun `WHEN SerializationException THEN parsing failure`() {
        assertEquals(ExceptionDataCollector.TYPE_PARSING, ExceptionDataCollector.classify(SerializationException("JSON error")))
    }

    @Test
    fun `WHEN any other exception THEN unknown failure`() {
        assertEquals(ExceptionDataCollector.TYPE_UNKNOWN, ExceptionDataCollector.classify(IllegalStateException("boom")))
    }

    @Test
    fun `WHEN recording a failure THEN failure_type is set and not cleared`() {
        collector.recordFailure(IOException("No internet"), "Upload failed")

        verify(exactly = 1) { crashlytics.setCustomKey(ExceptionDataCollector.FAILURE_TYPE, ExceptionDataCollector.TYPE_NETWORK) }
        // Crashlytics reads custom keys later, on a background thread, so the key must not be reset.
        verify(exactly = 0) { crashlytics.setCustomKey(ExceptionDataCollector.FAILURE_TYPE, "") }
    }

    @Test
    fun `WHEN recording a failure THEN sends exactly one analytics event with its type`() {
        collector.recordFailure(AmazonServiceException("Access Denied"), "Upload failed")

        verify(exactly = 1) { analytics.uploadFailure(ExceptionDataCollector.TYPE_SERVER) }
    }

    @Test
    fun `WHEN checking an exception THEN wasRecorded is true only after it was recorded`() {
        val recorded = IOException("recorded")
        val other = IOException("other")

        assertFalse(collector.wasRecorded(recorded))
        collector.recordFailure(recorded, "Upload failed")

        assertTrue(collector.wasRecorded(recorded))
        assertFalse(collector.wasRecorded(other))
    }

    @Test
    fun `WHEN a sync is cancelled THEN nothing is recorded`() {
        val cancelled = CancellationException("cancelled")

        collector.recordFailure(cancelled, "Upload failed")

        verify(exactly = 0) { analytics.uploadFailure(any()) }
        verify(exactly = 0) { crashlytics.setCustomKey(ExceptionDataCollector.FAILURE_TYPE, any<String>()) }
        assertFalse(collector.wasRecorded(cancelled))
    }
}