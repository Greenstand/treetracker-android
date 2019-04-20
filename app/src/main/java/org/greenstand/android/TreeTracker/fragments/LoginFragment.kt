package org.greenstand.android.TreeTracker.fragments

import android.Manifest
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_user_identification.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import timber.log.Timber
import java.util.*

class LoginFragment : Fragment(){

    lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        requireActivity().toolbarTitle?.apply {
            setText(R.string.greenstand_welcome_text)
            setTextColor(resources.getColor(R.color.blackColor))
        }

        viewModel.errorMessageLiveDate.observe(this, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        viewModel.loginButtonStateLiveDate.observe(this, Observer {
            login_button.isEnabled = it
        })

        loginPhoneEditText.onTextChanged { viewModel.updatePhone(it) }
        loginEmailEditText.onTextChanged { viewModel.updateEmail(it) }

        login_button.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                if (viewModel.isUserPresentOnDevice()) {
                    // User already has their info on device, skip the sign up and just update photo
                } else {
                    // User has no info on device, go through the sign up process
                    withContext(Dispatchers.Main) {
                        val fragment = SignUpFragment.getInstance(viewModel.userIdentification)
                        activity?.supportFragmentManager?.beginTransaction()?.run {
                            addToBackStack(null).replace(R.id.containerFragment, fragment)
                            commit()
                        }
                    }
                }
            }
        }

//        sign_up_button.setOnClickListener{
//            val termsFragment = TermsPolicyFragment()
//            val extras = Bundle()
//            extras.apply {
//                putString(PHONE_NUMBER, phoneNumberEntered)
//                putString(EMAIL_ADDRESS, emailEntered)}
//            termsFragment.arguments = extras
//            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
//            fragmentTransaction?.addToBackStack(null)?.replace(R.id.containerFragment, termsFragment)
//            fragmentTransaction?.commit()
//
//        }
    }

//    fun activateLoginButton(){
//        login_button.apply {
//            setBackgroundResource(R.drawable.button_active)
//            setOnClickListener {
//                //Like the user_flow says if the user has already an account the camera for taking a selfie should open
//                if(mPhotoPath == null){
//                    takePicture()
//                }else saveNewUsersIdentifications()
//            }
//        }
//
//    }

//    fun inactivateLoginButton(){
//        login_button.apply{
//            setBackgroundResource(R.drawable.button_inactive)
//            setOnClickListener(null)
//        }
//
//    }

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

//    fun saveNewUsersIdentifications(){
//            val planterDetailsId = viewModel.planter?.id
//            val identifier = viewModel.planter?.identifier
//            val photoPath = mPhotoPath
//            val photoUrl = null
//            val timeCreated = Utils.dateFormat.format(Date())
//            viewModel.planter = PlanterIdentificationsEntity(
//                planterDetailsId = planterDetailsId?.toLong(), identifier = identifier,
//                photoPath = photoPath, photoUrl = photoUrl, timeCreated = timeCreated
//            )
//            //viewModel.addPlanterIdentifications()
//
//    }

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
}

