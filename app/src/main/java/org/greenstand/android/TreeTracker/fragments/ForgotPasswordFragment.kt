package org.greenstand.android.TreeTracker.fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.models.requests.ForgotPasswordRequest

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class ForgotPasswordFragment : Fragment(), OnClickListener {
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater!!.inflate(R.layout.fragment_forgot_password, container, false)

        val loginBtn = v.findViewById(R.id.fragment_forgot_password_submit) as Button
        loginBtn.setOnClickListener(this@ForgotPasswordFragment)

        return v
    }

    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.fragment_forgot_password_submit -> {

                var validForm = true
                val forgotEmail = activity.findViewById(R.id.fragment_forgot_password_email_address) as TextView

                if (forgotEmail.text.length == 0) {
                    forgotEmail.error = "Please enter your e-mail address."
                    forgotEmail.requestFocus()
                    validForm = false
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(forgotEmail.text).matches()) {
                    forgotEmail.error = "Please enter valid e-mail address."
                    forgotEmail.requestFocus()
                    validForm = false
                }


                if (validForm) {
                    val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(forgotEmail.windowToken, 0)

                    progressDialog = ProgressDialog(activity)
                    progressDialog!!.setCancelable(false)
                    progressDialog!!.setMessage(activity.getString(R.string.forgot_password_resetting))
                    progressDialog!!.show()

                    val forgotPasswordRequest = ForgotPasswordRequest()
                    forgotPasswordRequest.clientId = forgotEmail.text.toString()
                    val forgotPassword = Api.instance().api!!.passwordReset(forgotPasswordRequest)
                    forgotPassword.enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            progressDialog!!.hide()

                            val builder = AlertDialog.Builder(activity)

                            builder.setTitle(R.string.password_reset)

                            val mailOK = true // Need to check what the server returned
                            if (mailOK) {
                                builder.setMessage(R.string.reset_password_link_was_sent)
                            } else {
                                builder.setMessage(R.string.email_was_not_found)
                            }

                            builder.setNeutralButton(R.string.ok) { dialog, which ->
                                if (mailOK) {
                                    activity.supportFragmentManager.popBackStack()
                                }
                                dialog.dismiss()
                            }


                            val alert = builder.create()
                            alert.show()
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            progressDialog!!.hide()
                            Toast.makeText(activity, "Password Reset Failed", Toast.LENGTH_SHORT).show()
                            Timber.d(t.toString())

                        }
                    })
                }
            }
        }

    }

    companion object {

        private val TAG = "ForgotPasswordFragment"
    }


}//some overrides and settings go here
