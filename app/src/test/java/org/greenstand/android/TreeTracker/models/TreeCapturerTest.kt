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
package org.greenstand.android.TreeTracker.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.location.Convergence
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class TreeCapturerTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var stepCounter: StepCounter

    @MockK(relaxed = true)
    private lateinit var createTreeUseCase: CreateTreeUseCase

    @MockK(relaxed = true)
    private lateinit var deviceOrientation: DeviceOrientation

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    private lateinit var treeCapturer: TreeCapturer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        treeCapturer = TreeCapturer(
            locationDataCapturer = locationDataCapturer,
            stepCounter = stepCounter,
            createTreeUseCase = createTreeUseCase,
            deviceOrientation = deviceOrientation,
            sessionTracker = sessionTracker,
        )
    }

    @Test
    fun `WHEN location coordinates available THEN pinLocation returns true`() = runTest {
        val testUuid = UUID.randomUUID()
        val convergence = mockk<Convergence>(relaxed = true)
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence

        val result = treeCapturer.pinLocation()

        assertTrue(result)
    }

    @Test
    fun `WHEN location not available THEN pinLocation returns false`() = runTest {
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns false

        val result = treeCapturer.pinLocation()

        assertFalse(result)
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN pinLocation not called THEN setImage throws`() = runTest {
        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"

        treeCapturer.setImage(imageFile)
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN no active session THEN setImage throws`() = runTest {
        val testUuid = UUID.randomUUID()
        val convergence = mockk<Convergence>(relaxed = true)
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence
        every { sessionTracker.currentSessionId } returns null

        treeCapturer.pinLocation()

        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"

        treeCapturer.setImage(imageFile)
    }

    @Test
    fun `WHEN preconditions met THEN setImage creates Tree with correct data`() = runTest {
        val testUuid = UUID.randomUUID()
        val sessionId = 42L
        val convergence = mockk<Convergence>(relaxed = true)
        val lonConvergence = ConvergenceStats(mean = -122.0, variance = 0.01, standardDeviation = 0.1)
        val latConvergence = ConvergenceStats(mean = 37.0, variance = 0.01, standardDeviation = 0.1)

        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence
        every { convergence.longitudeConvergence } returns lonConvergence
        every { convergence.latitudeConvergence } returns latConvergence
        every { sessionTracker.currentSessionId } returns sessionId
        every { stepCounter.absoluteStepCount } returns 100
        every { stepCounter.deltaSteps } returns 10
        every { deviceOrientation.rotationMatrixSnapshot } returns null

        treeCapturer.pinLocation()

        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"

        treeCapturer.setImage(imageFile)

        val tree = treeCapturer.currentTree
        assertNotNull(tree)
        assertEquals(testUuid, tree.treeUuid)
        assertEquals(sessionId, tree.sessionId)
        assertEquals("/test/image.jpg", tree.photoPath)
        assertEquals(-122.0, tree.meanLongitude)
        assertEquals(37.0, tree.meanLatitude)
        assertEquals("", tree.content)
    }

    @Test
    fun `WHEN setNote called THEN currentTree content is updated`() = runTest {
        val testUuid = UUID.randomUUID()
        val convergence = mockk<Convergence>(relaxed = true)
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence
        every { sessionTracker.currentSessionId } returns 1L
        every { stepCounter.absoluteStepCount } returns 0
        every { stepCounter.deltaSteps } returns 0
        every { deviceOrientation.rotationMatrixSnapshot } returns null

        treeCapturer.pinLocation()

        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"
        treeCapturer.setImage(imageFile)

        treeCapturer.setNote("Test note")

        assertEquals("Test note", treeCapturer.currentTree?.content)
    }

    @Test
    fun `WHEN addAttribute called THEN attribute is added to currentTree`() = runTest {
        val testUuid = UUID.randomUUID()
        val convergence = mockk<Convergence>(relaxed = true)
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence
        every { sessionTracker.currentSessionId } returns 1L
        every { stepCounter.absoluteStepCount } returns 0
        every { stepCounter.deltaSteps } returns 0
        every { deviceOrientation.rotationMatrixSnapshot } returns null

        treeCapturer.pinLocation()

        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"
        treeCapturer.setImage(imageFile)

        treeCapturer.addAttribute("test_key", "test_value")

        val attributes = treeCapturer.currentTree?.treeCaptureAttributes()
        assertNotNull(attributes)
        assertEquals("test_value", attributes["test_key"])
    }

    @Test
    fun `WHEN saveTree succeeds THEN returns true`() = runTest {
        val testUuid = UUID.randomUUID()
        val convergence = mockk<Convergence>(relaxed = true)
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence
        every { sessionTracker.currentSessionId } returns 1L
        every { stepCounter.absoluteStepCount } returns 0
        every { stepCounter.deltaSteps } returns 0
        every { deviceOrientation.rotationMatrixSnapshot } returns null

        treeCapturer.pinLocation()

        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"
        treeCapturer.setImage(imageFile)

        coEvery { createTreeUseCase.execute(any()) } returns 1L

        val result = treeCapturer.saveTree()

        assertTrue(result)
        coVerify(exactly = 1) { createTreeUseCase.execute(any()) }
    }

    @Test
    fun `WHEN currentTree is null THEN saveTree returns false`() = runTest {
        val result = treeCapturer.saveTree()

        assertFalse(result)
    }

    @Test
    fun `WHEN createTreeUseCase throws THEN saveTree returns false`() = runTest {
        val testUuid = UUID.randomUUID()
        val convergence = mockk<Convergence>(relaxed = true)
        coEvery { locationDataCapturer.isLocationCoordinateAvailable() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns testUuid
        coEvery { locationDataCapturer.convergence() } returns convergence
        every { sessionTracker.currentSessionId } returns 1L
        every { stepCounter.absoluteStepCount } returns 0
        every { stepCounter.deltaSteps } returns 0
        every { deviceOrientation.rotationMatrixSnapshot } returns null

        treeCapturer.pinLocation()

        val imageFile = mockk<File>(relaxed = true)
        every { imageFile.absolutePath } returns "/test/image.jpg"
        treeCapturer.setImage(imageFile)

        coEvery { createTreeUseCase.execute(any()) } throws RuntimeException("DB error")

        val result = treeCapturer.saveTree()

        assertFalse(result)
    }
}
