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
import com.amazonaws.AmazonClientException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class UploadImageUseCaseTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var objectStorageClient: ObjectStorageClient

    private lateinit var uploadImageUseCase: UploadImageUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        uploadImageUseCase = UploadImageUseCase(doSpaces = objectStorageClient)
    }

    @Test
    fun `WHEN execute called successfully THEN returns URL from client`() = runTest {
        val expectedUrl = "https://bucket.s3.amazonaws.com/image.jpg"
        every { objectStorageClient.put("/photos/tree.jpg", 1.0, 2.0) } returns expectedUrl

        val params = UploadImageParams(
            imagePath = "/photos/tree.jpg",
            lat = 1.0,
            long = 2.0,
        )

        val result = uploadImageUseCase.execute(params)

        assertEquals(expectedUrl, result)
    }

    @Test
    fun `WHEN execute called and AmazonClientException occurs THEN returns null`() = runTest {
        every { objectStorageClient.put(any(), any(), any()) } throws AmazonClientException("Network error")

        val params = UploadImageParams(
            imagePath = "/photos/tree.jpg",
            lat = 1.0,
            long = 2.0,
        )

        val result = uploadImageUseCase.execute(params)

        assertNull(result)
    }
}
