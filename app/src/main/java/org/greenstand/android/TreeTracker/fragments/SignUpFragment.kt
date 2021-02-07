package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.databinding.FragmentSignupBinding
import org.greenstand.android.TreeTracker.utilities.mainActivity
import org.greenstand.android.TreeTracker.utilities.onTextChanged
import org.greenstand.android.TreeTracker.viewmodels.SignupViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class SignUpFragment : Fragment() {

    private lateinit var bindings: FragmentSignupBinding

    private val vm: SignupViewModel by viewModel()
    private val analytics: Analytics by inject()

    private val args by navArgs<SignUpFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.userIdentification = args.userIdentification
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindings = FragmentSignupBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainActivity().bindings.toolbarTitle.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.black))
        }

        vm.signupButtonStateLiveDate.observe(this, androidx.lifecycle.Observer {
            bindings.signUpFragmentButton.isEnabled = it
        })

        bindings.signupFirstNameEditText.onTextChanged { vm.firstName = it }
        bindings.signupLastNameEditText.onTextChanged { vm.lastName = it }
        bindings.signupOrganizationEditText.onTextChanged { vm.organization = it }
        bindings.signupOrganizationEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO && bindings.signUpFragmentButton.isEnabled) {
                    bindings.signUpFragmentButton.performClick()
                    true
                }
                false
            }

        vm.organization = bindings.signupOrganizationEditText.text.toString()

        bindings.signUpFragmentButton.setOnClickListener {
            analytics.userEnteredDetails()
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToTermsPolicyFragment(vm.userInfo))
        }
    }
}