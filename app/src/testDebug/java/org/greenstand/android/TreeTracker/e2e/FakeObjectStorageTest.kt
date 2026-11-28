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
package org.greenstand.android.TreeTracker.e2e

import com.amazonaws.services.s3.model.PutObjectRequest
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeObjectStorageTest {
    private val request = PutObjectRequest("bucket", "key", File("photo.jpg"))

    @Test
    fun `WHEN uploading THEN it succeeds without a network`() {
        assertNotNull(FakeObjectStorage.create(delayMs = 0).putObject(request))
    }

    @Test
    fun `WHEN the fake is slow THEN each upload waits for the delay`() {
        val s3 = FakeObjectStorage.create(delayMs = 150)

        val elapsed = measureTimeMillis { s3.putObject(request) }

        assertTrue(elapsed >= 150, "upload took ${elapsed}ms")
    }

    @Test
    fun `WHEN anything but an upload is called THEN it fails loudly`() {
        assertFailsWith<UnsupportedOperationException> { FakeObjectStorage.create(delayMs = 0).listBuckets() }
    }

    @Test
    fun `WHEN the fake is inspected THEN Object methods still work`() {
        val s3 = FakeObjectStorage.create(delayMs = 0)

        assertEquals(s3, s3)
        assertEquals(s3.hashCode(), s3.hashCode())
        assertEquals("FakeObjectStorage", s3.toString())
    }

    @Test
    fun `WHEN the mode is ok THEN there is no delay`() {
        assertEquals(0L, FakeObjectStorage.delayFor("ok", delayMs = null))
    }

    @Test
    fun `WHEN the mode is slow THEN it uses the given delay or a default`() {
        assertEquals(250L, FakeObjectStorage.delayFor("slow", delayMs = 250))
        assertEquals(1000L, FakeObjectStorage.delayFor("slow", delayMs = null))
    }

    @Test
    fun `WHEN the mode is unknown THEN it is rejected`() {
        assertFailsWith<IllegalArgumentException> { FakeObjectStorage.delayFor("offline", delayMs = null) }
    }
}