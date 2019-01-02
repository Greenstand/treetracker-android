package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.ValueHelper

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

import timber.log.Timber

class NoteFragment : Fragment(), OnClickListener, OnCheckedChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mImageView: ImageView? = null
    private var mCurrentPhotoPath: String? = null
    private val mImageBitmap: Bitmap? = null
    private val fragment: Fragment? = null
    private val bundle: Bundle? = null
    private val fragmentTransaction: FragmentTransaction? = null
    private var userId: Long = 0
    private var mSharedPreferences: SharedPreferences? = null
    private var treeIdStr: String? = null
    private var mTreeIsMissing: Boolean = false

    /* Photo album for this application */
    private val albumName: String
        get() = getString(R.string.album_name)

    private val albumDir: File?
        get() {
            var storageDir: File? = null

            if (Environment.MEDIA_MOUNTED == Environment
                            .getExternalStorageState()) {

                val cw = ContextWrapper(activity!!.applicationContext)
                storageDir = cw.getDir("treeImages", Context.MODE_PRIVATE)

                if (storageDir != null) {
                    if (!storageDir.mkdirs()) {
                        if (!storageDir.exists()) {
                            Timber.d("CameraSample failed to create directory")
                            return null
                        }
                    }
                }

            } else {
                Log.v(getString(R.string.app_name),
                        "External storage is not mounted READ/WRITE.")
            }

            return storageDir
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu!!.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_note, container, false)

        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        (activity!!.findViewById(R.id.toolbar_title) as TextView).setText(R.string.tree_preview)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val extras = arguments

        treeIdStr = extras!!.getString(ValueHelper.TREE_ID)

        mSharedPreferences = (activity as AppCompatActivity).getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)

        userId = mSharedPreferences!!.getLong(ValueHelper.MAIN_USER_ID, -1)

        val saveBtn = v.findViewById(R.id.fragment_note_save) as Button
        saveBtn.setOnClickListener(this@NoteFragment)

        val treeMissingBtn = v
                .findViewById(R.id.fragment_note_tree_missing) as Button
        treeMissingBtn.setOnClickListener(this@NoteFragment)

        val treeMissingChk = v.findViewById(R.id.fragment_note_missing_tree_checkbox) as CheckBox
        treeMissingChk.setOnCheckedChangeListener(this@NoteFragment)

        mImageView = v.findViewById(R.id.fragment_note_image) as ImageView

        val query = "select * from tree " +
                "left outer join location on location._id = tree.location_id " +
                "left outer join tree_photo on tree._id = tree_id " +
                "left outer join photo on photo._id = photo_id where is_outdated = 'N' and tree._id =" + treeIdStr

        val photoCursor = TreeTrackerApplication.getDatabaseManager().queryCursor(query, null)
        photoCursor.moveToFirst()

        do {
            mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"))

            val noImage = v.findViewById(R.id.fragment_note_no_image) as TextView

            if (mCurrentPhotoPath != null) {
                setPic()
                noImage.visibility = View.INVISIBLE
            } else {
                noImage.visibility = View.VISIBLE
            }

            val lat = photoCursor.getString(photoCursor.getColumnIndex("lat"))
            val lon = photoCursor.getString(photoCursor.getColumnIndex("long"))

            MainActivity.mCurrentTreeLocation = Location("") // Empty location
            MainActivity.mCurrentTreeLocation!!.latitude = java.lang.Double.parseDouble(lat)
            MainActivity.mCurrentTreeLocation!!.longitude = java.lang.Double.parseDouble(lon)

        } while (photoCursor.moveToNext())

        return v
    }

    override fun onClick(v: View) {

        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {

            R.id.fragment_note_save ->

                if (mTreeIsMissing) {
                    val builder = AlertDialog.Builder(activity)

                    builder.setTitle(R.string.tree_missing)
                    builder.setMessage(R.string.you_are_about_to_mark_this_tree_as_missing)

                    builder.setPositiveButton(R.string.yes) { dialog, which ->
                        saveToDb()

                        Toast.makeText(activity, "Tree saved", Toast.LENGTH_SHORT)
                                .show()
                        val manager = activity!!.supportFragmentManager
                        val second = manager.getBackStackEntryAt(1)
                        manager.popBackStack(second.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                        dialog.dismiss()
                    }


                    builder.setNegativeButton(R.string.no) { dialog, which ->
                        // Code that is executed when clicking NO

                        dialog.dismiss()
                    }


                    val alert = builder.create()
                    alert.show()
                } else {
                    saveToDb()

                    Toast.makeText(activity, "Tree saved", Toast.LENGTH_SHORT)
                            .show()

                    val manager = activity!!.supportFragmentManager
                    val second = manager.getBackStackEntryAt(1)
                    manager.popBackStack(second.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                }
            R.id.fragment_note_tree_missing -> {
            }
        }//			takePicture();

    }


    private fun takePicture() {
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CAMERA),
                    Permissions.MY_PERMISSION_CAMERA)
        } else {
            val takePictureIntent = Intent(activity, CameraActivity::class.java)
            startActivityForResult(takePictureIntent, ValueHelper.INTENT_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Permissions.MY_PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        }
    }

    @Throws(IOException::class)
    private fun setUpPhotoFile(): File {

        val f = createImageFile()
        mCurrentPhotoPath = f.absolutePath

        return f
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(Date())
        val imageFileName = ValueHelper.JPEG_FILE_PREFIX + timeStamp + "_"
        val albumF = albumDir
        return File.createTempFile(imageFileName,
                ValueHelper.JPEG_FILE_SUFFIX, albumF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {

            mCurrentPhotoPath = data!!.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)

            if (mCurrentPhotoPath != null) {
                (activity!!.findViewById(R.id.fragment_note) as RelativeLayout).visibility = View.VISIBLE
                setPic()
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            //			if (((RelativeLayout)getActivity().findViewById(R.id.fragment_new_tree)).getVisibility() != View.VISIBLE) {
            //				getActivity().getSupportFragmentManager().popBackStack();
            //			}
        }

    }

    private fun saveToDb() {

        var contentValues = ContentValues()

        // location
        contentValues.put("user_id", userId)

        MainActivity.mCurrentLocation!!.accuracy
        contentValues.put("user_id", userId)
        contentValues.put("accuracy",
                java.lang.Float.toString(MainActivity.mCurrentLocation!!.accuracy))
        contentValues.put("lat",
                java.lang.Double.toString(MainActivity.mCurrentLocation!!.latitude))
        contentValues.put("long",
                java.lang.Double.toString(MainActivity.mCurrentLocation!!.longitude))

        val locationId = TreeTrackerApplication.getDatabaseManager().insert("location", null, contentValues)

        Timber.d("locationId " + java.lang.Long.toString(locationId))

        // note
        val content = (activity!!.findViewById(R.id.fragment_note_note) as EditText).text.toString()
        contentValues = ContentValues()
        contentValues.put("user_id", userId)
        contentValues.put("content", content)

        val noteId = TreeTrackerApplication.getDatabaseManager().insert("note", null, contentValues)
        Timber.d("noteId " + java.lang.Long.toString(noteId))


        // tree
        contentValues = ContentValues()
        contentValues.put("location_id", locationId)
        contentValues.put("is_synced", "N")
        contentValues.put("is_priority", "N")

        if (mTreeIsMissing) {
            contentValues.put("is_missing", "Y")
            contentValues.put("cause_of_death_id", noteId)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        var date = Date()
        val calendar = Calendar.getInstance()
        calendar.time = date

        val timeToNextUpdate = mSharedPreferences!!.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences!!.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
                ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING))

        calendar.add(Calendar.DAY_OF_MONTH, timeToNextUpdate)
        date = calendar.time as Date

        Timber.d("date " + date.toString())

        contentValues.put("time_for_update", dateFormat.format(date))
        contentValues.put("time_updated", dateFormat.format(Date()))

        TreeTrackerApplication.getDatabaseManager().update("tree", contentValues, "_id = ?", arrayOf<String>(treeIdStr!!))

        // tree_note
        contentValues = ContentValues()
        contentValues.put("tree_id", treeIdStr)
        contentValues.put("note_id", noteId)

        val treeNoteId = TreeTrackerApplication.getDatabaseManager().insert("tree_note", null, contentValues)
        Timber.d("treeNoteId " +java.lang.Long.toString(treeNoteId))

    }

    private fun setPic() {

        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        /* Get the size of the ImageView */
        val targetW = mImageView!!.width
        val targetH = mImageView!!.height

        /* Get the size of the image */
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val imageHeight = bmOptions.outHeight
        val imageWidth = bmOptions.outWidth
        val imageType = bmOptions.outMimeType

        // Calculate your sampleSize based on the requiredWidth and
        // originalWidth
        // For e.g you want the width to stay consistent at 500dp
        val requiredWidth = (500 * resources.displayMetrics.density).toInt()

        var sampleSize = Math.ceil((imageWidth.toFloat() / requiredWidth.toFloat()).toDouble()).toInt()

        // If the original image is smaller than required, don't sample
        if (sampleSize < 1) {
            sampleSize = 1
        }

        bmOptions.inSampleSize = sampleSize
        bmOptions.inPurgeable = true
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565
        bmOptions.inJustDecodeBounds = false

        /* Decode the JPEG file into a Bitmap */
        val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)

        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(mCurrentPhotoPath)
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

        Timber.d("rotationAngle", Integer.toString(rotationAngle))

        val matrix = Matrix()
        matrix.setRotate(rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                bitmap.height.toFloat() / 2)
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bmOptions.outWidth, bmOptions.outHeight, matrix, true)

        /* Associate the Bitmap to the ImageView */
        mImageView!!.setImageBitmap(rotatedBitmap)
        mImageView!!.visibility = View.VISIBLE
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.fragment_note_missing_tree_checkbox -> {
                mTreeIsMissing = isChecked
                val noteTxt = activity!!.findViewById(R.id.fragment_note_note) as EditText

                if (isChecked) {
                    noteTxt.hint = activity!!.resources.getString(R.string.cause_of_death)
                } else {
                    noteTxt.hint = activity!!.resources.getString(R.string.add_text_note)
                }
            }

            else -> {
            }
        }

    }

    companion object {

        fun calculateInSampleSize(options: BitmapFactory.Options,
                                  reqWidth: Int, reqHeight: Int): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                // Calculate ratios of height and width to requested height and
                // width
                val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
                val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())

                // Choose the smallest ratio as inSampleSize value, this will
                // guarantee
                // a final image with both dimensions larger than or equal to the
                // requested height and width.
                inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
            }

            return inSampleSize
        }
    }

}// some overrides and settings go here
