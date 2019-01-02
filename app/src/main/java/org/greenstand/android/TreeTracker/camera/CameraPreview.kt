package org.greenstand.android.TreeTracker.camera

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import timber.log.Timber
import java.io.IOException


/** A basic Camera preview class  */
class CameraPreview(context: Context, private val camera: Camera?) : SurfaceView(context), SurfaceHolder.Callback {

    private val TAG = "CAMERA PREVIEW"

    // TODO see: https://stackoverflow.com/questions/19577299/android-camera-preview-stretched
    // Below changes are composed from a mix of answers among the above SO post on camera preview

    private var supportedPreviewSizes: List<Camera.Size>? = null
    private var optimalPreviewSize: Camera.Size? = null

    private var distance = 0F

    init {

        if (camera != null) {

            val parameters = camera.parameters
            parameters.setRotation(90)
            camera.parameters = parameters

            setCameraFocusMode()

            // Query for Supported preview sizes and find best fit size for preview
            supportedPreviewSizes = camera.parameters.supportedPreviewSizes

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.

            holder.addCallback(this)
            // deprecated setting, but required on Android versions prior to 3.0
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }

    /**  Find the best size for preview using available screen dimensions */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)

        if(supportedPreviewSizes != null) {
            optimalPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, height, width)
            Timber.i("Found Optimal Camera Preview Size: ${optimalPreviewSize.toString()}")
        }

        if(optimalPreviewSize != null) {
            val ratio : Int
            if(optimalPreviewSize!!.height >= optimalPreviewSize!!.width)
                ratio = optimalPreviewSize!!.height / optimalPreviewSize!!.width
            else
                ratio = optimalPreviewSize!!.width / optimalPreviewSize!!.height

            setMeasuredDimension(width*ratio, height)
        }
    }

    /**
     * Supply device dimensions and device's supported camera preview sizes to find the optimal size and find best ratio for image quality
     */
    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, width: Int, height: Int): Camera.Size? {

        val ASPECT_TOLERANCE = 0.1
        val targetRatio = height.toDouble() / width
        Timber.i("getOptimalPreviewSize:: Target Ratio = $targetRatio")

        if (sizes == null)
            return null

        var optimalSize: Camera.Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.height.toDouble() / size.width
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue

            if (Math.abs(size.height - height) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - height).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE

            for (size in sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - height).toDouble()
                }
            }
        }

        return optimalSize
    }

    private fun setCameraFocusMode() {

        if (null == camera) {
            return
        }

        val parameters = camera.parameters
        val availableFocusModes = parameters.supportedFocusModes

        if (availableFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        } else if (availableFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
        camera.parameters = parameters
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            camera?.setPreviewDisplay(holder)
            camera?.setDisplayOrientation(90)
            camera?.startPreview()
        } catch (e: IOException) {
            Timber.d("Error setting camera preview: " + e.message)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
        this.getHolder().removeCallback(this)
        camera?.release()
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
            camera?.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {

            var cameraParams = camera?.parameters
            cameraParams?.setPreviewSize(optimalPreviewSize!!.width, optimalPreviewSize!!.height)
            camera?.parameters = cameraParams

            camera?.setDisplayOrientation(90)
            camera?.setPreviewDisplay(holder)
            camera?.startPreview()

        } catch (e: Exception) {
            Timber.d("Error starting camera preview: " + e.message)
        }
    }

    /**
     * Adding Pinch To Zoom while taking new tree images
     */
    //TODO: re-enable pinch to zoom.  This introduces a crash on some phones
    // Fatal Exception: java.lang.RuntimeException
    // Camera is being used after Camera.release() was called
    /*
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if(camera.)

        val params = camera?.parameters
        val action = event?.action

        // need more than 1 point of action in touch event
        if (event?.pointerCount!! > 1) {
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                distance = getGestureDistance(event)
            } else if (action == MotionEvent.ACTION_MOVE && params!!.isZoomSupported) {
                camera?.cancelAutoFocus()
                performZoom(event, params)
            }
        }

        return true
    }

    private fun performZoom(event: MotionEvent, params: Camera.Parameters) {
        val maxZoom = params.maxZoom
        var zoom = params.zoom
        val newGestureDistance = getGestureDistance(event)

        if (newGestureDistance > distance) {
            // Set Zoom in increment values here
            if (zoom < maxZoom)
                zoom++
        } else if (newGestureDistance < distance) {
            // Set Zoom out increment values here
            if (zoom > 0)
                zoom--
        }
        distance = newGestureDistance
        params.zoom = zoom
        camera?.parameters = params
    }

    private fun getGestureDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }
    */

}