package org.greenstand.android.TreeTracker.fragments


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.os.Bundle
import android.view.*
import android.view.View.OnClickListener
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_tree_preview.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.io.IOException
import java.text.ParseException
import java.util.*

class TreePreviewFragment : Fragment(), OnClickListener {

    private var mImageView: ImageView? = null
    private var mCurrentPhotoPath: String? = null
    private val mImageBitmap: Bitmap? = null
    private var treeIdStr: String? = ""
    private var fragment: androidx.fragment.app.Fragment? = null
    private var fragmentTransaction: androidx.fragment.app.FragmentTransaction? = null
    private var bundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_tree_preview, container, false)

        activity?.toolbarTitle?.setText(R.string.tree_preview)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val extras = arguments

        treeIdStr = extras!!.getString(ValueHelper.TREE_ID)

        mImageView = v.fragmentTreePreviewImage

        v.fragmentTreePreviewMore.setOnClickListener(this@TreePreviewFragment)

        runBlocking {

            val trees = GlobalScope.async {
                TreeTrackerApplication.getAppDatabase().treeDao().getTreeDtoByID(treeIdStr!!.toLong())
            }.await()

            trees.forEach {


                mCurrentPhotoPath = it.name

                val isOutdated = it.isOutdated == true

                val noImage = v.fragmentTreePreviewNoImage

                if (mCurrentPhotoPath != null && !isOutdated) {
                    setPic()

                    noImage.visibility = View.INVISIBLE
                } else {
                    noImage.visibility = View.VISIBLE
                }

                MainActivity.mCurrentTreeLocation = Location("")
                MainActivity.mCurrentTreeLocation!!.latitude = it.latitude
                MainActivity.mCurrentTreeLocation!!.longitude = it.longitude

                // No GPS accuracy info from new api.
                //			MainActivity.mCurrentTreeLocation.setAccuracy(Float.parseFloat(photoCursor.getString(photoCursor.getColumnIndex("accuracy"))));

                val results = floatArrayOf(0f, 0f, 0f)
                if (MainActivity.mCurrentLocation != null) {
                    Location.distanceBetween(
                        MainActivity.mCurrentLocation!!.latitude,
                        MainActivity.mCurrentLocation!!.longitude,
                        MainActivity.mCurrentTreeLocation!!.latitude,
                        MainActivity.mCurrentTreeLocation!!.longitude,
                        results
                    )
                }

                val distanceTxt = v.fragmentTreePreviewDistance
                val distanceTxtString =
                    Integer.toString(Math.round(results[0])) + " " + resources.getString(R.string.meters)
                distanceTxt.text = distanceTxtString

                val accuracyTxt = v.fragmentTreePreviewGpsAccuracy
                val treeAccuracy = it.accuracy
                val accuracyTxtString = treeAccuracy.toString() + " " + resources.getString(R.string.meters)
                accuracyTxt.text = accuracyTxtString


                val createdTxt = v.fragmentTreePreviewCreated
                createdTxt.text = it.tree_time_created!!.substring(
                    0,
                    it.tree_time_created!!.lastIndexOf(":")
                )

                val updatedTxt = v.fragmentTreePreviewLastUpdate
                updatedTxt.text = it.tree_time_updated!!.substring(
                    0,
                    it.tree_time_updated!!.lastIndexOf(":")
                )

                val statusTxt = v.fragmentTreePreviewImageStatus

                var dateForUpdate = Date()
                try {
                    dateForUpdate = Utils.dateFormat.parse(it.time_for_update)
                } catch (e: ParseException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }

                if (dateForUpdate.before(Date())) {
                    statusTxt.setText(R.string.outdated)
                }


                val notes = GlobalScope.async {
                    return@async TreeTrackerApplication.getAppDatabase().noteDao().getNotesByTreeID(treeIdStr!!)
                }.await()

                val notesTxt = v.fragmentTreePreviewNotes

                notesTxt.text = " "

                for (note in notes) {
                    val currentText = notesTxt.text.toString()

                    if (note.content?.isBlank() == true) {
                        continue
                    }

                    notesTxt.text = "${note.content}\n\n$currentText"
                }
            }
        }


        return v
    }

    override fun onClick(v: View) {

        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.fragmentTreePreviewMore -> {
                fragment = NoteFragment()

                bundle = activity!!.intent.extras

                if (bundle == null)
                    bundle = Bundle()

                bundle!!.putString(ValueHelper.TREE_ID, treeIdStr)
                fragment!!.arguments = bundle

                fragmentTransaction = activity!!.supportFragmentManager
                    .beginTransaction()
                fragmentTransaction?.replace(R.id.containerFragment, fragment as NoteFragment)
                    ?.addToBackStack(ValueHelper.NOTE_FRAGMENT)?.commit()
            }

            else -> {
            }
        }
    }

    private fun setPic() {

        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        val targetH = mImageView!!.height

        /* Get the size of the image */
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val imageWidth = bmOptions.outWidth
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
            exif = ExifInterface(mCurrentPhotoPath!!)

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
            matrix.setRotate(
                rotationAngle.toFloat(), bitmap.width.toFloat() / 2,
                bitmap.height.toFloat() / 2
            )
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bmOptions.outWidth, bmOptions.outHeight, matrix, true
            )
            /* Associate the Bitmap to the ImageView */
            mImageView!!.setImageBitmap(rotatedBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            mImageView!!.setImageBitmap(bitmap)
        }

        mImageView!!.visibility = View.VISIBLE
    }


}//some overrides and settings go here
