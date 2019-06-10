package org.greenstand.android.TreeTracker.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

object ImageUtils {

    @SuppressLint("SimpleDateFormat")
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
        val requiredWidth = (500 * density).toInt()

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
            exif = ExifInterface(photoPath!!)
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
        matrix.setRotate(rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                bitmap.height.toFloat() / 2)
        return Bitmap.createBitmap(bitmap, 0, 0,
                bmOptions.outWidth, bmOptions.outHeight, matrix, true)
    }

    /**
     * out put image data pixels to console
     */
    fun printImage(image: Array<Array<Int>> ) {
        var counter = 0.0
        val rows = image.lastIndex + 1
        val cols = image.get(0).lastIndex + 1
        println()
        for (r in 0 until rows)
        {
            for (c in 0 until cols) {
                print(image[r][c])
                print(",")

            }
            print(counter)
            counter++
            println()
            System.out.flush()
        }
    }

    /**
     * out put image data pixels to console
     */
     fun printImage(image: IntArray,rows:Int,cols:Int ) {
        var counter = 0.0

        println()
        for (r in 0 until rows)
        {
            for (c in 0 until cols) {
                print(image[(r * cols) + cols])
                print(",")

            }
            print(counter)
            counter++
            println()
            System.out.flush()
        }
    }

    fun getGrayPixelFromBitmap(imagePath: String?, scaleToWidth: Int):  Array<Array<Int>>? {

        /* Get the size of the image */
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, bmOptions)
        val imageWidth = bmOptions.outWidth




        var sampleSize = Math.ceil((imageWidth.toFloat() / scaleToWidth.toFloat()).toDouble()).toInt()
        // If the original image is smaller than required, don't sample
        if (sampleSize < 1) {
            sampleSize = 1
        }


        bmOptions.inSampleSize = sampleSize

        bmOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bmOptions.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(imagePath, bmOptions) ?: return null

        val rows = bitmap.height
        val cols = bitmap.width
        val bc = bitmap.byteCount
        val pix = IntArray(rows * cols)
        bitmap.getPixels(pix, 0, cols, 0, 0, cols, rows)

        var img = Array(rows) { Array(cols) { 0 } }
        // need to get a grid to calculate gradients.
        var index = 0;
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                img[r][c] = pix[index++]
            }
        }

        val gimg = ImageUtils.getGrayPixels(img)
       return gimg
    }

    /**
     * blur and image with gaussian kernel with random width between 0 (no blur) and 10.
     */
    public fun generateRandomBlurredImage(image: Array<Array<Int>>) : Array<Array<Int>>{


        var ran = kotlin.random.Random(System.currentTimeMillis()).nextInt(0,10);
        println(ran)
        if (ran > 0) {
           return ImageUtils.applyGaussianKernel(image, ran, 2.0)
        }
        return image
    }
    /**
     *  Get grayscale image using standard formula.
     */
    private fun getGrayPixels(image: Array<Array<Int>>): Array<Array<Int>> {
        val rows = image.lastIndex + 1;
        val cols = image.get(0).lastIndex + 1;

        var result = Array(rows) { Array(cols) { 0 } }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                var blueElement = image[r][c] and 0x000000FF
                var greenElement = (image[r][c] and 0x0000FF00) shr 8
                var redElement = (image[r][c] and 0x00FF0000) shr 16

                val gray = (0.2990 * redElement + 0.5870 * greenElement + 0.1140 * blueElement)
                result[r][c] = gray.toInt()
            }
        }
        return result
    }

    /**
     * make a gaussian kernel
     */
    fun gaussian(width: Int, sigma: Double): Pair<Array<Array<Double>>, Double> {

        var result = Array<Array<Double>>(width) { Array<Double>(width) { 0.0 } }

        val center = width / 2
        val sigmaSquared = sigma * sigma
        for (c in 0 until width) {
            for (r in 0 until width) {
                val distanceSquared = (c - center) * (c - center) + (r - center) * (r - center);
                val numerator = kotlin.math.exp(-distanceSquared / (2.0 * sigmaSquared));
                result[r][c] = numerator / (2.0 * kotlin.math.PI * sigmaSquared);
            }
        }
        var normalFactor = 0.0;
        for (c in 0 until width) {
            for (r in 0 until width) {
                normalFactor = normalFactor + result[r][c]
            }
        }

        return Pair(result, normalFactor)
    }

    /**
     *  correlation (or convolution since the kernel should be symmetrical)
     */
    fun performCorrelation(image: Array<Array<Int>>, kernel: Array<Array<Double>>,pixRow:Int,pixCol: Int,kernelWidth: Int,normal: Double) : Double {
        val rowOffset = kernelWidth / 2;
        val colOffset = kernelWidth / 2;
        var sum = 0.0;
        var computedPixRow = pixRow - rowOffset;
        for (kr in 0 until kernelWidth) {
            var computedPixCol = pixCol - colOffset;
            for (kc in 0 until kernelWidth) {
                val pix = image[computedPixRow][computedPixCol]
                sum = sum + pix  * kernel[kr][kc]
                computedPixCol++
            }
            computedPixRow++
        }
        return sum / normal
    }

    /**
     *  To test the focus detection, we need to blur some images.
     */

    fun applyGaussianKernel(image: Array<Array<Int>>, kWidth : Int,sigma: Double): Array<Array<Int>> {
        val imRows = image.lastIndex + 1;
        val imCols = image.get(0).lastIndex + 1;

        var result = Array<Array<Int>>(imRows) { Array<Int>(imCols) { 0 } }
        val kernelAndNorm  = gaussian(kWidth,sigma)
        val rowStart = kWidth / 2
        val rowEnd = imRows - rowStart
        val colStart = kWidth / 2
        val colEnd = imCols - colStart

        for (r in rowStart until rowEnd){
            for (c in colStart until colEnd){
                result[r][c] = performCorrelation(image,kernelAndNorm.first,r,c,kWidth,kernelAndNorm.second).toInt()
            }
        }
        return result
    }

    /**
     * // Compute average of sum of squares of the gradient in H and V directions.
     */
    fun brennersFocusMetric(input: Array<Array<Int>>): Double {

        val rows = input.lastIndex + 1;
        val cols = input.get(0).lastIndex + 1;

        var verticalGradients = Array(rows) { Array(cols) { 0 } }
        var horizontalGradients = Array(rows) { Array(cols) { 0 } }
        for (row in 0 until rows) {
            for (col in 0 until cols - 2) {
                val grad = input[row][col + 2] - input[row][col];
                horizontalGradients[row][col] = grad;
            }
        }

        for (row in 0 until rows - 2) {
            for (col in 0 until cols) {
                val grad = input[row + 2][col] - input[row][col];
                verticalGradients[row][col] = grad;
            }
        }

        var sum = 0;
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val horizontalGradientAtRowColumn = horizontalGradients[row][col];
                val verticalGradientAtRowColumn = verticalGradients[row][col];
                if (kotlin.math.abs(horizontalGradientAtRowColumn) > kotlin.math.abs(verticalGradientAtRowColumn)) {
                    sum += horizontalGradientAtRowColumn * horizontalGradientAtRowColumn
                } else {
                    sum += verticalGradientAtRowColumn * verticalGradientAtRowColumn
                }
            }
        }
        return sum / (rows * cols).toDouble();
    }

}
