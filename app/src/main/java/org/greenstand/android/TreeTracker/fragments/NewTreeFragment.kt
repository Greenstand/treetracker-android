package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_new_tree.*
import kotlinx.android.synthetic.main.fragment_new_tree.view.*
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.database.entity.*
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.Utils.Companion.dateFormat
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.util.*

class NewTreeFragment : androidx.fragment.app.Fragment(), OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private var mImageView: ImageView? = null
    private var mCurrentPhotoPath: String? = null
    private var userId: Long = 0
    private var mSharedPreferences: SharedPreferences? = null
    private var takePictureInvoked: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_new_tree, container, false)

        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        activity!!.toolbarTitle.setText(R.string.new_tree)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mSharedPreferences = (activity as AppCompatActivity).getSharedPreferences(
            "org.greenstand.android", Context.MODE_PRIVATE
        )

        userId = mSharedPreferences!!.getLong(ValueHelper.MAIN_USER_ID, -1)

        val saveBtn = v.fragmentNewTreeSave

        saveBtn.text = if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
            getString(R.string.next)
        } else {
            getString(R.string.save)
        }

        v.fragmentNewTreeNote.visibility = if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
            View.GONE
        } else {
            View.VISIBLE
        }

        saveBtn.setOnClickListener(this@NewTreeFragment)

        mImageView = v.fragmentNewTreeImage

        val newTreeDistance = v.fragmentNewTreeDistance
        val newTreeDistanceString = Integer.toString(0) + " " + resources.getString(R.string.meters)
        newTreeDistance.text = newTreeDistanceString

        val newTreeGpsAccuracy = v.fragmentNewTreeGpsAccuracy
        if (MainActivity.mCurrentLocation != null) {
            val newTreeGpsAccuracyString1 = Integer.toString(Math.round(MainActivity.mCurrentLocation!!.accuracy)) +
                    " " + resources.getString(R.string.meters)
            newTreeGpsAccuracy.text = newTreeGpsAccuracyString1
        } else {
            val newTreeGpsAccuracyString2 = "0 " + resources.getString(R.string.meters)
            newTreeGpsAccuracy.text = newTreeGpsAccuracyString2
        }


        val timeToNextUpdate = mSharedPreferences!!.getInt(
            ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences!!.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
                ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING
            )
        )

        val newTreetimeToNextUpdate = v.fragmentNewTreeNextUpdate
        val newTreeTimeToNextUpdateString = Integer.toString(timeToNextUpdate)
        newTreetimeToNextUpdate.setText(newTreeTimeToNextUpdateString)

        if (mSharedPreferences!!.getBoolean(ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING_PRESENT, false)) {
            newTreetimeToNextUpdate.isEnabled = false
        }

        newTreetimeToNextUpdate.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Timber.d("days " + s.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

        })

        return v
    }

    override fun onStart() {
        super.onStart()

        if (MainActivity.mCurrentLocation == null) {
            Toast.makeText(activity, "Insufficient GPS accuracy", Toast.LENGTH_SHORT).show()
            activity!!.supportFragmentManager.popBackStack()
        } else if (!takePictureInvoked) {
            takePicture()
        }
    }

    override fun onClick(v: View) {

        v.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )

        when (v.id) {

            R.id.fragmentNewTreeSave -> {

                GlobalScope.launch {

                    val newTree = createNewTreeData() ?: run {
                        Toast.makeText(activity, "Insufficient GPS accuracy", Toast.LENGTH_SHORT).show()
                        activity!!.supportFragmentManager.popBackStack()
                        return@launch
                    }

                    if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
                        requireActivity()
                            .supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.containerFragment, TreeHeightFragment.newInstance(newTree))
                            .addToBackStack(ValueHelper.TREE_HEIGHT_FRAGMENT)
                            .commit()
                    } else {
                        withContext(Dispatchers.IO) { saveToDb(newTree) }
                        Toast.makeText(activity, "Tree saved", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    private fun takePicture() {
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Permissions.MY_PERMISSION_CAMERA
            )
        } else {
            takePictureInvoked = true
            val takePictureIntent = Intent(activity, CameraActivity::class.java)
            startActivityForResult(takePictureIntent, ValueHelper.INTENT_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (requestCode == Permissions.MY_PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        takePictureInvoked = true

        if (data != null && resultCode != Activity.RESULT_CANCELED) {
            if (resultCode == Activity.RESULT_OK) {

                mCurrentPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)

                if (mCurrentPhotoPath != null) {

                    MainActivity.mCurrentTreeLocation = Location("") // Just a blank location
                    if (MainActivity.mCurrentLocation != null) {
                        MainActivity.mCurrentTreeLocation!!.latitude = MainActivity.mCurrentLocation!!.latitude
                        MainActivity.mCurrentTreeLocation!!.longitude = MainActivity.mCurrentLocation!!.longitude
                    }

                    setPic()
                }

            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")
            activity!!.supportFragmentManager.popBackStack()
        }
    }

    private fun createNewTreeData(): NewTree? {
        val minAccuracy = mSharedPreferences!!.getInt(
            ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
            ValueHelper.MIN_ACCURACY_DEFAULT_SETTING
        )

        val newTreetimeToNextUpdate = activity!!.fragmentNewTreeNextUpdate
        val timeToNextUpdate = Integer.parseInt(
            if (newTreetimeToNextUpdate.text.isEmpty())
                "0"
            else
                newTreetimeToNextUpdate.text.toString()
        )

        // note
        val content = requireActivity().fragmentNewTreeNote.text.toString()

        // tree
        val planterIdentifierId = mSharedPreferences!!.getLong(ValueHelper.PLANTER_IDENTIFIER_ID, 0)

        return mCurrentPhotoPath?.let {
            NewTree(it,
                    minAccuracy,
                    timeToNextUpdate,
                    content,
                    userId,
                    planterIdentifierId)
        }
    }

    private suspend fun saveToDb(newTree: NewTree): Long {
        return TreeManager.addTree(newTree.photoPath,
                                   newTree.minAccuracy,
                                   newTree.timeToNextUpdate,
                                   newTree.content,
                                   newTree.userId,
                                   newTree.planterIdentifierId)
    }

    private fun setPic() {

        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        val rotatedBitmap = ImageUtils.decodeBitmap(mCurrentPhotoPath, resources.displayMetrics.density)
        if (rotatedBitmap == null) {
            Toast.makeText(activity, "Error setting image. Please try again.", Toast.LENGTH_SHORT).show()
            activity!!.supportFragmentManager.popBackStack()
        }
        /* Associate the Bitmap to the ImageView */
        mImageView!!.setImageBitmap(rotatedBitmap)
        mImageView!!.visibility = View.VISIBLE
    }

    companion object {

        private val TAG = "NewTreeFragment"

        fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int, reqHeight: Int
        ): Int {
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

}
