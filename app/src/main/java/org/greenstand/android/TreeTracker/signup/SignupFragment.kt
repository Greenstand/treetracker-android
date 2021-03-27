package org.greenstand.android.TreeTracker.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.greenstand.android.TreeTracker.utilities.createCompose
import org.koin.android.viewmodel.ext.android.viewModel

class SignupFragment : Fragment() {

    private val viewModel: SignupViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return createCompose(2) {
            SignupScreen(
                viewModel = viewModel,
                onNavBackward = { findNavController().popBackStack() },
                onNavForward = { /* Go to image capture */ },
                onNavLanguage = { findNavController().navigate(SignupFragmentDirections.actionGlobalLanguagePickerFragment()) }
            )
        }
    }
}
