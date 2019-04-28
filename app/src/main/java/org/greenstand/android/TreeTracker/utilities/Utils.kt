package org.greenstand.android.TreeTracker.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.*
import java.security.NoSuchAlgorithmException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class Utils {

    companion object {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        fun computeMD5Hash(password: String): String {

            try {
                // Create MD5 Hash
                val digest = java.security.MessageDigest.getInstance("MD5")
                digest.update(password.toByteArray())
                val messageDigest = digest.digest()

                val MD5Hash = StringBuffer()
                for (i in messageDigest.indices) {
                    var h = Integer.toHexString(0xFF and messageDigest[i].toInt())
                    while (h.length < 2)
                        h = "0$h"
                    MD5Hash.append(h)
                }

                return MD5Hash.toString()

            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }

            return ""

        }

        fun resizedImage(path: String): Bitmap {

            /* There isn't enough memory to open up more than a couple camera photos */
            /* So pre-scale the target bitmap into which the file is decoded */

            /* Get the size of the image */
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, bmOptions)
            val imageHeight = bmOptions.outHeight
            val imageWidth = bmOptions.outWidth

            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(path)
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

            // Calculate your sampleSize based on the requiredWidth and
            // originalWidth
            // For e.g you want the width to stay consistent at 500dp
            var requiredWidth = 800

            if (imageHeight > imageWidth) {
                requiredWidth = 600
            }


            var sampleSize = Math.ceil((imageWidth.toFloat() / requiredWidth.toFloat()).toDouble()).toInt()

            // If the original image is smaller than required, don't sample
            if (sampleSize < 1) {
                sampleSize = 1
            }

            bmOptions.inSampleSize = sampleSize
            bmOptions.inPurgeable = true
            bmOptions.inPreferredConfig = Bitmap.Config.RGB_565
            bmOptions.inJustDecodeBounds = false

            /* Decode the JPEG file into a Bitmap */
            val bitmap = BitmapFactory.decodeFile(path, bmOptions)

            val matrix = Matrix()
            matrix.setRotate(rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                    bitmap.height.toFloat() / 2)
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bmOptions.outWidth, bmOptions.outHeight, matrix, true)


            val compressionQuality = 100
            val byteArrayBitmapStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, compressionQuality,
                    byteArrayBitmapStream)


            return rotatedBitmap

        }

        fun convertDateToTimestamp(str: String): Long {
            var date: Date? = null
            try {
                date = Utils.dateFormat.parse(str)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return date!!.time / 1000
        }

        fun base64Image(path: String): String {

            /* There isn't enough memory to open up more than a couple camera photos */
            /* So pre-scale the target bitmap into which the file is decoded */

            /* Get the size of the image */
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, bmOptions)
            val imageHeight = bmOptions.outHeight
            val imageWidth = bmOptions.outWidth
            val imageType = bmOptions.outMimeType

            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(path)
            } catch (e: IOException) {
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

            // Calculate your sampleSize based on the requiredWidth and
            // originalWidth
            // For e.g you want the width to stay consistent at 500dp
            var requiredWidth = 800

            if (imageHeight > imageWidth) {
                requiredWidth = 600
            }


            var sampleSize = Math.ceil((imageWidth.toFloat() / requiredWidth.toFloat()).toDouble()).toInt()

            // If the original image is smaller than required, don't sample
            if (sampleSize < 1) {
                sampleSize = 1
            }

            bmOptions.inSampleSize = sampleSize
            bmOptions.inPurgeable = true
            bmOptions.inPreferredConfig = Bitmap.Config.RGB_565
            bmOptions.inJustDecodeBounds = false

            /* Decode the JPEG file into a Bitmap */
            val bitmap = BitmapFactory.decodeFile(path, bmOptions)

            val matrix = Matrix()
            matrix.setRotate(rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                    bitmap.height.toFloat() / 2)
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bmOptions.outWidth, bmOptions.outHeight, matrix, true)

            val compressionQuality = 80
            val encodedImage: String
            val byteArrayBitmapStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality,
                    byteArrayBitmapStream)
            val b = byteArrayBitmapStream.toByteArray()
            encodedImage = Base64.encodeToString(b, Base64.DEFAULT)

            return encodedImage


        }

    }

}
