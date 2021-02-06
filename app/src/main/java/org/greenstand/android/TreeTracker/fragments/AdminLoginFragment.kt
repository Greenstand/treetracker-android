package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.databinding.FragmentAdminLoginBinding
import org.greenstand.android.TreeTracker.utilities.hashString

class AdminLoginFragment : DialogFragment() {

    private lateinit var bindings: FragmentAdminLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentAdminLoginBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners() {
        bindings.btnPositive.setOnClickListener {
            val pwdHash = bindings.adminPassword.text.toString().hashString("sha-256")
            if (pwdHash == ADMIN_PWD_HASH) {
                findNavController().navigate(
                    AdminLoginFragmentDirections.actionAdminLoginFragmentToConfigFragment()
                )
                dismiss()
            } else {
                bindings.pwdError.text = "Invalid password"
                bindings.pwdError.visibility = View.VISIBLE
            }
        }
        bindings.btnNegative.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val ADMIN_PWD_HASH = "B06B37124C9E46A33EAEFC4221878485CD637B3DF928A68F7E71DE7CE04A1F3C" // kt-lint-disable --verbose max-line-length
    }
}
