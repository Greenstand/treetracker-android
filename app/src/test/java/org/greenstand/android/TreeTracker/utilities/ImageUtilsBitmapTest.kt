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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Covers the bitmap and file helpers in [ImageUtils]. Native graphics mode gives real bitmap
 * decoding, encoding and transforms.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ImageUtilsBitmapTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Writes a [width] x [height] image whose left half is red and right half is blue. */
    private fun imageFile(
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        orientation: Int? = null,
    ): File {
        val bitmap = halvesBitmap(width, height)
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val file = tempFolder.newFile("image-${width}x$height-${System.nanoTime()}.$extension")
        FileOutputStream(file).use { bitmap.compress(format, 100, it) }
        if (orientation != null) {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return file
    }

    private fun halvesBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val leftWidth = width / 2
        bitmap.setPixels(IntArray(leftWidth * height) { Color.RED }, 0, leftWidth, 0, 0, leftWidth, height)
        return bitmap
    }

    private fun decodedSize(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth to options.outHeight
    }

    private fun isMostlyRed(pixel: Int) = Color.red(pixel) > 200 && Color.blue(pixel) < 60

    private fun isMostlyBlue(pixel: Int) = Color.blue(pixel) > 200 && Color.red(pixel) < 60

    @Test
    fun `createImageFile creates an empty jpg in the tree images directory`() {
        val file = ImageUtils.createImageFile(context)

        assertTrue(file.exists())
        assertTrue(file.name.startsWith("IMG_"))
        assertTrue(file.name.endsWith(".jpg"))
        assertEquals(context.getDir("treeImages", Context.MODE_PRIVATE), file.parentFile)
    }

    @Test
    fun `createImageFile creates a new file each time`() {
        val first = ImageUtils.createImageFile(context)
        val second = ImageUtils.createImageFile(context)

        assertFalse(first == second)
    }

    @Test
    fun `createTestImageFile copies the bundled test image`() {
        val expected = context.assets.open("testtreeimage.jpg").use { it.readBytes() }

        val file = ImageUtils.createTestImageFile(context)

        assertArrayEquals(expected, file.readBytes())
    }

    @Test
    fun `flip mirrors the bitmap horizontally`() {
        val source = halvesBitmap(8, 2)

        val flipped = ImageUtils.flip(source)!!

        assertEquals(8, flipped.width)
        assertEquals(2, flipped.height)
        assertTrue(isMostlyBlue(flipped.getPixel(0, 0)))
        assertTrue(isMostlyRed(flipped.getPixel(7, 1)))
    }

    @Test
    fun `getGrayPixelFromBitmap converts pixels to luminance`() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.RED)
        bitmap.setPixel(1, 0, Color.GREEN)
        bitmap.setPixel(0, 1, Color.BLUE)
        bitmap.setPixel(1, 1, Color.BLACK)
        val file = tempFolder.newFile("gray.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val gray = ImageUtils.getGrayPixelFromBitmap(file.absolutePath, 2)!!

        // 0.299 R + 0.587 G + 0.114 B, truncated
        assertEquals(listOf(76, 149), gray[0].toList())
        assertEquals(listOf(29, 0), gray[1].toList())
    }

    @Test
    fun `getGrayPixelFromBitmap downsamples to the requested width`() {
        val file = imageFile(8, 4, Bitmap.CompressFormat.PNG)

        val gray = ImageUtils.getGrayPixelFromBitmap(file.absolutePath, 4)!!

        assertEquals(2, gray.size)
        gray.forEach { assertEquals(4, it.size) }
    }

    @Test
    fun `getGrayPixelFromBitmap returns null when the file cannot be decoded`() {
        assertNull(ImageUtils.getGrayPixelFromBitmap(File(tempFolder.root, "missing.jpg").absolutePath, 4))
    }

    @Test
    fun `decodeBitmap returns null when the file cannot be decoded`() {
        assertNull(ImageUtils.decodeBitmap(File(tempFolder.root, "missing.jpg").absolutePath, 1f))
    }

    @Test
    fun `decodeBitmap keeps the size of a small upright image`() {
        val file = imageFile(40, 20)

        val bitmap = ImageUtils.decodeBitmap(file.absolutePath, 1f)!!

        assertEquals(40, bitmap.width)
        assertEquals(20, bitmap.height)
    }

    @Test
    fun `decodeBitmap downsamples images wider than 500dp`() {
        val file = imageFile(1000, 500)

        val bitmap = ImageUtils.decodeBitmap(file.absolutePath, 1f)!!

        assertEquals(500, bitmap.width)
        assertEquals(250, bitmap.height)
    }

    @Test
    fun `decodeBitmap scales the 500dp limit by the screen density`() {
        val file = imageFile(1000, 500)

        val bitmap = ImageUtils.decodeBitmap(file.absolutePath, 2f)!!

        assertEquals(1000, bitmap.width)
        assertEquals(500, bitmap.height)
    }

    @Test
    fun `decodeBitmap rotates images with a 90 degree exif orientation`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_ROTATE_90)

        val bitmap = ImageUtils.decodeBitmap(file.absolutePath, 1f)!!

        assertEquals(20, bitmap.width)
        assertEquals(40, bitmap.height)
    }

    @Test
    fun `decodeBitmap rotates images with a 270 degree exif orientation`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_ROTATE_270)

        val bitmap = ImageUtils.decodeBitmap(file.absolutePath, 1f)!!

        assertEquals(20, bitmap.width)
        assertEquals(40, bitmap.height)
    }

    @Test
    fun `decodeBitmap turns images with a 180 degree exif orientation upside down`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_ROTATE_180)

        val bitmap = ImageUtils.decodeBitmap(file.absolutePath, 1f)!!

        assertEquals(40, bitmap.width)
        assertEquals(20, bitmap.height)
        assertTrue(isMostlyBlue(bitmap.getPixel(5, 10)))
        assertTrue(isMostlyRed(bitmap.getPixel(34, 10)))
    }

    @Test
    fun `orientImage leaves an upright image untouched`() {
        val file = imageFile(40, 20)
        val original = file.readBytes()

        ImageUtils.orientImage(file.absolutePath)

        assertArrayEquals(original, file.readBytes())
    }

    @Test
    fun `orientImage leaves an image with a normal exif orientation untouched`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_NORMAL)
        val original = file.readBytes()

        ImageUtils.orientImage(file.absolutePath)

        assertArrayEquals(original, file.readBytes())
    }

    @Test
    fun `orientImage rewrites a 90 degree image upright`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_ROTATE_90)

        ImageUtils.orientImage(file.absolutePath)

        assertEquals(20 to 40, decodedSize(file))
    }

    @Test
    fun `orientImage rewrites a 270 degree image upright`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_ROTATE_270)

        ImageUtils.orientImage(file.absolutePath)

        assertEquals(20 to 40, decodedSize(file))
    }

    @Test
    fun `orientImage turns a 180 degree image upside down`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_ROTATE_180)

        ImageUtils.orientImage(file.absolutePath)

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        assertEquals(40, bitmap.width)
        assertEquals(20, bitmap.height)
        assertTrue(isMostlyBlue(bitmap.getPixel(5, 10)))
        assertTrue(isMostlyRed(bitmap.getPixel(34, 10)))
    }

    @Test
    fun `orientImage mirrors a horizontally flipped image`() {
        val file = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL)

        ImageUtils.orientImage(file.absolutePath)

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        assertEquals(40, bitmap.width)
        assertEquals(20, bitmap.height)
        assertTrue(isMostlyBlue(bitmap.getPixel(5, 10)))
        assertTrue(isMostlyRed(bitmap.getPixel(34, 10)))
    }

    @Test
    fun `orientImage swaps the size of transposed images`() {
        val transpose = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_TRANSPOSE)
        val transverse = imageFile(40, 20, orientation = ExifInterface.ORIENTATION_TRANSVERSE)

        ImageUtils.orientImage(transpose.absolutePath)
        ImageUtils.orientImage(transverse.absolutePath)

        assertEquals(20 to 40, decodedSize(transpose))
        assertEquals(20 to 40, decodedSize(transverse))
    }

    @Test
    fun `orientImage ignores a missing file`() {
        val missing = File(tempFolder.root, "missing.jpg")

        ImageUtils.orientImage(missing.absolutePath)

        assertFalse(missing.exists())
    }

    @Test
    fun `resizeImage with forced scaling resizes to the target width keeping the aspect ratio`() {
        val file = imageFile(400, 200)

        ImageUtils.resizeImage(file.absolutePath, forceScaling = true, targetHeight = 100)

        assertEquals(100 to 50, decodedSize(file))
    }

    @Test
    fun `resizeImage without forced scaling downsamples towards the target width`() {
        val file = imageFile(400, 200)

        ImageUtils.resizeImage(file.absolutePath, forceScaling = false, targetHeight = 100)

        assertEquals(100 to 50, decodedSize(file))
    }

    @Test
    fun `resizeImage keeps the exif orientation`() {
        val file = imageFile(400, 200, orientation = ExifInterface.ORIENTATION_ROTATE_90)

        ImageUtils.resizeImage(file.absolutePath, forceScaling = true, targetHeight = 100)

        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            ),
        )
    }

    @Test
    fun `base64Image encodes a small landscape image at full size`() {
        val file = imageFile(700, 350)

        val encoded = ImageUtils.base64Image(file.absolutePath)

        assertEquals(700 to 350, decodedBase64Size(encoded))
    }

    @Test
    fun `base64Image downsamples landscape images wider than 800 pixels`() {
        val file = imageFile(1600, 800)

        val encoded = ImageUtils.base64Image(file.absolutePath)

        assertEquals(800 to 400, decodedBase64Size(encoded))
    }

    @Test
    fun `base64Image downsamples portrait images wider than 600 pixels`() {
        val file = imageFile(700, 1400)

        val encoded = ImageUtils.base64Image(file.absolutePath)

        assertEquals(350 to 700, decodedBase64Size(encoded))
    }

    private fun decodedBase64Size(encoded: String): Pair<Int, Int> {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return bitmap.width to bitmap.height
    }
}