package org.greenstand.android.TreeTracker.languagepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.utilities.createCompose
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class LanguagePickerFragment : Fragment() {

    private val viewModel: LanguagePickerViewModel by viewModel()
    private val languageSwitcher: LanguageSwitcher by inject()
    private val args by navArgs<LanguagePickerFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createCompose(2) {
            TreeTrackerTheme {
                LanguageSelectScreen(
                    onNavNext = {
                        if (args.isFromTopBar) {
                            findNavController().popBackStack()
                        } else {
                            findNavController()
                                .navigate(LanguagePickerFragmentDirections.actionLanguagePickerFragmentToSignupFragment())
                        }
                        languageSwitcher.applyCurrentLanguage(requireActivity())
                                },
                    viewModel = viewModel
                )
            }
        }
    }
}

