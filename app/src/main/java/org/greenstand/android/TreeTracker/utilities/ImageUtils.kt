package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import android.widget.Toast
import timber.log.Timber

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

object ImageUtils {

    @Throws(IOException::class)
    fun createImageFile(context: Context): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = ValueHelper.JPEG_FILE_PREFIX + timeStamp + "_"
        val directory = context.getDir("treeImages", Context.MODE_PRIVATE)

        return File.createTempFile(imageFileName, ValueHelper.JPEG_FILE_SUFFIX, directory)
    }

    fun decodeBitmap(photoPath: String?, density: Float): Bitmap? {


        /* Get the size of the image */
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoPath, bmOptions)
        val imageWidth = bmOptions.outWidth

        // Calculate your sampleSize based on the requiredWidth and
        // originalWidth
        // For e.g you want the width to stay consistent at 500dp
        val requiredWidth = (500 * density!!).toInt()

        var sampleSize = Math.ceil((imageWidth.toFloat() / requiredWidth.toFloat()).toDouble()).toInt()

        Timber.d("sampleSize " + Integer.toString(sampleSize))
        // If the original image is smaller than required, don't sample
        if (sampleSize < 1) {
            sampleSize = 1
        }

        Timber.d("sampleSize 2 " + Integer.toString(sampleSize))
        bmOptions.inSampleSize = sampleSize
        bmOptions.inPurgeable = true
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565
        bmOptions.inJustDecodeBounds = false

        /* Decode the JPEG file into a Bitmap */
        val bitmap = BitmapFactory.decodeFile(photoPath, bmOptions) ?: return null


        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(photoPath)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        val orientString = exif!!.getAttribute(ExifInterface.TAG_ORIENTATION)
        val orientation = if (orientString != null)
            Integer.parseInt(orientString)
        else
            ExifInterface.ORIENTATION_NORMAL
        var rotationAngle = 0
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
            rotationAngle = 90
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            rotationAngle = 180
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
            rotationAngle = 270

        Timber.d("rotationAngle " + Integer.toString(rotationAngle))

        val matrix = Matrix()
        matrix.setRotate(rotationAngle.toFloat(), bitmap!!.width.toFloat() / 2,
                bitmap.height.toFloat() / 2)
        return Bitmap.createBitmap(bitmap, 0, 0,
                bmOptions.outWidth, bmOptions.outHeight, matrix, true)
    }
}
