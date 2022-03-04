package org.greenstand.android.TreeTracker.camera

import android.util.DisplayMetrics
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import timber.log.Timber
import java.io.File

@Composable
fun Camera(
    isSelfieMode: Boolean = false,
    cameraControl: CameraControl,
    modifier: Modifier = Modifier.fillMaxSize(),
    onImageCaptured: (File) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).also { previewView ->
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
                    val screenSize = Size(metrics.widthPixels, metrics.widthPixels)

                    val preview = if (isSelfieMode) {
                        Preview.Builder()
                            .setTargetResolution(screenSize)
                            .build()
                    } else {
                        Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .build()
                    }

                    val imageCapture = if (isSelfieMode) {
                        ImageCapture.Builder()
                            .setTargetResolution(screenSize)
                            .build()
                    } else {
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            //.setTargetResolution(Size(800, 800))
                            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                            .build()
                    }



                    cameraControl.captureListener = {

                        val file = ImageUtils.createImageFile(context)

                        val metadata = ImageCapture.Metadata().apply {
                            // Mirror image when using the front camera
                            isReversedHorizontal = isSelfieMode
                        }

                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
                            .setMetadata(metadata)
                            .build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(previewView.context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    ImageUtils.resizeImage(file.absolutePath, isSelfieMode)
                                    Timber.tag("CameraXApp")
                                        .d("Photo capture succeeded: ${file.absolutePath}")
                                    onImageCaptured(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Timber.tag("CameraXApp")
                                        .e("Photo capture failed: ${exception.localizedMessage}")
                                    exception.printStackTrace()
                                }
                            })
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(
                            if (isSelfieMode) CameraSelector.LENS_FACING_FRONT
                            else CameraSelector.LENS_FACING_BACK
                        )
                        .build()


                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector,  imageCapture, preview
                    )

                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }, ContextCompat.getMainExecutor(previewView.context))
            }
        },
    )
}

class CameraControl {

    var captureListener: (() -> Unit)? = null

    fun captureImage() {
        captureListener?.invoke()
    }
}
