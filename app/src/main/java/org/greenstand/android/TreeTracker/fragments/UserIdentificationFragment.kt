package org.greenstand.android.TreeTracker.fragments


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.application.Permissions

import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.Validation
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.util.regex.Pattern

/**
 * A simple [Fragment] subclass.
 * Use the [UserIdentificationFragement.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UserIdentificationFragment : Fragment() {

    private var mPhotoPath: String? = null
    private var mUserIdentifier: CharSequence? = null
    private var mSharedPreferences: SharedPreferences? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_user_identification, container, false)

        val loginButton = v.findViewById(R.id.fragment_user_identification_login)
        loginButton.setOnClickListener {
            // check if we got a photo already
            // if not go get the photo
            // other wise got forward
            val identifierTextView = v.findViewById(R.id.fragment_user_identification_email_address) as TextView
            mUserIdentifier = null
            val identifier = identifierTextView.text.toString()

            val phoneNumberPattern = Pattern.compile("\\d{7,15}")

            if(Validation.isEmailValid(identifier)){
                mUserIdentifier = identifier
            } else {
                identifier.replace(" ", "")
                identifier.replace("(", "")
                identifier.replace(")", "")
                identifier.replace("-", "")

                if(phoneNumberPattern.matcher(identifier).matches()) {
                    mUserIdentifier = identifier
                } else {

                    // Show some error message, with details about format
                    Toast.makeText(activity, "Invalid Identifier.  Please enter an email or a phone number", Toast.LENGTH_LONG)
                }
            }

            if(mUserIdentifier == null) {
                // Some Toast
            } else if(mPhotoPath == null){
                takePicture()
            } else {
                // do the login
                mSharedPreferences = activity.getSharedPreferences(
                        ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
                val editor = mSharedPreferences!!.edit()

                val tsLong = System.currentTimeMillis() / 1000
                editor!!.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, tsLong)
                editor!!.putString(ValueHelper.USER_IDENTIFIER, mUserIdentifier.toString())
                editor!!.putString(ValueHelper.USER_PHOTO, mPhotoPath)
                editor!!.commit()

                // TODO consider returning to MapFragment and pushing this new fragment from there
                val fragment = NewTreeFragment()

                activity.supportFragmentManager.popBackStack()
                val fragmentTransaction = activity.supportFragmentManager
                        .beginTransaction()
                fragmentTransaction!!.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.NEW_TREE_FRAGMENT).commit()
            }
        }

        val cameraButton = v.findViewById(R.id.fragment_user_identification_photo)
        cameraButton.setOnClickListener {
            takePicture()
        }

        return v
    }

    private fun takePicture() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Permissions.MY_PERMISSION_CAMERA)
        } else {
            val takePictureIntent = Intent(activity, CameraActivity::class.java)
            takePictureIntent.extras.putBoolean(ValueHelper.TAKE_SELFIE_EXTRA, true)
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
                    val imageButton = view!!.findViewById(R.id.fragment_user_identification_photo) as ImageButton
                    val rotatedBitmap = ImageUtils.decodeBitmap(mPhotoPath, resources.displayMetrics.density)
                    if(rotatedBitmap != null){
                        imageButton.setImageBitmap(rotatedBitmap)
                    }
                }

            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Photo was cancelled")
            if ((activity.findViewById(R.id.fragment_new_tree) as RelativeLayout).visibility != View.VISIBLE) {
                activity.supportFragmentManager.popBackStack()
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UserIdentificationFragement.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                UserIdentificationFragment().apply {

                }
    }
}
