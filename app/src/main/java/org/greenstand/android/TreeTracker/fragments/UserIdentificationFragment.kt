package org.greenstand.android.TreeTracker.fragments


import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.fragment_user_identification.view.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CameraActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication

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
class UserIdentificationFragment : androidx.fragment.app.Fragment() {

    private var mPhotoPath: String? = null
    private var mUserIdentifier: CharSequence? = null
    private var mSharedPreferences: SharedPreferences? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_user_identification, container, false)

        val loginButton = v.fragmentUserIdentificationLogin
        loginButton.setOnClickListener {
            // check if we got a photo already
            // if not go get the photo
            // other wise got forward
            val identifierTextView = v.fragmentUserIdentificationEmailAddress
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
                }
            }

            if(mUserIdentifier == null) {
                Toast.makeText(activity, "Invalid Identifier.  Please enter an email or a phone " +
                        "number", Toast.LENGTH_LONG).show()
            } else if(mPhotoPath == null){
                takePicture()
            } else {
                // do the login
                TreeTrackerApplication. getDatabaseManager().openDatabase();

                val identifier = mUserIdentifier.toString()
                val planterDetailsCursor = TreeTrackerApplication.getDatabaseManager().queryCursor(
                        "SELECT * FROM planter_details WHERE identifier = '$identifier'", null);
                var planterDetailsId : Int? = null
                if(planterDetailsCursor.count > 0){
                    planterDetailsCursor.moveToFirst()
                    planterDetailsId = planterDetailsCursor.getInt(planterDetailsCursor.getColumnIndex("_id"))

                }

                // photo
                val identificationContentValues = ContentValues()
                identificationContentValues.put("identifier", identifier)
                identificationContentValues.put("photo_path", mPhotoPath)
                identificationContentValues.put("planter_details_id", planterDetailsId)

                val identificationId = TreeTrackerApplication.getDatabaseManager().insert(
                        "planter_identifications", null, identificationContentValues)


                mSharedPreferences = activity!!.getSharedPreferences(
                        ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
                val editor = mSharedPreferences!!.edit()

                val tsLong = System.currentTimeMillis() / 1000
                editor!!.putString(ValueHelper.PLANTER_IDENTIFIER, mUserIdentifier.toString())
                editor!!.putString(ValueHelper.PLANTER_PHOTO, mPhotoPath)
                editor!!.putLong(ValueHelper.PLANTER_IDENTIFIER_ID, identificationId)
                editor!!.commit()

                // TODO consider returning to MapFragment and pushing this new fragment from there

                activity!!.supportFragmentManager.popBackStack()
                val fragmentTransaction = activity!!.supportFragmentManager
                        .beginTransaction()
                if(planterDetailsId == null){
                    val fragment = UserDetailsFragment()
                    fragmentTransaction!!.replace(R.id.containerFragment, fragment).addToBackStack(ValueHelper.USER_DETAILS_FRAGMENT).commit()

                } else {

                    // We only fully verify the user identification if we have already collected the details
                    editor!!.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, tsLong)
                    editor!!.commit()

                    val fragment = UserDetailsFragment()
                    fragmentTransaction!!.replace(R.id.containerFragment, fragment).commit()
                }
            }
        }

        val cameraButton: ImageButton = v.fragmentUserIdentificationPhoto
        cameraButton.setOnClickListener {
            takePicture()
        }

        return v
    }

    private fun takePicture() {
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Permissions.MY_PERMISSION_CAMERA)
        } else {
            val takePictureIntent = Intent(activity, CameraActivity::class.java)
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
                    val imageButton = view?.fragmentUserIdentificationPhoto
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
