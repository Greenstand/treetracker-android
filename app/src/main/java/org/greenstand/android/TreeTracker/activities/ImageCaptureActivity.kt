package org.greenstand.android.TreeTracker.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.TextureView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.usecases.CaptureTreeLocationUseCase
import org.greenstand.android.TreeTracker.utilities.AutoFitPreviewBuilder
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.viewmodels.NewTreeViewModel.Companion.FOCUS_THRESHOLD
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import timber.log.Timber
import java.io.File

class ImageCaptureActivity : AppCompatActivity() {

    private lateinit var viewFinder: TextureView
    private lateinit var imageCaptureButton: ImageButton
    private lateinit var toolbarTitle: TextView
    private val treeCaptureSession by inject<Scope>(named(ValueHelper.TREE_LOCATION_CAPTURE_SESSION))
    private val locationCaptureScope: CaptureTreeLocationUseCase = treeCaptureSession.get(
        named(ValueHelper.TREE_LOCATION_CAPTURE_SCOPE)
    )

    companion object {
        private const val SELFIE_MODE = "SELFIE_MODE"

        fun createIntent(context: Context, selfieMode: Boolean = false): Intent {
            return Intent(context, ImageCaptureActivity::class.java).apply {
                putExtra(SELFIE_MODE, selfieMode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_camera)

        toolbarTitle = findViewById(R.id.toolbar_title)
        viewFinder = findViewById(R.id.view_finder)
        imageCaptureButton = findViewById(R.id.capture_button)

        val captureSelfie = intent.extras?.getBoolean(SELFIE_MODE, false) ?: false

        if (captureSelfie) {
            toolbarTitle.text= getString(R.string.take_a_selfie)
        } else {
            toolbarTitle.text = getString(R.string.add_a_tree)
        }

        viewFinder.post { startCamera(captureSelfie) }
    }

    override fun onPause() {
        super.onPause()
        locationCaptureScope.stopLocationCapture()
    }

    override fun onResume() {
        super.onResume()
        locationCaptureScope.startLocationCapture()
    }

    private fun startCamera(captureSelfie: Boolean) {
        val preview = setupPreview(captureSelfie)

        val lensFacing = if (captureSelfie) CameraX.LensFacing.FRONT else CameraX.LensFacing.BACK

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
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
                                             val focusMetric = testFocusQuality(file)

                                             val data = Intent().apply {
                                                putExtra(ValueHelper.TAKEN_IMAGE_PATH, file.absolutePath)
                                                 // if we can't trust the focus metric (it will be null), because of problems
                                                 // generating the metric, we set the quality to "good" to
                                                 // avoid false positives. Ultimately, some research would be needed
                                                 // into determining the root cause of the false positives, but
                                                 // that will be a future effort.
                                                 if (focusMetric == null) {
                                                     putExtra(ValueHelper.FOCUS_METRIC_VALUE, FOCUS_THRESHOLD)
                                                 }
                                                 else {
                                                     putExtra(ValueHelper.FOCUS_METRIC_VALUE, focusMetric)
                                                 }
                                             }

                                             setResult(Activity.RESULT_OK, data)
                                             finish()
                                         }
                                     })
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun setupPreview(captureSelfie: Boolean): Preview {

        val lensFacing = if (captureSelfie) CameraX.LensFacing.FRONT else CameraX.LensFacing.BACK

        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(lensFacing)
            .build()

        return AutoFitPreviewBuilder.build(previewConfig, viewFinder)
    }

    // Return either the focus metric, or, in the case of an
    // exception return null.
    private fun testFocusQuality(imageFile: File): Double? {
        try {
            // metric only cares about luminance.
            // for memory limitations, and performance and metric consistency,
            // the image is 200 pixels wide.
            val grayImage = ImageUtils.getGrayPixelFromBitmap(imageFile.absolutePath, 200)
            if (grayImage.isNullOrEmpty()) {
                Timber.d("Failed to create Grayscale Image.")
                return null
            }
            val metric = ImageUtils.brennersFocusMetric(grayImage)
            return metric
        } catch (e: java.lang.Exception) {
            Timber.d("Unable to get focus metric.")
            Timber.d(e.message)
        }
        // on an error, we return null.
        return null
    }
}