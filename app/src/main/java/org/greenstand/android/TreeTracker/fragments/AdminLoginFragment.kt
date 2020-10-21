package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_admin_login.adminPassword
import kotlinx.android.synthetic.main.fragment_admin_login.pwdError
import kotlinx.android.synthetic.main.fragment_admin_login.view.btnNegative
import kotlinx.android.synthetic.main.fragment_admin_login.view.btnPositive
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.hashString

class AdminLoginFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners(view: View) {
        view.btnPositive.setOnClickListener {
            val pwdHash = adminPassword.text.toString().hashString("sha-256")
            if (pwdHash == ADMIN_PWD_HASH) {
                findNavController().navigate(
                    AdminLoginFragmentDirections.actionAdminLoginFragmentToConfigFragment()
                )
                dismiss()
            } else {
                pwdError.text = "Invalid password"
                pwdError.visibility = View.VISIBLE
            }
        }
        view.btnNegative.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private val ADMIN_PWD_HASH = "B06B37124C9E46A33EAEFC4221878485CD637B3DF928A68F7E71DE7CE04A1F3C" // kt-lint-disable --verbose max-line-length
    }
}
