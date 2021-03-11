package org.greenstand.android.TreeTracker.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.ImageCaptureActivity
import org.greenstand.android.TreeTracker.utilities.CameraHelper
import org.greenstand.android.TreeTracker.utilities.mainActivity
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginFragment : Fragment() {

//    private lateinit var bindings: FragmentLoginBinding

    private val vm: LoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LoginView(viewModel = vm, navController = findNavController())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mainActivity().bindings.toolbarTitle.apply {
            setText(R.string.greenstand_welcome_text)
            setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        vm.uiEvents.observe(viewLifecycleOwner) { uiEvent ->
            when (uiEvent) {
                is LoginViewModel.UIEvent.TakePhotoEvent -> CameraHelper.takePictureForResult(this, true)
                else -> Unit // **Note: Some UIEvents are consumed in the composable LoginView and are ignored here.
            }
        }

//        vm.errorMessageLiveDate.observe(
//            viewLifecycleOwner,
//            Observer {
//                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
//            }
//        )
//
//        vm.loginButtonStateLiveDate.observe(
//            viewLifecycleOwner,
//            Observer {
//                bindings.loginButton.isEnabled = it
//            }
//        )

//        vm.onNavigateToMap = {
//            findNavController()
//                .navigate(LoginFragmentDirections.actionLoginFragmentToMapsFragment())
//        }

//        bindings.loginPhoneEditText.onTextChanged { vm.updatePhone(it) }
//        bindings.loginEmailEditText.onTextChanged { vm.updateEmail(it) }
//        bindings.loginEmailEditText.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_GO && bindings.loginButton.isEnabled) {
//                bindings.loginButton.performClick()
//                true
//            }
//            false
//        }

//        bindings.loginButton.setOnClickListener {
//            GlobalScope.launch(Dispatchers.IO) {
//                if (vm.isUserPresentOnDevice()) {
//                    withContext(Dispatchers.Main) {
//                        CameraHelper.takePictureForResult(this@LoginFragment, selfie = true)
//                    }
//                    // User already has their info on device, skip the sign up and just update photo
//                    Timber.d("User already on device, going to map")
//                } else {
//                    // User has no info on device, go through the sign up process
//                    Timber.d("User not on device, going to signup flow")
//                    analytics.userEnteredEmailPhone()
//                    withContext(Dispatchers.Main) {
//                        findNavController()
//                            .navigate(
//                                LoginFragmentDirections
//                                    .actionLoginFragmentToSignUpFragment(vm.userIdentification)
//                            )
//                    }
//                }
//            }
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (CameraHelper.wasCameraPermissionGranted(requestCode, grantResults)) {
            CameraHelper.takePictureForResult(this, selfie = true)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode != Activity.RESULT_CANCELED) {
            if (resultCode == Activity.RESULT_OK) {
                vm.photoPath = data.getStringExtra(ImageCaptureActivity.TAKEN_IMAGE_PATH)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")
        }
    }
}
