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
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.utilities.onTextChanged
import org.greenstand.android.TreeTracker.viewmodels.LoginViewModel
import org.greenstand.android.TreeTracker.viewmodels.SignupViewModel
import timber.log.Timber
import java.util.*

class SignUpFragment : Fragment() {

    lateinit var viewModel: SignupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(SignupViewModel::class.java)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().toolbarTitle?.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.blackColor))
        }

        val extras = arguments

        if (extras != null) {
            viewModel.userIdentification = extras.getString(USER_IDENTIFICATION_KEY)!!
        } else {
            throw IllegalStateException("User Identification must be present from Login")
        }

        viewModel.signupButtonStateLiveDate.observe(this, androidx.lifecycle.Observer {
            signUpFragmentButton.isEnabled = it
        })

        signupFirstNameEditText.onTextChanged { viewModel.firstName = it }
        signupLastNameEditText.onTextChanged { viewModel.lastName = it }
        signupOrganizationEditText.onTextChanged { viewModel.organization = it }

        signUpFragmentButton.setOnClickListener {
            val termsFragment = TermsPolicyFragment.getInstance(viewModel.userInfo)
            activity?.supportFragmentManager?.beginTransaction()?.run {
                addToBackStack(null).replace(R.id.containerFragment, termsFragment)
                commit()
            }
        }
    }


//    private fun inactivateSigupButton() {
//        signUpFragmentButton.apply {
//            setBackgroundResource(R.drawable.button_inactive)
//            setOnClickListener(null)
//        }
//    }

//    fun activateSignupButton() {
//        signUpFragmentButton.apply {
//            setBackgroundResource(R.drawable.button_active)
//            setOnClickListener {
//                organizationName = organizationEditText?.text.toString()
//                saveNewUsersDetails()
//                makeNoEditableText()
//                storagePermissionCheck()
//                if(mPhotoPath == null){
//                    takePicture()
//                }else  saveNewUsersIdentifications()
//
//            }
//        }
//    }

    // eel avacado roll
    // alaska roll
    // shrimp tempura
    // philidelpha roll
    // spicy kani
    // salmon roll

    private fun storagePermissionCheck() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(
                    context!!,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ) {
                requireActivity().requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                    ),
                    Permissions.NECESSARY_PERMISSIONS
                )
            }
        }else {
            // Permission already granted.
            takePicture()

        }

    }

    fun takePicture() {
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

        } else {
            val takePictureIntent = Intent(context!!, CameraActivity::class.java)
            takePictureIntent.putExtra(ValueHelper.TAKE_SELFIE_EXTRA, true)
            startActivityForResult(takePictureIntent, ValueHelper.INTENT_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.size > 0){
            if (requestCode == Permissions.NECESSARY_PERMISSIONS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//
//        if (data != null && resultCode != Activity.RESULT_CANCELED) {
//            if (resultCode == Activity.RESULT_OK) {
//
//                mPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH)
//
//                if (mPhotoPath != null) {
//                    val imageButton = fragmentUserIdentificationPhoto
//                    val rotatedBitmap = ImageUtils.decodeBitmap(mPhotoPath, resources.displayMetrics.density)
//                    if(rotatedBitmap != null){
//                        imageButton?.setImageBitmap(rotatedBitmap)
//                    }
//                }
//
//            }
//        } else if (resultCode == Activity.RESULT_CANCELED) {
//            Timber.d("Photo was cancelled")
//
//        }
//    }

//    fun saveNewUsersIdentifications(){
//        val planterDetailsId = viewModel.planter?.id
//        val identifier = viewModel.planter?.identifier
//        val photoPath = mPhotoPath
//        val photoUrl = null
//        val timeCreated = Utils.dateFormat.format(Date())
//        viewModel.planter = PlanterIdentificationsEntity(
//            planterDetailsId = planterDetailsId?.toLong(), identifier = identifier,
//            photoPath = photoPath, photoUrl = photoUrl, timeCreated = timeCreated
//        )
//        viewModel.addPlanterIdentifications()
//
//    }

//    fun saveNewUsersDetails() {
//        val identifier = if(newPhoneNumberEntered != null) newPhoneNumberEntered else newEmailEntered
//        val firstName = fullName.substringBefore(' ', fullName)
//        val lastName = fullName.substringAfter(' ', "")
//        val organization = if(organizationName != null) organizationName else null
//        val phoneNumber = if(newPhoneNumberEntered != null) newPhoneNumberEntered else null
//        val email = if(newEmailEntered != null) newEmailEntered else null
//        val timeCreated = Utils.dateFormat.format(Date())
//       viewModel.newPlanter = PlanterDetailsEntity(identifier = identifier, firstName = firstName, lastName = lastName,
//           organization = organization, phone = phoneNumber, email = email, uploaded = false, timeCreated = timeCreated )
//        viewModel.addNewPlanter()
//    }

//    fun makeNoEditableText() {
//        organizationEditText.keyListener = null
//        nameEditText.keyListener = null
//        phoneEditTextSignup.keyListener = null
//        emailEditTextSignup.keyListener = null
//        signUpFragmentButton.setOnClickListener(null)
//    }

    companion object {

        private const val USER_IDENTIFICATION_KEY = "USER_IDENTIFICATION_KEY"

        fun getInstance(userIdentification: String): SignUpFragment {
            val bundle = Bundle().apply {
                putString(USER_IDENTIFICATION_KEY, userIdentification)
            }
            return SignUpFragment().apply {
                arguments = bundle
            }
        }
    }
}