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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import kotlinx.android.synthetic.main.fragment_user_identification.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

class LoginFragment : Fragment(){
    private var phoneNumberEntered: String? = null
    private var emailEntered: String? = null
    private var mPhotoPath: String? = null
    private var planterId: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_login, container, false)
        requireActivity().toolbarTitle?.setText(R.string.greenstand_welcome_text)

        v.sign_up_button?.visibility = View.INVISIBLE
//Here we will watch the phone edit text to check if the input number(#phoneNumberInput) is in our database already.
    //if the #phoneNumberInput is in our database we will turn Login button to ActiveButtonStyle and we will set
    // ClickListener on this button to go on further with the login feature
     v.phoneEditTextLogin.addTextChangedListener(object: TextWatcher {
        @SuppressLint("NewApi")
        override fun afterTextChanged(editTextInputs: Editable) {
            phoneNumberEntered = editTextInputs.toString()
            if(isExistingUser(phoneNumberEntered!!)) {
                activateLoginButton()
            }else {
                v.emailEditText.addTextChangedListener(object: TextWatcher {
                    @SuppressLint("NewApi")
                    override fun afterTextChanged(editTextInputs: Editable) {
                        emailEntered = editTextInputs.toString()
                        if (isExistingUser(emailEntered!!) || isExistingUser(phoneNumberEntered!!)) {
                            activateLoginButton()
                                }else
                            when (isExistingUser(emailEntered!!)) {
                                true -> activateLoginButton()
                                else -> {
                                    sign_up_button?.visibility = View.VISIBLE
                                }
                            }
                    }

                    override fun beforeTextChanged(editTextInputs: CharSequence?, p1: Int, p2: Int, p3: Int){
                        inactivateLoginButton()
                    }

                    override fun onTextChanged(editTextInputs: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        inactivateLoginButton()
                    }
                })
                when (isExistingUser(phoneNumberEntered!!)) {
                    true -> activateLoginButton()
                    else -> {
                        sign_up_button?.visibility = View.VISIBLE
                    }
                }

            }

        }

        override fun beforeTextChanged(editTextInputs: CharSequence?, p1: Int, p2: Int, p3: Int) {
            inactivateLoginButton()
        }

        override fun onTextChanged(editTextInputs: CharSequence?, p1: Int, p2: Int, p3: Int) {
            inactivateLoginButton()
        }

    })

    v.sign_up_button.setOnClickListener{
        //Open the Terms and Condition fragment

    }

 return v
}


//This is the method that will check into DB if the textEntered(that could be the phoneNumber or the email) already
// exists in our DB. If exists it will return TRUE else it will return FALSE. in fact, it will return the if the
// planterId is different than -1 ( this is the initial value given to var planterId), this var will receive
// the planterId from the DB if the user already exists
private fun isExistingUser(textEntered: String): Boolean {
    GlobalScope.launch{
        planterId = TreeTrackerApplication.getAppDatabase().planterDao().getPlanterIDByIdentifier(textEntered)

    }
    return planterId != null
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

