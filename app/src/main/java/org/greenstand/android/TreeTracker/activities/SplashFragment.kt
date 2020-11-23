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
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.User
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.koin.android.ext.android.inject
import timber.log.Timber

class SplashFragment : Fragment() {

    private val user: User by inject()
    private val preferencesMigrator: PreferencesMigrator by inject()
    private val languageSwitcher: LanguageSwitcher by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            whenStarted {

                preferencesMigrator.migrateIfNeeded()

                delay(ValueHelper.SPLASH_SCREEN_DURATION)

                when {
                    user.isLoggedIn ->
                        findNavController()
                            .navigate(SplashFragmentDirections
                                .actionSplashFragment2ToMapsFragment())

                    FeatureFlags.ORG_LINK_ENABLED ->
                        findNavController()
                            .navigate(SplashFragmentDirections
                                .actionSplashFragment2ToOrgWallFragment())

                    else -> findNavController()
                        .navigate(SplashFragmentDirections.actionSplashFragment2ToLoginFlowGraph())
                }
            }
        }
    }
}
