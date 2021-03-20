package org.greenstand.android.TreeTracker.orgpicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.greenstand.android.TreeTracker.utilities.createCompose
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.android.viewmodel.ext.android.viewModel

class OrgPickerFragment : Fragment() {

    private val viewModel: OrgPickerViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createCompose(1) {
            TreeTrackerTheme {
               OrgPickerScreen(
                   viewModel = viewModel,
                   navController = findNavController()
               )
            }
        }
    }


}