package org.greenstand.android.TreeTracker.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.activity_main.toolbarTitle
import kotlinx.android.synthetic.main.fragment_new_tree.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.utilities.CameraHelper
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.utilities.vibrate
import org.greenstand.android.TreeTracker.view.CustomToast
import org.greenstand.android.TreeTracker.viewmodels.NewTreeViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class NewTreeFragment :
    androidx.fragment.app.Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val vm: NewTreeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        with(activity as AppCompatActivity) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            toolbarTitle.setText(R.string.new_tree)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

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
            fragmentNewTreeGpsAccuracy.text =
                fragmentNewTreeGpsAccuracy.context.getString(R.string.gps_accuracy_double_colon, it)
        })

        vm.navigateBack.observe(this, Observer {
            vm.newTreeCaptureCancelled()
            findNavController().popBackStack()
        })

        vm.navigateToTreeHeight.observe(this, Observer {
            findNavController()
                .navigate(NewTreeFragmentDirections.actionNewTreeFragmentToTreeHeightFragment(it))
        })

        vm.onInsufficientGps.observe(this, Observer {
            CustomToast.showToast("Insufficient GPS accuracy")
        })

        vm.onTreeSaved.observe(this, Observer {
            CustomToast.showToast("Tree saved")
        })

        vm.onTakePicture.observe(this, Observer {
            CameraHelper.takePictureForResult(this, selfie = false)
        })

        fragmentNewTreeSave.setOnClickListener {
            it.vibrate()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                vm.createTree(fragmentNewTreeNote.text.toString())
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (CameraHelper.wasCameraPermissionGranted(requestCode, grantResults)) {
            CameraHelper.takePictureForResult(this, selfie = false)
        } else {
            vm.newTreeCaptureCancelled()
            findNavController().popBackStack()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        vm.newTreePhotoCaptured()
        if (data != null && resultCode == Activity.RESULT_OK) {
            vm.photoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)

            vm.photoPath?.let {

                setPic(it)

                if (FeatureFlags.BLUR_DETECTION_ENABLED && vm.isImageBlurry(data)) {
                    fragment_new_tree_focus_warning_text.visibility = View.VISIBLE
                    fragment_new_tree_focus_warning_text.setText(R.string.focus_warning)
                } else {
                    fragment_new_tree_focus_warning_text.visibility = View.GONE
                    fragment_new_tree_focus_warning_text.text = ""
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")
            findNavController().popBackStack()
        }
    }

    private fun setPic(photoPath: String) {
        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */
        val rotatedBitmap = ImageUtils.decodeBitmap(photoPath, resources.displayMetrics.density)
        if (rotatedBitmap == null) {
            CustomToast.showToast("Error setting image. Please try again.")
            findNavController().popBackStack()
        }
        /* Associate the Bitmap to the ImageView */
        fragmentNewTreeImage.setImageBitmap(rotatedBitmap)
        fragmentNewTreeImage.visibility = View.VISIBLE
    }
}
