package org.greenstand.android.TreeTracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_signup.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.onTextChanged
import org.greenstand.android.TreeTracker.viewmodels.SignupViewModel

class SignUpFragment : Fragment() {

    lateinit var viewModel: SignupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this).get(SignupViewModel::class.java)

        arguments?.let {
            viewModel.userIdentification = it.getString(USER_IDENTIFICATION_KEY)!!
        } ?: run { throw IllegalStateException("User Identification must be present from Login") }

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().toolbarTitle?.apply {
            setText(R.string.sign_up_title)
            setTextColor(resources.getColor(R.color.black))
        }

        viewModel.signupButtonStateLiveDate.observe(this, androidx.lifecycle.Observer {
            signUpFragmentButton.isEnabled = it
        })

        signupFirstNameEditText.onTextChanged { viewModel.firstName = it }
        signupLastNameEditText.onTextChanged { viewModel.lastName = it }
        signupOrganizationEditText.onTextChanged { viewModel.organization = it }

        signUpFragmentButton.setOnClickListener {
            val termsFragment = TermsPolicyFragment.getInstance(viewModel.userInfo)
            activity?.supportFragmentManager?.beginTransaction()?.run {
                addToBackStack(null).replace(R.id.containerFragment, termsFragment)
                commit()
            }
        }
    }

    companion object {

        private const val USER_IDENTIFICATION_KEY = "USER_IDENTIFICATION_KEY"

        fun getInstance(userIdentification: String): SignUpFragment {
            val bundle = Bundle().apply {
                putString(USER_IDENTIFICATION_KEY, userIdentification)
            }

            return SignUpFragment().apply {
                arguments = bundle
            }
        }
    }
}