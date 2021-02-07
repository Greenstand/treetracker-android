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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.databinding.FragmentNewTreeBinding
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.utilities.*
import org.greenstand.android.TreeTracker.view.CustomToast
import org.greenstand.android.TreeTracker.viewmodels.NewTreeViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class NewTreeFragment :
    androidx.fragment.app.Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var bindings: FragmentNewTreeBinding
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
    ): View {
        bindings = FragmentNewTreeBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(mainActivity()) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            bindings.toolbarTitle.setText(R.string.new_tree)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val progressBar = view.findViewById<View>(R.id.progressBar)

        bindings.fragmentNewTreeNote.visibleIf(vm.isNoteEnabled)
        bindings.fragmentNewTreeDBH.visibleIf(vm.isDbhEnabled)
        bindings.fragmentNewTreeGPS.visibleIf(vm.isDbhEnabled)

        bindings.fragmentNewTreeSave.isEnabled = !vm.isDbhEnabled

        if (vm.isTreeHeightEnabled) {
            bindings.fragmentNewTreeSave.text = getString(R.string.next)
        } else {
            bindings.fragmentNewTreeSave.text = getString(R.string.save)
        }

        vm.navigateBack.observe(
            this,
            Observer {
                vm.newTreeCaptureCancelled()
                findNavController().popBackStack()
            }
        )

        vm.navigateToTreeHeight.observe(
            this,
            Observer {
                findNavController().navigate(
                    NewTreeFragmentDirections.actionNewTreeFragmentToTreeHeightFragment(it)
                )
            }
        )

        vm.onTreeSaved.observe(
            this,
            Observer {
                CustomToast.showToast("Tree saved")
            }
        )

        vm.onTakePicture.observe(
            this,
            Observer {
                CameraHelper.takePictureForResult(this, selfie = false)
            }
        )

        bindings.fragmentNewTreeSave.setOnClickListener {
            it.vibrate()
            vm.newTreePhotoCaptured()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                vm.createTree(
                    bindings.fragmentNewTreeNote.text.toString(),
                    bindings.fragmentNewTreeDBH.text.toString()
                )
            }
        }

        bindings.fragmentNewTreeGPS.setOnClickListener {
            requireActivity().dismissKeyboard()
            viewLifecycleOwner.lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                progressBar.setOnClickListener {
                    // Do nothing. We want to intercept taps on screen while loading is shown
                }
                vm.waitForConvergence()
                progressBar.setOnClickListener(null)
                progressBar.visibility = View.GONE
                bindings.fragmentNewTreeSave.isEnabled = true
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
        if (data != null && resultCode == Activity.RESULT_OK) {
            vm.photoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)

            vm.photoPath?.let {

                setPic(it)

                if (FeatureFlags.BLUR_DETECTION_ENABLED && vm.isImageBlurry(data)) {
                    bindings.fragmentNewTreeFocusWarningText.visibility = View.VISIBLE
                    bindings.fragmentNewTreeFocusWarningText.setText(R.string.focus_warning)
                } else {
                    bindings.fragmentNewTreeFocusWarningText.visibility = View.GONE
                    bindings.fragmentNewTreeFocusWarningText.text = ""
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
        bindings.fragmentNewTreeImage.setImageBitmap(rotatedBitmap)
        bindings.fragmentNewTreeImage.visibility = View.VISIBLE
    }
}
