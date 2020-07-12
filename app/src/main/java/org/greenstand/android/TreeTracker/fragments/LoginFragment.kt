package org.greenstand.android.TreeTracker.fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.utilities.CameraHelper
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.utilities.onTextChanged
import org.greenstand.android.TreeTracker.viewmodels.LoginViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginFragment : Fragment(){

    private val vm: LoginViewModel by viewModel()
    private val analytics: Analytics by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        requireActivity().toolbarTitle?.apply {
            setText(R.string.greenstand_welcome_text)
            setTextColor(resources.getColor(R.color.black))
        }

        vm.errorMessageLiveDate.observe(this, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        vm.loginButtonStateLiveDate.observe(this, Observer {
            login_button.isEnabled = it
        })

        vm.onNavigateToMap = {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToMapsFragment())
        }

        loginPhoneEditText.onTextChanged { vm.updatePhone(it) }
        loginEmailEditText.onTextChanged { vm.updateEmail(it) }

        login_button.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                if (vm.isUserPresentOnDevice()) {
                    withContext(Dispatchers.Main) {
                        CameraHelper.takePictureForResult(this@LoginFragment, selfie = true)
                    }
                    // User already has their info on device, skip the sign up and just update photo
                    Timber.d("User already on device, going to map")
                } else {
                    // User has no info on device, go through the sign up process
                    Timber.d("User not on device, going to signup flow")
                    analytics.userEnteredEmailPhone()
                    withContext(Dispatchers.Main) {
                        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToSignUpFragment(vm.userIdentification))
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Permissions.MY_PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CameraHelper.takePictureForResult(this, selfie = true)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode != Activity.RESULT_CANCELED) {
            if (resultCode == Activity.RESULT_OK) {
                vm.photoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")

        }
    }
}

