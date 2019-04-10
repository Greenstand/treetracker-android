package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_signup.*
import kotlinx.android.synthetic.main.fragment_signup.view.*
import kotlinx.android.synthetic.main.fragment_user_identification.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.viewmodels.PlanterDetailsViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class SignUpFragment:Fragment() {
    var newPhoneNumberEntered: String? = null
    var newEmailEntered: String? = null
    lateinit var fullName: String
    var organizationName: String? = null
    lateinit var viewModel: PlanterDetailsViewModel
    private var mPhotoPath: String? = null
    private val RECORD_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(PlanterDetailsViewModel::class.java)
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        requireActivity().toolbarTitle?.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.blackColor))
        }

        val extras = arguments
        if (extras != null) {
            newPhoneNumberEntered = extras.getString(ValueHelper.PHONE_NUMBER)
            newEmailEntered = extras.getString(ValueHelper.EMAIL_ADDRESS)
        }
        view.phoneEditTextSignup.setText(newPhoneNumberEntered)
        view.emailEditTextSignup.setText(newEmailEntered)

        view.nameEditText.addTextChangedListener(object : TextWatcher {
            @SuppressLint("NewApi")
            override fun afterTextChanged(textInputted: Editable?) {
                fullName = textInputted.toString()
                activateSignupButton()
                view.nameEditText.setCompoundDrawablesWithIntrinsicBounds(
                    resources.getDrawable(R.drawable.person_black),
                    null, null, null
                )
                view.nameEditText.setTextColor(resources.getColor(R.color.blackColor))

            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                inactivateSigupButton()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                inactivateSigupButton()
            }
        })

        return view
    }

    @SuppressLint("NewApi")
    private fun inactivateSigupButton() {
        signUpFragmentButton.apply {
            setTextAppearance(R.style.InactiveButtonStyle)
            setBackgroundResource(R.drawable.button_inactive)
            setOnClickListener(null)
        }
    }

    @SuppressLint("NewApi")
    fun activateSignupButton() {
        signUpFragmentButton.apply {
            setTextAppearance(R.style.ActiveButtonStyle)
            setBackgroundResource(R.drawable.button_active)
            setOnClickListener {
                organizationName = organizationEditText?.text.toString()
                saveNewUsersDetails()
                makeNoEditableText()
                storagePermissionCheck()

            }
        }
    }

    private fun storagePermissionCheck() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ||(checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                requireActivity().toolbarTitle?.apply {
                    setText(R.string.storage_permission_title)
                }
                requireActivity().requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA),
                    RECORD_REQUEST_CODE)
            } else {
                // Permission already granted.
                takePicture()

            }
        }

    }

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
        if (requestCode == RECORD_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    //This was taken from TreeManager.kt
    fun getTime(): String{
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val calendar = Calendar.getInstance().apply {
            time = Date()
        }
        return dateFormat.format(calendar.time)
    }

    fun saveNewUsersDetails() {
        val identifier = if(newPhoneNumberEntered != null) newPhoneNumberEntered!! else newEmailEntered!!
        val firstName = fullName.substringBefore(' ', fullName)
        val lastName = fullName.substringAfter(' ', "")
        val organization = if(organizationName != null) organizationName else null
        val phoneNumber = if(newPhoneNumberEntered != null) newPhoneNumberEntered else null
        val email = if(newEmailEntered != null) newEmailEntered else null
        val timeCreated = getTime()
       viewModel.newPlanter = PlanterDetailsEntity(identifier = identifier, firstName = firstName, lastName = lastName,
           organization = organization, phone = phoneNumber, email = email, uploaded = false, timeCreated = timeCreated )
        viewModel.addNewPlanter()
    }

    fun makeNoEditableText() {
        organizationEditText.keyListener = null
        nameEditText.keyListener = null
        phoneEditTextSignup.keyListener = null
        emailEditTextSignup.keyListener = null
        signUpFragmentButton.setOnClickListener(null)
    }


}