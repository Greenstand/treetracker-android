package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_signup.*
import kotlinx.android.synthetic.main.fragment_signup.view.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.viewmodels.PlanterDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*

class SignUpFragment:Fragment() {
    var newPhoneNumberEntered: String? = null
    var newEmailEntered: String? = null
    lateinit var fullName: String
    var organizationName: String? = null
    lateinit var viewModel: PlanterDetailsViewModel

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
        activity?.title = getString(R.string.sign_up_title)
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
        signUpFragmentButton.setTextAppearance(R.style.InactiveButtonStyle)
        signUpFragmentButton.setBackgroundResource(R.drawable.button_inactive)
        signUpFragmentButton.setOnClickListener(null)
    }

    @SuppressLint("NewApi")
    fun activateSignupButton() {
        signUpFragmentButton.setTextAppearance(R.style.ActiveButtonStyle)
        signUpFragmentButton.setBackgroundResource(R.drawable.button_active)

        signUpFragmentButton.setOnClickListener {
            organizationName = organizationEditText?.text.toString()
            saveNewUsersDetails()

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
        val organization = organizationName
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