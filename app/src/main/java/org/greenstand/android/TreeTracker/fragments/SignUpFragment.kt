package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_signup.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.onTextChanged
import org.greenstand.android.TreeTracker.viewmodels.SignupViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class SignUpFragment : Fragment() {

    private val vm: SignupViewModel by viewModel()

    private val args by navArgs<SignUpFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.userIdentification = args.userIdentification
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().toolbarTitle?.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.black))
        }

        vm.signupButtonStateLiveDate.observe(this, androidx.lifecycle.Observer {
            signUpFragmentButton.isEnabled = it
        })

        signupFirstNameEditText.onTextChanged { vm.firstName = it }
        signupLastNameEditText.onTextChanged { vm.lastName = it }
        signupOrganizationEditText.onTextChanged { vm.organization = it }
        vm.organization = signupOrganizationEditText.text.toString()

        signUpFragmentButton.setOnClickListener {
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToTermsPolicyFragment(vm.userInfo))
        }
    }
}