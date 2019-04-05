package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_user_identification.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.utilities.ValueHelper.EMAIL_ADDRESS
import org.greenstand.android.TreeTracker.utilities.ValueHelper.PHONE_NUMBER
import org.greenstand.android.TreeTracker.viewmodels.PlanterDetailsViewModel
import timber.log.Timber

class LoginFragment : Fragment(){
    var phoneNumberEntered: String? = null
    var emailEntered: String? = null
    private var mPhotoPath: String? = null
    lateinit var viewModel: PlanterDetailsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(PlanterDetailsViewModel::class.java)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        requireActivity().toolbarTitle?.setText(R.string.greenstand_welcome_text)

        sign_up_button?.visibility = View.INVISIBLE

        phoneEditTextLogin.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                inactivateLoginButton()
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                inactivateLoginButton()
            }

            override fun afterTextChanged(editTextInputs: Editable) {
                phoneNumberEntered = editTextInputs.toString()
                viewModel.isUserPresentOnDevice(phoneNumberEntered!!).observe(this@LoginFragment,
                    Observer {
                        if (it != null && it.identifier == phoneNumberEntered) {
                            activateLoginButton()
                        } else {
                            sign_up_button.visibility = View.VISIBLE
                            emailEditText.addTextChangedListener(object : TextWatcher {
                                override fun afterTextChanged(editTextInputs: Editable?) {
                                    emailEntered = editTextInputs.toString()
                                    viewModel.isUserPresentOnDevice(emailEntered!!).observe(this@LoginFragment,
                                        Observer {
                                            if (it != null && (it.identifier == emailEntered)) {
                                                activateLoginButton()
                                            } else {
                                                inactivateLoginButton()
                                                sign_up_button.visibility = View.VISIBLE
                                            }
                                        })
                                }
                                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                                    inactivateLoginButton()
                                }
                                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                                    inactivateLoginButton()
                                }
                            })
                        }
                    })
            }
        })
        sign_up_button.setOnClickListener{
            val termsFragment = TermsPolicyFragment()
            val extras = Bundle()
            extras.apply {
                putString(PHONE_NUMBER, phoneNumberEntered)
                putString(EMAIL_ADDRESS, emailEntered)}
            termsFragment.arguments = extras
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            fragmentTransaction?.addToBackStack(null)?.replace(R.id.containerFragment, termsFragment)
            fragmentTransaction?.commit()

        }
    }
@SuppressLint("NewApi")
fun activateLoginButton(){
    login_button.apply {
        setTextAppearance(R.style.ActiveButtonStyle)
        setBackgroundResource(R.drawable.button_active)
    }
        if(sign_up_button.visibility == View.VISIBLE) sign_up_button?.visibility = View.INVISIBLE
        login_button.setOnClickListener {
            //Like the user_flow says if the user has already an account the camera for taking a selfie should open
            takePicture()

    }

}


@SuppressLint("NewApi")
fun inactivateLoginButton(){
    login_button.apply{
        setTextAppearance(R.style.InactiveButtonStyle)
        setBackgroundResource(R.drawable.button_inactive)
        setOnClickListener(null)
    }

}

//This  method is a copy of the one that is in UserIdentificationFragment
fun takePicture() {
    if (ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.context!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Permissions.MY_PERMISSION_CAMERA)
        }
    } else {
        val takePictureIntent = Intent(this.context!!, CameraActivity::class.java)
        takePictureIntent.putExtra(ValueHelper.TAKE_SELFIE_EXTRA, true)
        startActivityForResult(takePictureIntent, ValueHelper.INTENT_CAMERA)
    }
}


override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == Permissions.MY_PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        takePicture()
    }
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    if (data != null && resultCode != Activity.RESULT_CANCELED) {
        if (resultCode == Activity.RESULT_OK) {

            mPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)

            if (mPhotoPath != null) {
                val imageButton = fragmentUserIdentificationPhoto
                val rotatedBitmap = ImageUtils.decodeBitmap(mPhotoPath, resources.displayMetrics.density)
                if(rotatedBitmap != null){
                    imageButton?.setImageBitmap(rotatedBitmap)
                }
            }

        }
    } else if (resultCode == Activity.RESULT_CANCELED) {
        Timber.d("Photo was cancelled")

    }
}
}

