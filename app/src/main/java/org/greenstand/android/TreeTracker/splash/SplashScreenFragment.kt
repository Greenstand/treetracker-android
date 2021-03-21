package org.greenstand.android.TreeTracker.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.utilities.createCompose
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.android.ext.android.inject

class SplashScreenFragment : Fragment() {

    private val preferencesMigrator: PreferencesMigrator by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createCompose(1) {
            TreeTrackerTheme {
//                SplashScreen(preferencesMigrator, findNavController())
            }
        }
    }
}
