package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import android.content.ContextWrapper

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

object TreeImage {

    @Throws(IOException::class)
    fun createImageFile(context: Context): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = ValueHelper.JPEG_FILE_PREFIX + timeStamp + "_"
        val directory = context.getDir("treeImages", Context.MODE_PRIVATE)

        return File.createTempFile(imageFileName, ValueHelper.JPEG_FILE_SUFFIX, directory)
    }
}
