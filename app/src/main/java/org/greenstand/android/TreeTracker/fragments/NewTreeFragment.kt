package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_new_tree.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.usecases.CreateTreeParams
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.view.CustomToast
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.getKoin
import timber.log.Timber
import kotlin.math.roundToInt

class NewTreeFragment : androidx.fragment.app.Fragment(),
    ActivityCompat.OnRequestPermissionsResultCallback {
    private val userLocationManager: UserLocationManager by inject()
    private val sharedPreferences: SharedPreferences by inject()
    private var takePictureInvoked: Boolean = false
    private var currentPhotoPath: String? = null
    private val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_tree, container, false)

        val activity = activity as AppCompatActivity
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        activity.toolbarTitle.setText(R.string.new_tree)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fragmentNewTreeSave.text = getString(
            if (FeatureFlags.TREE_NOTE_FEATURE_ENABLED) {
                R.string.save
            } else {
                R.string.next
            }
        )

        fragmentNewTreeNote.visibility = if (FeatureFlags.TREE_NOTE_FEATURE_ENABLED) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val meterText = resources.getString(R.string.meters)
        fragmentNewTreeDistance.text = "0 $meterText"
        val newTreeGpsAccuracy = userLocationManager.currentLocation?.accuracy?.roundToInt() ?: 0

        fragmentNewTreeGpsAccuracy.text = "$newTreeGpsAccuracy $meterText"

        val timeToNextUpdate = sharedPreferences.getInt(
            ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, sharedPreferences.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
                ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING
            )
        )

        fragmentNewTreeNextUpdate.setText(timeToNextUpdate.toString())

        if (sharedPreferences.getBoolean(ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING_PRESENT, false)) {
            fragmentNewTreeNextUpdate.isEnabled = false
        }

        fragmentNewTreeSave.setOnClickListener {
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                generateNewTree(timeToNextUpdate)?.let { newTree ->
                    if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
                        findNavController().navigate(
                            NewTreeFragmentDirections.actionNewTreeFragmentToTreeHeightFragment(newTree)
                        )
                    } else {
                        if (newTree.content.isNotBlank()) {
                            analytics.treeNoteAdded(newTree.content.length)
                        }
                        withContext(Dispatchers.IO) { saveToDb(newTree) }
                        CustomToast.showToast("Tree saved")
                        findNavController().popBackStack()
                    }
                } ?: run {
                    CustomToast.showToast("Insufficient GPS accuracy")
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (userLocationManager.currentLocation == null) {
            Toast.makeText(activity, "Insufficient GPS accuracy", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        } else if (!takePictureInvoked) {
            takePicture()
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
        if (grantResults.isNotEmpty() && requestCode == Permissions.MY_PERMISSION_CAMERA
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        takePictureInvoked = true

        if (data != null && resultCode == Activity.RESULT_OK) {
            currentPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)

            currentPhotoPath?.let {

                MainActivity.currentTreeLocation = Location("") // Just a blank location
                userLocationManager.currentLocation?.let { location ->
                    MainActivity.currentTreeLocation!!.latitude = location.latitude
                    MainActivity.currentTreeLocation!!.longitude = location.longitude
                }

                setPic(it)
                val imageQuality = data.getDoubleExtra(ValueHelper.FOCUS_METRIC_VALUE, 0.0);

                if (imageQuality < FOCUS_THRESHOLD) {
                    fragment_new_tree_focus_warning_text.visibility = View.VISIBLE
                    fragment_new_tree_focus_warning_text.setText(R.string.focus_warning)
                } else {
                    fragment_new_tree_focus_warning_text.visibility = View.GONE
                    fragment_new_tree_focus_warning_text.text = ""
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")
            activity!!.supportFragmentManager.popBackStack()
        }
    }

    private fun generateNewTree(timeToNextUpdate: Int): NewTree? {
        val minAccuracy = sharedPreferences.getInt(
            ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
            ValueHelper.MIN_ACCURACY_DEFAULT_SETTING
        )

        // note
        val content = fragmentNewTreeNote.text.toString()

        // tree
        val planterInfoId = sharedPreferences.getLong(ValueHelper.PLANTER_INFO_ID, 0)
        val planterCheckinId = sharedPreferences.getLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)

        return currentPhotoPath?.let {
            NewTree(
                it,
                minAccuracy,
                timeToNextUpdate,
                content,
                planterCheckinId,
                planterInfoId
            )
        }
    }

    private suspend fun saveToDb(newTree: NewTree): Long {
        val createTreeParams = CreateTreeParams(
            planterCheckInId = newTree.planterCheckInId,
            photoPath = newTree.photoPath,
            content = newTree.content
        )

        return getKoin().get<CreateTreeUseCase>().execute(createTreeParams)
    }

    private fun setPic(photoPath: String) {
        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */
        val rotatedBitmap = ImageUtils.decodeBitmap(photoPath, resources.displayMetrics.density)
        if (rotatedBitmap == null) {
            Toast.makeText(activity, "Error setting image. Please try again.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        /* Associate the Bitmap to the ImageView */
        fragmentNewTreeImage.setImageBitmap(rotatedBitmap)
        fragmentNewTreeImage.visibility = View.VISIBLE
    }

    companion object {
        const val FOCUS_THRESHOLD = 700.0
    }
}
