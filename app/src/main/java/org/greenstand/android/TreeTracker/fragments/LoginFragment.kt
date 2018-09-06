package org.greenstand.android.TreeTracker.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import org.apache.http.HttpStatus
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse
import org.greenstand.android.TreeTracker.utilities.ValueHelper


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber


class LoginFragment : Fragment(), OnClickListener {

    protected var mSharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu!!.clear()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater!!.inflate(R.layout.fragment_login, container, false)


        (activity as AppCompatActivity).supportActionBar!!.hide()


        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val loginBtn = v.findViewById(R.id.fragment_login_login) as Button
        loginBtn.setOnClickListener(this@LoginFragment)


        val loginText = v.findViewById(R.id.fragment_login_login_do_not_have_account) as TextView
        loginText.paintFlags = loginText.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        loginText.setOnClickListener(this@LoginFragment)

        val forgotPasswordText = v.findViewById(R.id.fragment_login_login_forgot_password) as TextView
        forgotPasswordText.paintFlags = forgotPasswordText.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        forgotPasswordText.setOnClickListener(this@LoginFragment)

        mSharedPreferences = activity.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)

        return v
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.fragment_login_login -> {


                var txtEmail = ""
                var txtPass = ""

                var validForm = true

                val loginEmail = activity.findViewById(R.id.fragment_login_email_address) as EditText
                val loginPassword = activity.findViewById(R.id.fragment_login_password) as EditText


                if (loginPassword.text.length == 0) {
                    loginPassword.error = "Please enter your password."
                    loginPassword.requestFocus()
                    validForm = false
                } else {
                    txtPass = loginPassword.text.toString()
                }

                if (loginEmail.text.length == 0) {
                    loginEmail.error = "Please enter your e-mail address."
                    loginEmail.requestFocus()
                    validForm = false
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(loginEmail.text).matches()) {
                    loginEmail.error = "Please enter valid e-mail address."
                    loginEmail.requestFocus()
                    validForm = false
                } else {
                    txtEmail = loginEmail.text.toString()
                    mSharedPreferences!!.edit().putString(ValueHelper.USERNAME, txtEmail).commit()
                }




                if (validForm) {
                    val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(loginEmail.windowToken, 0)

                    val authRequest = AuthenticationRequest()
                    authRequest.clientId = txtEmail
                    authRequest.clientSecret = txtPass

                    MainActivity.progressDialog = ProgressDialog(activity)
                    MainActivity.progressDialog!!.setCancelable(false)
                    MainActivity.progressDialog!!.setMessage(activity.getString(R.string.log_in_in_progress))
                    MainActivity.progressDialog!!.show()

                    val signIn = Api.instance().api!!.signIn(authRequest)
                    signIn.enqueue(object : Callback<TokenResponse> {
                        override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                            MainActivity.progressDialog!!.dismiss()

                            if (response.isSuccessful) {
                                val mSharedPreferences = activity.getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE)
                                mSharedPreferences.edit().putString(ValueHelper.TOKEN, response.body()!!.token).commit()
                                Api.instance().setAuthToken(response.body()!!.token!!)
                                (activity as MainActivity).transitionToMapsFragment()
                            } else {
                                when (response.code()) {
                                    HttpStatus.SC_UNAUTHORIZED -> Toast.makeText(activity, "Incorrect username or password.", Toast.LENGTH_SHORT).show()

                                    -1 -> Toast.makeText(activity, "Please check your internet connection and try again.", Toast.LENGTH_SHORT).show()

                                    else -> {
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                            MainActivity.progressDialog!!.dismiss()




                            Toast.makeText(activity, t.message, Toast.LENGTH_SHORT).show()
                            Timber.tag(TAG).e(t.message)
                        }
                    })
                }
            }

            R.id.fragment_login_login_do_not_have_account -> {

                mSharedPreferences = activity.getSharedPreferences(
                        "org.greenstand.android", Context.MODE_PRIVATE)

                mSharedPreferences!!.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, true).commit()
                mSharedPreferences!!.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit()

                val fragment = SignupFragment()
                val bundle = activity.intent.extras
                fragment.arguments = bundle

                val fragmentTransaction1 = activity.supportFragmentManager
                        .beginTransaction()
                fragmentTransaction1.replace(R.id.container_fragment, fragment).commit()
            }
            R.id.fragment_login_login_forgot_password -> {

                val fragment2 = ForgotPasswordFragment()
                val bundle2 = activity.intent.extras
                fragment2.arguments = bundle2

                val fragmentTransaction2 = activity.supportFragmentManager
                        .beginTransaction()
                fragmentTransaction2.replace(R.id.container_fragment, fragment2).addToBackStack(ValueHelper.FORGOT_PASSWORD_FRAGMENT).commit()
            }
        }

    }

    companion object {

        private val TAG = "LoginFragment"
    }


}//some overrides and settings go here
