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
import androidx.lifecycle.Observer
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
import org.greenstand.android.TreeTracker.viewmodels.NewTreeViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.getKoin
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class NewTreeFragment : androidx.fragment.app.Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val userLocationManager: UserLocationManager by inject()
    private val sharedPreferences: SharedPreferences by inject()
    private var takePictureInvoked: Boolean = false
    private var currentPhotoPath: String? = null
    private val analytics: Analytics by inject()

    private val vm: NewTreeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = activity as AppCompatActivity
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        activity.toolbarTitle.setText(R.string.new_tree)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        vm.noteEnabledLiveData.observe(this, Observer { isNoteEnabled ->
            if (isNoteEnabled) {
                fragmentNewTreeSave.text = getString(R.string.save)
                fragmentNewTreeNote.visibility = View.VISIBLE
            } else {
                fragmentNewTreeSave.text = getString(R.string.next)
                fragmentNewTreeNote.visibility = View.GONE
            }
        })

        vm.accuracyLiveData.observe(this, Observer {
            fragmentNewTreeGpsAccuracy.text = fragmentNewTreeGpsAccuracy.context.getString(R.string.gps_accuracy_double_colon, it)
        })


        val timeToNextUpdate = sharedPreferences.getInt(
            ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, sharedPreferences.getInt(
                ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
                ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING
            )
        )

        fragmentNewTreeSave.setOnClickListener {
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                vm.generateNewTree(timeToNextUpdate,
                                   fragmentNewTreeNote.text.toString())?.let { newTree ->
                    if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
                        findNavController().navigate(NewTreeFragmentDirections.actionNewTreeFragmentToTreeHeightFragment(newTree))
                    } else {
                        if (newTree.content.isNotBlank()) {
                            analytics.treeNoteAdded(newTree.content.length)
                        }
                        analytics.treePlanted()
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
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                               Permissions.MY_PERMISSION_CAMERA)
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
