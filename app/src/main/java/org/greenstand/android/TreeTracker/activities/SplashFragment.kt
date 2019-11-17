package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.koin.android.ext.android.getKoin
import timber.log.Timber

class SplashFragment : Fragment() {

    private val userManager: UserManager = getKoin().get()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}, url: ${BuildConfig.BASE_URL}")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            whenStarted {
                delay(ValueHelper.SPLASH_SCREEN_DURATION)

                if (userManager.isLoggedIn) {
                    findNavController().navigate(SplashFragmentDirections.actionSplashFragment2ToMapsFragment())
                } else {
                    findNavController().navigate(SplashFragmentDirections.actionSplashFragment2ToLoginFlowGraph())
                }
            }
        }
    }
}
