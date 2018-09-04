package org.greenstand.android.TreeTracker.fragments


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.media.ExifInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ValueHelper

import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

import timber.log.Timber

class TreePreviewFragment : Fragment(), OnClickListener {

    private var treePreviewImageView: ImageView? = null
    private var mCurrentPhotoPath: String? = null
    private val mImageBitmap: Bitmap? = null
    private var treeIdStr: String? = ""
    private var fragment: Fragment? = null
    private var fragmentTransaction: FragmentTransaction? = null
    private var bundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_tree_preview, container, false)

        (activity.findViewById(R.id.toolbar_title) as TextView).setText(R.string.tree_preview)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val extras = arguments

        treeIdStr = extras.getString(ValueHelper.TREE_ID)

        treePreviewImageView = rootView.findViewById(R.id.fragment_tree_preview_image) as ImageView

        (rootView.findViewById(R.id.fragment_tree_preview_more) as Button).setOnClickListener(this@TreePreviewFragment)


        val readableDB = MainActivity.dbHelper!!.readableDatabase
        val query = "select * from tree " +
                "left outer join location on location._id = tree.location_id " +
                "left outer join tree_photo on tree._id = tree_id " +
                "left outer join photo on photo._id = photo_id where tree._id =" + treeIdStr

        val photoCursor = readableDB.rawQuery(query, null)

        photoCursor.moveToFirst()


        Log.e("query", query)

        do {

            mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"))

            val isOutdated = if (photoCursor.getString(photoCursor.getColumnIndex("is_outdated")) == null)
                false
            else
                photoCursor.getString(photoCursor.getColumnIndex("is_outdated")) == "Y"


            val noImage = rootView.findViewById(R.id.fragment_tree_preview_no_image) as TextView

            if (mCurrentPhotoPath != null && !isOutdated) {
                setPic()

                noImage.visibility = View.INVISIBLE
            } else {
                noImage.visibility = View.VISIBLE
            }

            MainActivity.mCurrentTreeLocation = Location("")
            MainActivity.mCurrentTreeLocation!!.latitude = java.lang.Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("lat")))
            MainActivity.mCurrentTreeLocation!!.longitude = java.lang.Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("long")))


            val results = floatArrayOf(0f, 0f, 0f)
            if (MainActivity.mCurrentLocation != null) {
                Location.distanceBetween(MainActivity.mCurrentLocation!!.latitude, MainActivity.mCurrentLocation!!.longitude,
                        MainActivity.mCurrentTreeLocation!!.latitude, MainActivity.mCurrentTreeLocation!!.longitude, results)
            }

            val distanceTxt = rootView.findViewById(R.id.fragment_tree_preview_distance) as TextView
            distanceTxt.text = Integer.toString(Math.round(results[0])) + " " + resources.getString(R.string.meters)

            val treeAccuracyTV = rootView.findViewById(R.id.fragment_tree_preview_gps_accuracy) as TextView
            val treeAccuracy = photoCursor.getString(photoCursor.getColumnIndex("accuracy")).toFloat()
            treeAccuracyTV.text = treeAccuracy.toString() + " " + resources.getString(R.string.meters)

            val createdTxt = rootView.findViewById(R.id.fragment_tree_preview_created) as TextView
            createdTxt.text = photoCursor.getString(photoCursor.getColumnIndex("time_created")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_created")).lastIndexOf(":"))

            val updatedTxt = rootView.findViewById(R.id.fragment_tree_preview_last_update) as TextView
            updatedTxt.text = photoCursor.getString(photoCursor.getColumnIndex("time_updated")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_updated")).lastIndexOf(":"))

            val statusTxt = rootView.findViewById(R.id.fragment_tree_preview_image_status) as TextView

            var dateForUpdate = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            try {
                dateForUpdate = dateFormat.parse(photoCursor.getString(photoCursor.getColumnIndex("time_for_update")))
            } catch (e: ParseException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            if (dateForUpdate.before(Date())) {
                statusTxt.setText(R.string.outdated)
            }


            val noteQuery = "select tree_id, note.*, content as notetext from tree " +
                    "left outer join tree_note on tree_id = tree._id " +
                    "left outer join note on note_id = note._id " +
                    "where content is not null and tree_id = " + treeIdStr + " order by note.time_created asc"

            val noteCursor = readableDB.rawQuery(noteQuery, null)


            val notesTxt = rootView.findViewById(R.id.fragment_tree_preview_notes) as TextView

            notesTxt.text = " "

            while (noteCursor.moveToNext()) {
                val currentText = notesTxt.text.toString()

                Log.e("tree_id", noteCursor.getString(noteCursor.getColumnIndex("tree_id")))
                Log.e("note", noteCursor.getString(noteCursor.getColumnIndex("notetext")))

                if (noteCursor.getString(noteCursor.getColumnIndex("notetext")).trim { it <= ' ' }.length == 0) {
                    continue
                }


                val text = noteCursor.getString(noteCursor.getColumnIndex("notetext")) + "\n\n" + currentText

                notesTxt.text = text

            }


        } while (photoCursor.moveToNext())


        return rootView
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.fragment_tree_preview_more -> {
                fragment = NoteFragment()

                bundle = activity.intent.extras

                if (bundle == null)
                    bundle = Bundle()

                bundle!!.putString(ValueHelper.TREE_ID, treeIdStr)
                fragment!!.arguments = bundle

                fragmentTransaction = activity.supportFragmentManager
                        .beginTransaction()
                fragmentTransaction!!.replace(R.id.container_fragment, fragment)
                        .addToBackStack(ValueHelper.NOTE_FRAGMENT).commit()
            }

            else -> {
            }
        }


    }

    private fun setPic() {

        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        val targetH = treePreviewImageView!!.height

        /* Get the size of the image */
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val imageWidth = bmOptions.outWidth
        // Calculate your sampleSize based on the requiredWidth and
        // originalWidth
        // For e.g you want the width to stay consistent at 500dp
        val requiredWidth = (500 * resources.displayMetrics.density).toInt()

        Log.e("required Width ", Integer.toString(requiredWidth))
        Log.e("imageWidth  ", Integer.toString(imageWidth))

        var sampleSize = Math.ceil((imageWidth.toFloat() / requiredWidth.toFloat()).toDouble()).toInt()

        Log.e("sampleSize ", Integer.toString(sampleSize))
        // If the original image is smaller than required, don't sample
        if (sampleSize < 1) {
            sampleSize = 1
        }

        Log.e("sampleSize 2 ", Integer.toString(sampleSize))
        bmOptions.inSampleSize = sampleSize
        bmOptions.inPurgeable = true
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565
        bmOptions.inJustDecodeBounds = false

        /* Decode the JPEG file into a Bitmap */
        val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)

        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(mCurrentPhotoPath)

            val orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
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

            Timber.d("rotationAngle " + Integer.toString(rotationAngle))

            val matrix = Matrix()
            matrix.setRotate(rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                    bitmap.height.toFloat() / 2)
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bmOptions.outWidth, bmOptions.outHeight, matrix, true)
            /* Associate the Bitmap to the ImageView */
            treePreviewImageView!!.setImageBitmap(rotatedBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            treePreviewImageView!!.setImageBitmap(bitmap)
        }



        treePreviewImageView!!.visibility = View.VISIBLE
    }


}//some overrides and settings go here
