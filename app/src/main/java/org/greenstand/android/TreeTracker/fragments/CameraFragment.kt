package org.greenstand.android.TreeTracker.fragments

import android.graphics.Matrix
import android.os.Bundle
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import timber.log.Timber
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var viewFinder: TextureView
    private lateinit var imageCaptureButton: ImageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewFinder = view.findViewById(R.id.view_finder)
        imageCaptureButton = view.findViewById(R.id.capture_button)

        startCamera()

    }

    private fun startCamera() {
        val preview = buildPreviewUseCase()

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .setTargetAspectRatio(Rational(1, 1))
            .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            .build()

        val file = ImageUtils.createImageFile(requireContext())

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        imageCaptureButton.setOnClickListener {
            imageCapture.takePicture(file,
                                     object : ImageCapture.OnImageSavedListener {
                                         override fun onError(imageCaptureError: ImageCapture.ImageCaptureError, message: String, cause: Throwable?) {
                                             Timber.d("FAILURE")
                                             val msg = "Photo capture failed: $message"
                                             Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                             Timber.e("CameraXApp", msg)
                                             cause?.printStackTrace()
                                         }

                                         override fun onImageSaved(file: File) {
                                             Timber.d("SUCCESS")
                                             val msg = "Photo capture succeeded: ${file.absolutePath}"
                                             Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                             Timber.d("CameraXApp", msg)
                                         }
                                     })
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun buildPreviewUseCase(): Preview {
        val previewConfig = PreviewConfig.Builder()
            .setTargetAspectRatio(Rational(1, 1))
            .setTargetRotation(Surface.ROTATION_180)
            .setTargetResolution(Size(800, 800))
            .setLensFacing(CameraX.LensFacing.BACK)
            .build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

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
}