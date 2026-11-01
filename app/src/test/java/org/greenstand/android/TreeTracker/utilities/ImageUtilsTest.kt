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
package org.greenstand.android.TreeTracker.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.math.PI
import kotlin.math.exp

/**
 * Covers the pure pixel math in [ImageUtils] (blur kernel, correlation and focus metric).
 * The bitmap and file helpers are covered by ImageUtilsBitmapTest.
 */
class ImageUtilsTest {
    private fun image(vararg rows: IntArray): Array<Array<Int>> = Array(rows.size) { r -> rows[r].toTypedArray() }

    private fun uniformImage(
        size: Int,
        value: Int,
    ): Array<Array<Int>> = Array(size) { Array(size) { value } }

    @Test
    fun `gaussian kernel has the requested width`() {
        val (kernel, _) = ImageUtils.gaussian(5, 1.0)

        assertEquals(5, kernel.size)
        kernel.forEach { assertEquals(5, it.size) }
    }

    @Test
    fun `gaussian kernel center is the peak of the 2d gaussian`() {
        val sigma = 1.5
        val (kernel, _) = ImageUtils.gaussian(3, sigma)

        assertEquals(1.0 / (2.0 * PI * sigma * sigma), kernel[1][1], DELTA)
        kernel.flatten().forEach { assertTrue(it <= kernel[1][1]) }
    }

    @Test
    fun `gaussian kernel values fall off with the squared distance from the center`() {
        val (kernel, _) = ImageUtils.gaussian(3, 1.0)

        val peak = 1.0 / (2.0 * PI)
        assertEquals(peak * exp(-0.5), kernel[0][1], DELTA)
        assertEquals(peak * exp(-1.0), kernel[0][0], DELTA)
    }

    @Test
    fun `gaussian kernel is symmetric`() {
        val (kernel, _) = ImageUtils.gaussian(5, 2.0)

        for (r in 0 until 5) {
            for (c in 0 until 5) {
                assertEquals(kernel[r][c], kernel[c][r], DELTA)
                assertEquals(kernel[r][c], kernel[4 - r][4 - c], DELTA)
            }
        }
    }

    @Test
    fun `gaussian normal factor is the sum of the kernel`() {
        val (kernel, normal) = ImageUtils.gaussian(5, 2.0)

        assertEquals(kernel.flatten().sum(), normal, DELTA)
    }

    @Test
    fun `correlation with a single center weight returns that pixel`() {
        val image =
            image(
                intArrayOf(1, 2, 3),
                intArrayOf(4, 5, 6),
                intArrayOf(7, 8, 9),
            )
        val kernel =
            arrayOf(
                arrayOf(0.0, 0.0, 0.0),
                arrayOf(0.0, 1.0, 0.0),
                arrayOf(0.0, 0.0, 0.0),
            )

        assertEquals(5.0, ImageUtils.performCorrelation(image, kernel, 1, 1, 3, 1.0), DELTA)
    }

    @Test
    fun `correlation is the weighted sum divided by the normal factor`() {
        val image =
            image(
                intArrayOf(1, 2, 3),
                intArrayOf(4, 5, 6),
                intArrayOf(7, 8, 9),
            )
        val kernel = Array(3) { Array(3) { 1.0 } }

        assertEquals(5.0, ImageUtils.performCorrelation(image, kernel, 1, 1, 3, 9.0), DELTA)
    }

    @Test
    fun `correlation lines the kernel up with the pixel`() {
        val image =
            image(
                intArrayOf(0, 0, 0, 0),
                intArrayOf(0, 0, 0, 0),
                intArrayOf(0, 0, 0, 0),
                intArrayOf(0, 0, 0, 10),
            )
        // Only the bottom-right kernel cell is weighted, so it reads the pixel below and to the right
        val kernel =
            arrayOf(
                arrayOf(0.0, 0.0, 0.0),
                arrayOf(0.0, 0.0, 0.0),
                arrayOf(0.0, 0.0, 2.0),
            )

        assertEquals(20.0, ImageUtils.performCorrelation(image, kernel, 2, 2, 3, 1.0), DELTA)
    }

