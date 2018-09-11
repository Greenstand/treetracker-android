package org.greenstand.android.TreeTracker.camera

import java.io.IOException

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.Parameters
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

import timber.log.Timber

/** A basic Camera preview class  */
class CameraPreview(context: Context, private val mCamera: Camera?) : SurfaceView(context), SurfaceHolder.Callback {
    private val TAG = "CAMERA PREVIEW"

    // TODO see: https://stackoverflow.com/questions/19577299/android-camera-preview-stretched

    init {

        if (mCamera != null) {

            val parameters = mCamera!!.parameters
            parameters.setRotation(90)
            mCamera.parameters = parameters
            //        mCamera.setParameters())
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.

            holder.addCallback(this)
            // deprecated setting, but required on Android versions prior to 3.0
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        }

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera!!.setPreviewDisplay(holder)
            mCamera.setDisplayOrientation(90)
            mCamera.startPreview()
        } catch (e: IOException) {
            Timber.d(TAG, "Error setting camera preview: " + e.message)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (holder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            mCamera!!.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera!!.setPreviewDisplay(holder)
            mCamera.startPreview()

        } catch (e: Exception) {
            Timber.d("Error starting camera preview: " + e.message)
        }

    }
}