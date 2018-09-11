package org.greenstand.android.TreeTracker.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.ExifInterface
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicHeader
import org.apache.http.params.HttpConnectionParams
import org.apache.http.protocol.HTTP
import org.json.JSONObject
import timber.log.Timber

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class Utils {

    companion object {

        var httpResponseCode = -1

        /*
     * Sets the font on all TextViews in the ViewGroup. Searches
     * recursively for all inner ViewGroups as well. Just add a
     * check for any other views you want to set as well (EditText,
     * etc.)
     */
        fun setFont(group: ViewGroup, font: Typeface, textSize: Int) {
            val count = group.childCount
            var v: View
            for (i in 0 until count) {
                v = group.getChildAt(i)
                if (v is TextView || v is Button /*etc.*/) {
                    (v as TextView).typeface = font
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
                } else if (v is ViewGroup) {
                    setFont(v, font, textSize)
                }

            }
        }


        /**
         * @param inputStream
         * InputStream which is to be converted into String
         * @return String by encoding the given InputStream (or) <br></br>
         * null if InputStream is null or cannot convert the InputStream.<br></br>
         */
        fun convertStreamToString(inputStream: InputStream): String {
            val sb = StringBuilder()
            try {

                val r = BufferedReader(InputStreamReader(
                        inputStream), 1024 * 8)
                var line: String? = r.readLine()
                while (line != null) {
                    sb.append(line)
                    line = r.readLine()
                }
            } catch (e: Exception) {

            }

            return sb.toString()
        }


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


        fun sendJson(json: JSONObject, where: String) {
            val t = object : Thread() {

                private var rsp: String? = null

                override fun run() {
                    Looper.prepare() //For Preparing Message Pool for the child Thread
                    val client = DefaultHttpClient()
                    HttpConnectionParams.setConnectionTimeout(client.params, 10000) //Timeout Limit
                    val response: HttpResponse?

                    try {
                        val post = HttpPost(where)

                        val se = StringEntity(json.toString())

                        Timber.d("json string " + json.toString())

                        se.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")
                        post.entity = se
                        response = client.execute(post)

                        /*Checking response */
                        if (response != null) {
                            val `in` = response.entity.content //Get the data in the entity

                            Utils.httpResponseCode = response.statusLine.statusCode

                            if (response.statusLine.statusCode == HttpStatus.SC_OK) {
                                rsp = Utils.convertStreamToString(`in`)
                            } else {
                                rsp = Utils.convertStreamToString(`in`)
                            }


                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        //createDialog("Error", "Cannot Estabilish Connection");
                    }

                    Looper.loop() //Loop in the message queue
                }
            }

            t.start()
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
            val imageType = bmOptions.outMimeType

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
            val encodedImage: String
            val byteArrayBitmapStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, compressionQuality,
                    byteArrayBitmapStream)


            return rotatedBitmap

        }

        fun convertDateToTimestamp(str: String): Long {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
            var date: Date? = null
            try {
                date = dateFormat.parse(str)
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