    @Test
    fun `gaussian blur of a uniform image keeps the value inside the border`() {
        val blurred = ImageUtils.applyGaussianKernel(uniformImage(5, 100), 3, 1.0)

        for (r in 1..3) {
            for (c in 1..3) {
                assertTrue("pixel $r,$c was ${blurred[r][c]}", blurred[r][c] in 99..100)
            }
        }
    }

    @Test
    fun `gaussian blur fills the border it cannot reach with mid gray`() {
        val blurred = ImageUtils.applyGaussianKernel(uniformImage(5, 0), 3, 1.0)

        for (i in 0 until 5) {
            assertEquals(127, blurred[0][i])
            assertEquals(127, blurred[4][i])
            assertEquals(127, blurred[i][0])
            assertEquals(127, blurred[i][4])
        }
    }

    @Test
    fun `gaussian blur keeps the image size`() {
        val blurred = ImageUtils.applyGaussianKernel(uniformImage(7, 50), 5, 2.0)

        assertEquals(7, blurred.size)
        blurred.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun `random blur keeps the image size`() {
        val blurred = ImageUtils.generateRandomBlurredImage(uniformImage(12, 80))

        assertEquals(12, blurred.size)
        blurred.forEach { assertEquals(12, it.size) }
    }

    @Test
    fun `focus metric of a uniform image is zero`() {
        assertEquals(0.0, ImageUtils.brennersFocusMetric(uniformImage(6, 200)), 0.0)
    }

    @Test
    fun `focus metric squares the horizontal gradient two pixels apart`() {
        val image =
            image(
                intArrayOf(0, 0, 10),
                intArrayOf(0, 0, 10),
                intArrayOf(0, 0, 10),
            )

        // Each row has one gradient of 10 -> 3 * 100 over 9 pixels
        assertEquals(300.0 / 9, ImageUtils.brennersFocusMetric(image), DELTA)
    }

    @Test
    fun `focus metric squares the vertical gradient two pixels apart`() {
        val image =
            image(
                intArrayOf(10, 10, 10),
                intArrayOf(0, 0, 0),
                intArrayOf(0, 0, 0),
            )

        assertEquals(300.0 / 9, ImageUtils.brennersFocusMetric(image), DELTA)
    }

    @Test
    fun `focus metric uses the stronger of the two gradients per pixel`() {
        val image =
            image(
                intArrayOf(0, 0, 30),
                intArrayOf(0, 0, 0),
                intArrayOf(10, 0, 0),
            )

        // pixel 0,0: horizontal 30 beats vertical 10 -> 900; pixel 0,2: vertical -30 -> 900;
        // pixel 2,0: horizontal -10 -> 100
        assertEquals(1900.0 / 9, ImageUtils.brennersFocusMetric(image), DELTA)
    }

    @Test
    fun `blurring lowers the focus metric of a sharp image`() {
        val stripes = Array(10) { Array(10) { c -> if ((c / 2) % 2 == 0) 0 else 255 } }

        val sharp = ImageUtils.brennersFocusMetric(stripes)
        val blurred = ImageUtils.brennersFocusMetric(ImageUtils.applyGaussianKernel(stripes, 3, 1.0))

        assertTrue("blurred $blurred should be below sharp $sharp", blurred < sharp)
    }

    @Test
    fun `printImage prints each row with its index`() {
        val output = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(output, true))
        try {
            ImageUtils.printImage(
                image(
                    intArrayOf(1, 2),
                    intArrayOf(3, 4),
                ),
            )
        } finally {
            System.setOut(originalOut)
        }

        assertEquals(listOf("1,2,0.0", "3,4,1.0"), output.toString().lines().filter { it.isNotEmpty() })
    }

    companion object {
        private const val DELTA = 1e-9
    }
}