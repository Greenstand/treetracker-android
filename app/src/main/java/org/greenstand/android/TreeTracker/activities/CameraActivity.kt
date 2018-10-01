package org.greenstand.android.TreeTracker.activities


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture

import android.hardware.Camera
import android.hardware.camera2.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageButton

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.camera.CameraPreview
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ImageUtils.createImageFile
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper

import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity: AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private val TAG = "Camera activity"
    private var mCurrentPhotoPath: String? = null

    //
    private var mTextureView: TextureView? = null

    private var captureButton: ImageButton? = null
    private var tmpImageFile: File? = null
    private var safeToTakePicture = true

    private var operationAttempt: Job? = null

    private var captureSelfie: Boolean = false

    // All camera2 operations need the manager
    private var cameraManager: CameraManager? = null

    // ID for current primary/secondary camera instance
    private var currentCameraID = ""

    // Best preview size for available dimensions
    private var optimalPreviewSize: Size? = null

    //
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    /**
     *
     */
    private var cameraCaptureSessionCallback = object : CameraCaptureSession.StateCallback() {

        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            if (mCameraDevice == null) {
                return
            }
            //
            try {
                val captureRequest = mCameraCaptureRequestBuilder?.build()
                mCameraCaptureSession = cameraCaptureSession
                cameraCaptureSession.setRepeatingRequest(captureRequest, null, backgroundHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

            //
            Timber.i(TAG, "CameraCaptureSession: onConfigured")
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            //
            Timber.e(TAG, "CameraCaptureSession: onConfigurationFailed")
        }
    }


    /**
     *
     */
    private val cameraStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice?) {
            mCameraDevice = camera
            setupPreview()
            Timber.i(TAG, "Camera State: OPENED")
        }

        override fun onClosed(camera: CameraDevice?) {
            //
            Timber.i(TAG, "Camera State: CLOSED")
        }

        override fun onDisconnected(camera: CameraDevice?) {
            //
            mCameraDevice?.close()
            mCameraDevice = null
            Timber.e(TAG, "Camera State: DISCONNECTED")
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            //
            mCameraDevice?.close()
            mCameraDevice = null
            Timber.e(TAG, "Camera State Error Code: $error")
        }
    }

    /**
     *
     */
    private val surfaceTextureListener =  object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            //
            setupCamera()
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {

        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_preview)

        mTextureView = findViewById(R.id.camera_preview_taken) as TextureView
        initCamera2()

        captureButton = findViewById(R.id.button_capture) as ImageButton

        // Listener for the capture button
        captureButton!!.setOnClickListener {
            captureButton?.isHapticFeedbackEnabled = true
            // get an image from the camera
            if (safeToTakePicture && mCamera != null) {     //check mCamera isn't null to avoid error
                safeToTakePicture = false

                val textureBitmap = mTextureView?.bitmap
                var outputPhoto = FileOutputStream(createImageFile(baseContext))
                val result = textureBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputPhoto)

                captureNewImage(convertBitmapToByteArray(textureBitmap!!), mCamera!!)

                Timber.d("take pic")
            }
        }

        if(intent.extras != null) {
            captureSelfie = intent.extras.getBoolean(ValueHelper.TAKE_SELFIE_EXTRA, false)
        }

        operationAttempt?.cancel()
        operationAttempt = launch(UI) {

            Timber.i("Opening Camera")

            captureButton?.visibility = View.INVISIBLE

            while(mCamera == null){
                try {

                    if(captureSelfie) {

                        val numberOfCameras = Camera.getNumberOfCameras()
                        if (numberOfCameras > 1) {
                            mCamera = Camera.open(1)
                        } else {
                            mCamera = Camera.open()
                        }

                    } else {

                        mCamera = Camera.open()

                    }
                } catch (e: Exception) {
                    Timber.d("in use" + e.localizedMessage)
                }
                delay(250)
            }

            //mPreview = CameraPreview(this@CameraActivity, mCamera)
            //val preview = findViewById(R.id.camera_preview) as FrameLayout
            //preview.addView(mPreview)
            captureButton?.visibility = View.VISIBLE
        }

    }

    /**
     *
     */
    private fun setCameraID(manager: CameraManager): String {
        for (cameraID in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraID)
            // We don't use a front facing camera in this sample.
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null &&
                    cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }
            return cameraID
        }
        throw IllegalStateException("Could not set Camera Id")
    }

    /**
     *
     */
    private fun initCamera2() {

        if(!isCameraHardwareAvailable((baseContext))) {
            ActivityCompat.requestPermissions(this, arrayOf<String>(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }

        //
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        currentCameraID = setCameraID(cameraManager!!)
    }

    /**
     *
     */
    private fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Timber.i(TAG, "Opening Camera ...")
                cameraManager?.openCamera(currentCameraID, cameraStateCallback, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    /**
     * Iterates through camera IDs (for each of the camera instance's this device has) and does yada yada with em/ find preview size etc for each here
     */
    private fun setupCamera() {
        try {
            cameraManager?.cameraIdList?.forEach {
                val cameraCharacteristics = cameraManager?.getCameraCharacteristics(it)
                if (cameraCharacteristics?.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    optimalPreviewSize = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)?.get(0)
                    Timber.i(TAG, "Best Preview Size: Width=${optimalPreviewSize?.width} | Height=${optimalPreviewSize?.height}")
                    this.currentCameraID = it
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Invoked after successfully opening camera / preview texture view
     */
     private fun setupPreview() {
        try {
            val surfaceTexture: SurfaceTexture = mTextureView!!.surfaceTexture
            surfaceTexture.setDefaultBufferSize(optimalPreviewSize!!.width, optimalPreviewSize!!.height)
            val previewSurface = Surface(surfaceTexture)
            mCameraCaptureRequestBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mCameraCaptureRequestBuilder?.addTarget(previewSurface)

            mCameraDevice?.createCaptureSession(Collections.singletonList(previewSurface), cameraCaptureSessionCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /** Check if this device has a camera  */
    private fun isCameraHardwareAvailable(context: Context): Boolean {
        return if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            true
        } else {
            // no camera on this device
            false
        }
    }

    /**
     *
     */
    private fun captureNewImage(data: ByteArray, camera: Camera) {
        captureButton!!.visibility = View.INVISIBLE

        try {
            tmpImageFile = File.createTempFile("tmpimage.jpg", null, cacheDir)
        } catch (e: IOException) {
            Timber.d("file not created")
            e.printStackTrace()
        }

        try {
            val fo = FileOutputStream(tmpImageFile!!)
            fo.write(data)
            fo.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        //
        if(captureSelfie) {

            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(tmpImageFile!!.absolutePath)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_270.toString())
                exif.saveAttributes()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        setPic()
        safeToTakePicture = true
        savePicture()      //skip picture preview
        releaseCamera()
    }

    private fun compressImage() {

        if(mCurrentPhotoPath != null) {
            val photo = Utils.resizedImage(mCurrentPhotoPath!!)

            val bytes = ByteArrayOutputStream()
            photo.compress(Bitmap.CompressFormat.JPEG, 70, bytes)

            val f = File(mCurrentPhotoPath!!)
            try {
                f.createNewFile()
                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())
                fo.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()       // release the camera immediately on pause event
    }

    private fun releaseCamera() {
        if (mCamera != null) {
            mCamera?.release()        // release the camera for other applications
            mCamera = null
            Timber.d("camera released")
        }
    }


    private fun setUpPhotoFile(): File {
        val cw = ContextWrapper(applicationContext)
        val f = ImageUtils.createImageFile(cw)
        mCurrentPhotoPath = f.absolutePath

        return f
    }

    private fun setPic() {

        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        /* Get the size of the image */
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(tmpImageFile!!.absolutePath, bitmapOptions)
        val imageWidth = bitmapOptions.outWidth

        // Calculate your sampleSize based on the requiredWidth and
        // originalWidth
        // For e.g you want the width to stay consistent at 500dp
        val requiredWidth = (500 * resources.displayMetrics.density).toInt()

        var sampleSize = Math.ceil((imageWidth.toFloat() / requiredWidth.toFloat()).toDouble()).toInt()

        Timber.d("sampleSize " + Integer.toString(sampleSize))
        // If the original image is smaller than required, don't sample
        if (sampleSize < 1) {
            sampleSize = 1
        }

        Timber.d("sampleSize 2 " + Integer.toString(sampleSize))
        bitmapOptions.inSampleSize = sampleSize
        bitmapOptions.inPurgeable = true
        bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565
        bitmapOptions.inJustDecodeBounds = false

        /* Decode the JPEG file into a Bitmap */
        val bitmap = BitmapFactory.decodeFile(tmpImageFile!!.absolutePath, bitmapOptions) ?: return


        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(tmpImageFile!!.absolutePath)
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


        val matrix = Matrix()
        matrix.setRotate(rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                bitmap.height.toFloat() / 2)
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmapOptions.outWidth, bitmapOptions.outHeight, matrix, true)

        /* Associate the Bitmap to the ImageView */
        Timber.i(TAG, "Successfully formed bitmap for new image capture!")
        //mTextureView?.bitmap = rotatedBitmap
        mTextureView?.visibility = View.VISIBLE
    }

    /**
     * Helper to send / store bitmap
     */
    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        val result = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        Timber.i(TAG, "Result For Converting New Image Bitmap to Byte[]: $result")

        return byteArray
    }

    private fun savePicture() {
        var pictureFile: File?
        try {
            pictureFile = setUpPhotoFile()
            mCurrentPhotoPath = pictureFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            pictureFile = null
            mCurrentPhotoPath = null
        }

        var saved = true
        try {
            val fos = FileOutputStream(pictureFile!!)
            val input: InputStream  = FileInputStream(tmpImageFile)
            val buf = ByteArray(1024)
            var len: Int = input.read(buf)
            while ( len > 0)
            {
                fos.write(buf, 0, len)
                len = input.read(buf)
            }
            fos.close()
            tmpImageFile!!.delete()
            compressImage()
        } catch (e: FileNotFoundException) {
            Timber.d(TAG, "File not found: " + e.message)
            saved = false
        } catch (e: IOException) {
            Timber.d(TAG, "Error accessing file: " + e.message)
            saved = false
        } catch (e: Exception) {
            Timber.d(TAG, "Error accessing file: " + e.message)
            saved = false
        }

        if (saved) {
            val data = Intent()
            data.putExtra(ValueHelper.TAKEN_IMAGE_PATH, mCurrentPhotoPath)
            setResult(Activity.RESULT_OK, data)

        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()
        if (mTextureView?.isAvailable!!) {
            setupCamera()
            openCamera()
        } else {
            // Don't listen unless necessary
            mTextureView?.surfaceTextureListener = surfaceTextureListener
        }
    }

    /**
     *
     */
    private fun startBackgroundThread() {
        //
        backgroundThread = HandlerThread("camera_background_thread")
        backgroundThread?.start()
        //
        backgroundHandler = Handler(backgroundThread?.getLooper())
    }

    override fun onStop() {
        super.onStop()

        closeCamera()
        closeBackgroundThread()
    }

    /**
     *
     */
    private fun closeCamera() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession?.close()
            mCameraCaptureSession = null
        }

        if (mCameraDevice != null) {
            mCameraDevice?.close()
            mCameraDevice = null
        }
    }

    /**
     *
     */
    private fun closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread?.quitSafely()
            backgroundThread = null
            backgroundHandler = null
        }
    }



    companion object {

        //
        var mCameraCaptureRequestBuilder: CaptureRequest.Builder? = null

        //
        var mCameraCaptureSession: CameraCaptureSession? = null

        //
        var mCameraDevice: CameraDevice? = null


        const val MEDIA_TYPE_IMAGE = 1
        const val CAMERA_REQUEST_CODE  = 123


        /** Create a file Uri for saving an image or video  */
        private fun ƒgetOutputMediaFileUri(type: Int): Uri {
            return Uri.fromFile(getOutputMediaFile(type))
        }

        /** Create a File for saving an image or video  */
        private fun getOutputMediaFile(type: Int): File? {
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.

            val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "MyCameraApp")
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Timber.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }

            // Create a media file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val mediaFile: File
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = File(mediaStorageDir.path + File.separator +
                        "IMG_" + timeStamp + ".jpg")
            } else {
                return null
            }
            return mediaFile
        }
    }
}



