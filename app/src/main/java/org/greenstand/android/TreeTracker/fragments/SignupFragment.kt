package org.greenstand.android.TreeTracker.fragments


import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.ApiService
import org.greenstand.android.TreeTracker.api.models.requests.RegisterRequest
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse
import org.greenstand.android.TreeTracker.api.models.responses.UserTree
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import java.util.UUID

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class SignupFragment : Fragment(), OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater!!.inflate(R.layout.fragment_signup, container, false)

        (activity as AppCompatActivity).supportActionBar!!.hide()

        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val signUpBtn = v.findViewById(R.id.fragment_signup_signup) as Button
        signUpBtn.setOnClickListener(this@SignupFragment)

        if (BuildConfig.DEBUG) {
            //It's not a release version.
            signUpBtn.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(view: View): Boolean {
                    val signupFirstName = activity.findViewById(R.id.fragment_signup_first_name) as EditText
                    val signupLastName = activity.findViewById(R.id.fragment_signup_last_name) as EditText
                    val signupEmail = activity.findViewById(R.id.fragment_signup_email_address) as EditText
                    val signupPassword = activity.findViewById(R.id.fragment_signup_password) as EditText
                    val signupOrganization = activity.findViewById(R.id.fragment_signup_organization) as EditText
                    val signupPhone = activity.findViewById(R.id.fragment_signup_phone_number) as EditText

                    signupFirstName.setText("First Name Test")
                    signupLastName.setText("Last Name Test")
                    signupEmail.setText(UUID.randomUUID().toString() + "@greenstand.org")
                    signupPassword.setText("tttttttt")
                    signupOrganization.setText("Greenstand")
                    signupPhone.setText("1234567890")

                    return true
                }
            })
        }

        val loginText = v.findViewById(R.id.fragment_signup_login_already_have_account) as TextView
        loginText.text = Html.fromHtml(loginText.text.toString() + " <a style=\"color:#916B4A;\" href=\"http://www.google.com\">"
                + resources.getString(R.string.log_in) + ".</a> ")
        Linkify.addLinks(loginText, Linkify.ALL)
        //loginText.setMovementMethod(LinkMovementMethod.getInstance());

        loginText.setOnClickListener(this@SignupFragment)

        val fragment_signup_privacy_policy_link = v.findViewById(R.id.fragment_signup_privacy_policy_link) as TextView
        fragment_signup_privacy_policy_link.movementMethod = LinkMovementMethod.getInstance()

        return v
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        val sharedPreferences = activity.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)


        when (v.id) {
            R.id.fragment_signup_signup -> {

                var txtFirstName = ""
                var txtLastName = ""
                var txtEmail = ""
                var txtPass = ""
                var txtPassConfirm = ""
                var txtOrg = ""
                var txtPhone = ""

                var validForm = true

                val signupFirstName = activity.findViewById(R.id.fragment_signup_first_name) as EditText
                val signupLastName = activity.findViewById(R.id.fragment_signup_last_name) as EditText
                val signupEmail = activity.findViewById(R.id.fragment_signup_email_address) as EditText
                val signupPassword = activity.findViewById(R.id.fragment_signup_password) as EditText
                val signupPasswordConfirm = activity.findViewById(R.id.fragment_signup_password_confirm) as EditText
                val signupOrganization = activity.findViewById(R.id.fragment_signup_organization) as EditText
                val signupPhone = activity.findViewById(R.id.fragment_signup_phone_number) as EditText

                txtOrg = signupOrganization.text.toString()
                txtPhone = signupPhone.text.toString()
                txtPass = signupPassword.text.toString().trim { it <= ' ' }
                txtPassConfirm = signupPasswordConfirm.text.toString().trim { it <= ' ' }

                val signupPrivacyPolicy = activity.findViewById(R.id.fragment_signup_privacy_policy_checkbox) as CheckBox

                if (signupPassword.text.length == 0) {
                    signupPassword.error = "Please enter your password."
                    signupPassword.requestFocus()
                    validForm = false
                } else if (txtPass != txtPassConfirm) {
                    signupPasswordConfirm.error = "Your passwords don't match."
                    signupPasswordConfirm.requestFocus()
                    validForm = false
                } else {
                    txtPass = signupPassword.text.toString()
                }

                if (signupEmail.text.length == 0) {
                    signupEmail.error = "Please enter your e-mail address."
                    signupEmail.requestFocus()
                    validForm = false
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(signupEmail.text).matches()) {
                    signupEmail.error = "Please enter valid e-mail address."
                    signupEmail.requestFocus()
                    validForm = false
                } else {
                    txtEmail = signupEmail.text.toString()
                    sharedPreferences.edit().putString(ValueHelper.USERNAME, txtEmail).commit()
                }

                if (signupLastName.text.length == 0) {
                    signupLastName.error = "Please enter your last name."
                    signupLastName.requestFocus()
                    validForm = false
                } else {
                    txtLastName = signupLastName.text.toString()
                }

                if (signupFirstName.text.length == 0) {
                    signupFirstName.error = "Please enter your first name."
                    signupFirstName.requestFocus()
                    validForm = false
                } else {
                    txtFirstName = signupFirstName.text.toString()
                }

                if (!signupPrivacyPolicy.isChecked) {
                    signupPrivacyPolicy.error = "Please accept the terms of service and privacy policy."
                    signupPrivacyPolicy.requestFocus()
                    validForm = false
                }

                if (validForm) {

                    val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(signupEmail.windowToken, 0)

                    val registerRequest = RegisterRequest()
                    registerRequest.firstName = txtFirstName
                    registerRequest.lastName = txtLastName
                    registerRequest.clientId = txtEmail
                    registerRequest.clientSecret = txtPass
                    registerRequest.organization = txtOrg
                    registerRequest.phone = txtPhone


                    MainActivity.progressDialog = ProgressDialog(activity)
                    MainActivity.progressDialog!!.setCancelable(false)
                    MainActivity.progressDialog!!.setMessage(activity.getString(R.string.sign_up_in_progress))
                    MainActivity.progressDialog!!.show()

                    val finalFirstName = txtFirstName
                    val finalLastName = txtLastName

                    val register = Api.instance().api!!.register(registerRequest)
                    register.enqueue(object : Callback<TokenResponse> {
                        override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                            MainActivity.progressDialog!!.cancel()

                            if (response.isSuccessful) {

                                val mSharedPreferences = activity.getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE)
                                mSharedPreferences.edit().putString(ValueHelper.TOKEN, response.body()!!.token).commit()
                                Api.instance().setAuthToken(response.body()!!.token!!)

                                (activity as MainActivity).transitionToMapsFragment()
                            }
                        }

                        override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                            MainActivity.progressDialog!!.cancel()
                            Toast.makeText(activity, t.message, Toast.LENGTH_SHORT).show()
                            Timber.tag(TAG).e(t.message)
                        }
                    })


                }
            }
            R.id.fragment_signup_login_already_have_account -> {

                sharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit()
                sharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, true).commit()


                val fragment = LoginFragment()
                val bundle = activity.intent.extras
                fragment.arguments = bundle

                val fragmentTransaction1 = activity.supportFragmentManager
                        .beginTransaction()
                fragmentTransaction1.replace(R.id.container_fragment, fragment).commit()
            }
        }

    }

    companion object {

        val TAG = "SignupFragment"
    }


}//some overrides and settings go here
