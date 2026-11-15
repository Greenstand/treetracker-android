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

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.View
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import timber.log.Timber
import java.io.File

@Composable
fun Camera(
    isSelfieMode: Boolean = false,
    cameraControl: CameraControl,
    modifier: Modifier = Modifier.fillMaxSize(),
    onImageCaptured: (File) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val context = LocalContext.current
    val previewView =
        remember(context) {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
    val currentOnImageCaptured = rememberUpdatedState(onImageCaptured)

    DisposableEffect(previewView, lifecycleOwner, isSelfieMode, cameraControl) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false
        var cameraProvider: ProcessCameraProvider? = null
        var capture: ImageCapture? = null
        var previewUseCase: Preview? = null

        fun updateRotation() {
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            capture?.targetRotation = rotation
            previewUseCase?.targetRotation = rotation
        }
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val displayListener =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {
                    if (previewView.display?.displayId == displayId) updateRotation()
                }
            }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateRotation() }
        previewView.addOnLayoutChangeListener(layoutListener)
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        cameraProviderFuture.addListener({
            if (!disposed) {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                val preview =
                    if (isSelfieMode) {
                        Preview
                            .Builder()
                            .setTargetResolution(Size(1000, 1000))
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

                capture = imageCapture
                previewUseCase = preview
                updateRotation()
                cameraControl.captureListener = {
                    updateRotation()
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
                                currentOnImageCaptured.value(file)
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

                // Navigation transitions can briefly keep the previous camera entry alive.
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture,
                    preview,
                )

                preview.setSurfaceProvider(previewView.surfaceProvider)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            cameraControl.captureListener = null
            previewView.removeOnLayoutChangeListener(layoutListener)
            displayManager.unregisterDisplayListener(displayListener)
            val useCases = listOfNotNull(previewUseCase, capture)
            cameraProvider?.unbind(*useCases.toTypedArray())
        }
    }
    AndroidView(modifier = modifier, factory = { previewView })
}

class CameraControl {
    var captureListener: (() -> Unit)? = null

    var isImageScalingEnabled: Boolean = false

    var imageScaleHeight: Int = 1920

    fun captureImage() {
        captureListener?.invoke()
    }
}