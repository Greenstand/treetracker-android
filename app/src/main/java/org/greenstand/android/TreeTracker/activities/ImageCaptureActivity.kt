package org.greenstand.android.TreeTracker.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.AutoFitPreviewBuilder
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer

class ImageCaptureActivity : AppCompatActivity() {

    private lateinit var viewFinder: TextureView
    private lateinit var imageCaptureButton: ImageButton

    companion object {
        private val SELFIE_MODE = "SELFIE_MODE"

        fun createIntent(context: Context, selfieMode: Boolean = true): Intent {
            return Intent(context, ImageCaptureActivity::class.java).apply {
                putExtra(SELFIE_MODE, selfieMode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_camera)

        viewFinder = findViewById(R.id.view_finder)
        imageCaptureButton = findViewById(R.id.capture_button)

        val bundle = intent.extras

        val captureSelfie = bundle?.getBoolean(SELFIE_MODE, false) ?: false

        viewFinder.post { startCamera(captureSelfie) }
    }

    private fun startCamera(captureSelfie: Boolean) {
        val preview = setupPreview(captureSelfie)

        val lensFacing = if (captureSelfie) CameraX.LensFacing.FRONT else CameraX.LensFacing.BACK

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            //.setTargetAspectRatio(Rational(1, 1))
            //.setTargetResolution(Size(800, 800))
            .setLensFacing(lensFacing)
            .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            .build()

        val file = ImageUtils.createImageFile(this)

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        imageCaptureButton.setOnClickListener {
            imageCapture.takePicture(file,
                                     object : ImageCapture.OnImageSavedListener {
                                         override fun onError(imageCaptureError: ImageCapture.ImageCaptureError, message: String, cause: Throwable?) {
                                             Timber.d("FAILURE")
                                             val msg = "Photo capture failed: $message"
                                             Timber.e("CameraXApp", msg)
                                             cause?.printStackTrace()
                                         }

                                         override fun onImageSaved(file: File) {
                                             Timber.d("SUCCESS")
                                             val msg = "Photo capture succeeded: ${file.absolutePath}"
                                             Timber.d("CameraXApp", msg)
                                             val imageQuality = testFocusQuality(file)

                                             val data = Intent().apply {
                                                putExtra(ValueHelper.TAKEN_IMAGE_PATH, file.absolutePath)
                                                putExtra(ValueHelper.FOCUS_METRIC_VALUE, imageQuality)
                                             }

                                             setResult(Activity.RESULT_OK, data)
                                             finish()
                                         }
                                     })
        }

//        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
//            //.setTargetResolution(Size(1280, 720))
//            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
//            .build()
//        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
//
//        imageAnalysis.setAnalyzer({ image: ImageProxy, rotationDegrees: Int ->
//                                      val buffer = image.planes[0].buffer
//                                      val imageByteArray = buffer.toByteArray()
//                                      val data = cropByteArray(imageByteArray, Rect(0, 0, 500, 500))
//                                  })

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun setupPreview(captureSelfie: Boolean): Preview {

        val lensFacing = if (captureSelfie) CameraX.LensFacing.FRONT else CameraX.LensFacing.BACK

        val previewConfig = PreviewConfig.Builder()
            //.setTargetAspectRatio(Rational(1, 1))
            //.setTargetRotation(Surface.ROTATION_0)
            //.setTargetResolution(Size(800, 800))
            .setLensFacing(lensFacing)
            .build()

        val preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)

        return preview
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    private fun testFocusQuality(imageFile: File): Double {
        try {
            // metric only cares about luminance.
            // for memory limitations, and performance and metric consistency,
            // the image is 200 pixels wide.
            val grayImage = ImageUtils.getGrayPixelFromBitmap(imageFile.absolutePath, 200) ?: return 0.0
            val q = ImageUtils.brennersFocusMetric(grayImage)
            println(q)
            return q
        } catch (e: java.lang.Exception) {
            println(e)
        }
        // on an error, we return very bad focus.
        return 0.0;
    }
}

private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
    private var lastAnalyzedTimestamp = 0L



    override fun analyze(image: ImageProxy, rotationDegrees: Int) {

        val buffer = image.planes[0].buffer
        val imageByteArray = buffer.toByteArray()
        val data = cropByteArray(imageByteArray, Rect(0, 0, 500, 500))

    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}

private fun cropByteArray(array : ByteArray, cropRect: Rect): ByteArray {
    val croppedArray = ByteArray(cropRect.width()*cropRect.height())
    val imageWidth = 640
    var i = 0
    array.forEachIndexed { index, byte ->
        val x = index % imageWidth
        val y = index / imageWidth

        if (cropRect.left <= x && x < cropRect.right && cropRect.top <= y && y < cropRect.bottom) {
            croppedArray[i] = byte
            i++
        }
    }
    return croppedArray
}