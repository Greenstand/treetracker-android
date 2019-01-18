package org.greenstand.android.TreeTracker.activities


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import kotlinx.android.synthetic.main.camera_preview.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.camera.CameraPreview
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class CameraActivity : Activity(), Camera.PictureCallback, View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private val TAG = "Camera activity"
    private var mCurrentPhotoPath: String? = null
    private var mImageView: ImageView? = null
    private var captureButton: ImageButton? = null
    private var tmpImageFile: File? = null
    private var safeToTakePicture = true

    private var operationAttempt: Job? = null

    private var captureSelfie: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_preview)

        mImageView = cameraPreviewTaken
        captureButton = buttonCapture

        // Add a listener to the buttons
        captureButton!!.setOnClickListener(this@CameraActivity)

        if(intent.extras != null) {
            captureSelfie = intent.extras.getBoolean(ValueHelper.TAKE_SELFIE_EXTRA, false)
        }
    }

    override fun onStart() {
        super.onStart()
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
            mPreview = CameraPreview(this@CameraActivity, mCamera)

            camera_preview.removeAllViews()
            camera_preview.addView(mPreview)
            captureButton?.visibility = View.VISIBLE
        }
    }
    /** Check if this device has a camera  */
    private fun checkCameraHardware(context: Context): Boolean {
        return if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            true
        } else {
            // no camera on this device
            false
        }
    }

    override fun onPictureTaken(data: ByteArray, camera: Camera) {
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

        if(captureSelfie){

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
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(tmpImageFile!!.absolutePath, bmOptions)
        val imageWidth = bmOptions.outWidth

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
        bmOptions.inSampleSize = sampleSize
        bmOptions.inPurgeable = true
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565
        bmOptions.inJustDecodeBounds = false

        /* Decode the JPEG file into a Bitmap */
        val bitmap = BitmapFactory.decodeFile(tmpImageFile!!.absolutePath, bmOptions) ?: return


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
                bmOptions.outWidth, bmOptions.outHeight, matrix, true)

        /* Associate the Bitmap to the ImageView */
        mImageView?.setImageBitmap(rotatedBitmap)
        mImageView?.visibility = View.VISIBLE
    }


    override fun onClick(v: View) {
        v.isHapticFeedbackEnabled = true
        // get an image from the camera
        if (safeToTakePicture && mCamera != null) {     //check mCamera isn't null to avoid error
            safeToTakePicture = false
            mCamera?.takePicture(null, null, this@CameraActivity)
            Timber.d("take pic")
        }
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




    companion object {

        val MEDIA_TYPE_IMAGE = 1



        /** Create a file Uri for saving an image or video  */
        private fun getOutputMediaFileUri(type: Int): Uri {
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



