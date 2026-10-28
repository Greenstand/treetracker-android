/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.camera

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun Camera(
    isSelfieMode: Boolean = false,
    cameraControl: CameraControl,
    modifier: Modifier = Modifier.fillMaxSize(),
    onImageCaptured: (File) -> Unit,
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
                    val screenSize =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val bounds = (previewView.context as Activity).windowManager.currentWindowMetrics.bounds
                            Size(bounds.width(), bounds.width())
                        } else {
                            @Suppress("DEPRECATION")
                            val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
                            Size(metrics.widthPixels, metrics.widthPixels)
                        }

                    val preview =
                        if (isSelfieMode) {
                            Preview
                                .Builder()
                                .setTargetResolution(screenSize)
                                .build()
                        } else {
                            Preview
                                .Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build()
                        }

                    val imageCapture =
                        if (isSelfieMode) {
                            ImageCapture
                                .Builder()
                                .setTargetResolution(Size(1000, 1000))
                                .build()
                        } else {
                            ImageCapture
                                .Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                // .setTargetResolution(Size(800, 800))
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build()
                        }

                    cameraControl.captureListener = {
                        val file = ImageUtils.createImageFile(context)

                        val metadata =
                            ImageCapture.Metadata().apply {
                                // Mirror image when using the front camera
                                isReversedHorizontal = isSelfieMode
                            }

                        val outputOptions =
                            ImageCapture.OutputFileOptions
                                .Builder(file)
                                .setMetadata(metadata)
                                .build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(previewView.context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    ImageUtils.resizeImage(
                                        path = file.absolutePath,
                                        forceScaling = cameraControl.isImageScalingEnabled,
                                        targetHeight = cameraControl.imageScaleHeight,
                                    )
                                    ImageUtils.orientImage(file.absolutePath)
                                    Timber
                                        .tag("CameraXApp")
                                        .d("Photo capture succeeded: ${file.absolutePath}")
                                    onImageCaptured(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Timber
                                        .tag("CameraXApp")
                                        .e(exception, "Photo capture failed")
                                }
                            },
                        )
                    }

                    val cameraSelector =
                        CameraSelector
                            .Builder()
                            .requireLensFacing(
                                if (isSelfieMode) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                },
                            ).build()

                    cameraProvider.unbindAll()
                    val camera =
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageCapture,
                            preview,
                        )

                    // Test, no changes in camera at all despite no exception. So the cameraControl itself isn't working
//                    val future =
//                        camera.cameraControl
//                            .setLinearZoom(
//                                0.1f,
//                            )
//                    future.addListener(
//                        {
//                            try {
//                                future.get()
//                                Timber.d("Zoom applied")
//                            } catch (e: Exception) {
//                                Timber.e(e, "Zoom failed")
//                            }
//                        },
//                        ContextCompat.getMainExecutor(previewView.context),
//                    )
//                    val future2 = camera.cameraControl.setExposureCompensationIndex(3)
//                    future2.addListener(
//                        {
//                            try {
//                                future2.get()
//                                Timber.d("Exposure applied")
//                            } catch (e: Exception) {
//                                Timber.e(e, "Exposure failed")
//                            }
//                        },
//                        ContextCompat.getMainExecutor(previewView.context),
//                    )

                    previewView.setOnTouchListener { view, motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_UP -> {
                                view.performClick()
                                val meteringPoint =
                                    previewView.meteringPointFactory
                                        .createPoint(motionEvent.x, motionEvent.y)
                                val action = FocusMeteringAction.Builder(meteringPoint).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                val result = camera.cameraControl.startFocusAndMetering(action)

                                result.addListener(
                                    {
                                        try {
                                            val isFocusSuccessful = result.get().isFocusSuccessful
                                            Timber.tag("CameraXApp").d("Focus result: $isFocusSuccessful and is focused on (x: ${motionEvent.x}, y: ${motionEvent.y})")
                                        } catch (e: Exception) {
                                            Timber.tag("CameraXApp").d(e, "Focus request was cancelled or failed")
                                        }
                                    },
                                    ContextCompat.getMainExecutor(previewView.context),
                                )
                            }
                        }
                        true
                    }

                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }, ContextCompat.getMainExecutor(previewView.context))
            }
        },
    )
}

class CameraControl {
    var captureListener: (() -> Unit)? = null

    var isImageScalingEnabled: Boolean = false

    var imageScaleHeight: Int = 1920

    fun captureImage() {
        captureListener?.invoke()
    }
}