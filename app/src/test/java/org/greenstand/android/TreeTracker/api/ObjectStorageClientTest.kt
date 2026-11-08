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
package org.greenstand.android.TreeTracker.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GroupGrantee
import com.amazonaws.services.s3.model.Permission
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.greenstand.android.TreeTracker.BuildConfig
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [35])
class ObjectStorageClientTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val requests = mutableListOf<PutObjectRequest>()

    /** Copied out of each request's stream at upload time, since the client never re-reads it. */
    private val uploadedBodies = mutableListOf<String?>()

    private lateinit var client: ObjectStorageClient

    @Before
    fun setUp() {
        mockkConstructor(AmazonS3Client::class)
        every { anyConstructed<AmazonS3Client>().putObject(any<PutObjectRequest>()) } answers {
            val request = firstArg<PutObjectRequest>()
            requests += request
            uploadedBodies += request.inputStream?.readBytes()?.toString(Charsets.UTF_8)
            PutObjectResult()
        }
        ObjectStorageClient.init(context)
        client = ObjectStorageClient.instance()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `put uploads the image file to the images bucket`() {
        val image = tempFolder.newFile("tree.jpg")

        client.put(image.absolutePath, 1.5, -2.25)

        val request = requests.single()
        assertEquals(BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES, request.bucketName)
        assertEquals(image.absolutePath, request.file.absolutePath)
    }

    @Test
    fun `put builds the key from timestamp, coordinates, a uuid and the file name`() {
        val image = tempFolder.newFile("tree.jpg")

        client.put(image.absolutePath, 1.5, -2.25)

        val key = requests.single().key
        val pattern =
            Regex(
                """\d{4}\.\d{2}\.\d{2}\.\d{2}\.\d{2}\.\d{2}_1\.5_-2\.25_""" +
                    """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_tree\.jpg""",
            )
        assertTrue(pattern.matches(key), "Unexpected key: $key")
    }

    @Test
    fun `put makes the image publicly readable`() {
        val image = tempFolder.newFile("tree.jpg")

        client.put(image.absolutePath, 0.0, 0.0)

        val grants = requests.single().accessControlList.grantsAsList
        assertTrue(grants.any { it.grantee == GroupGrantee.AllUsers && it.permission == Permission.Read })
    }

    @Test
    fun `put returns the public url of the uploaded key`() {
        val image = tempFolder.newFile("tree.jpg")

        val url = client.put(image.absolutePath, 3.0, 4.0)

        val key = requests.single().key
        val expected =
            if (BuildConfig.USE_AWS_S3) {
                "https://${BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES}.s3." +
                    "${BuildConfig.OBJECT_STORAGE_ENDPOINT}.amazonaws.com/$key"
            } else {
                "https://${BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES}.${BuildConfig.OBJECT_STORAGE_ENDPOINT}/$key"
            }
        assertEquals(expected, url)
    }

    @Test
    fun `put gives every upload of the same image a unique key`() {
        val image = tempFolder.newFile("tree.jpg")

        client.put(image.absolutePath, 1.0, 1.0)
        client.put(image.absolutePath, 1.0, 1.0)

        assertNotEquals(requests[0].key, requests[1].key)
    }

    @Test
    fun `put propagates storage client errors`() {
        every { anyConstructed<AmazonS3Client>().putObject(any<PutObjectRequest>()) } throws
            AmazonClientException("network down")
        val image = tempFolder.newFile("tree.jpg")

        assertFailsWith<AmazonClientException> {
            client.put(image.absolutePath, 1.0, 1.0)
        }
    }

    @Test
    fun `uploadBundle uploads the json to the batch uploads bucket`() {
        client.uploadBundle("""{"trees":[]}""", "abc123_trees")

        val request = requests.single()
        assertEquals(BuildConfig.OBJECT_STORAGE_BUCKET_BATCH_UPLOADS, request.bucketName)
        assertEquals("""{"trees":[]}""", uploadedBodies.single())
    }

    @Test
    fun `uploadBundle names the object with a timestamp, a uuid and the bundle id`() {
        client.uploadBundle("{}", "abc123_trees")

        val key = requests.single().key
        val pattern =
            Regex(
                """\d{4}-\d{2}-\d{2}-\d{2}-\d{2}-\d{2}_""" +
                    """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_abc123_trees\.json""",
            )
        assertTrue(pattern.matches(key), "Unexpected key: $key")
    }

    @Test
    fun `uploadBundle sets the content length to the utf8 byte size`() {
        // "é" and "🌳" take more than one byte each in UTF-8
        val json = """{"note":"é🌳"}"""

        client.uploadBundle(json, "bundle")

        assertEquals(
            json.toByteArray(Charsets.UTF_8).size.toLong(),
            requests.single().metadata.contentLength,
        )
        assertEquals(json, uploadedBodies.single())
    }

    @Test
    fun `uploadBundle makes the bundle publicly readable`() {
        client.uploadBundle("{}", "bundle")

        val grants = requests.single().accessControlList.grantsAsList
        assertTrue(grants.any { it.grantee == GroupGrantee.AllUsers && it.permission == Permission.Read })
    }

    @Test
    fun `uploadBundle propagates storage client errors`() {
        every { anyConstructed<AmazonS3Client>().putObject(any<PutObjectRequest>()) } throws
            AmazonClientException("network down")

        assertFailsWith<AmazonClientException> {
            client.uploadBundle("{}", "bundle")
        }
    }

    @Test
    fun `init replaces the shared instance`() {
        val first = ObjectStorageClient.instance()

        ObjectStorageClient.init(context)

        assertNotEquals(first, ObjectStorageClient.instance())
        verify(exactly = 0) { anyConstructed<AmazonS3Client>().putObject(any<PutObjectRequest>()) }
    }
}