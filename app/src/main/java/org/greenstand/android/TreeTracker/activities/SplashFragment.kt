package org.greenstand.android.TreeTracker.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ValueHelper

import timber.log.Timber

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}, url: ${BuildConfig.BASE_URL}")

        GlobalScope.launch {
            delay(ValueHelper.SPLASH_SCREEN_DURATION)

//            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
//                                 Intent.FLAG_ACTIVITY_CLEAR_TASK or
//                                 Intent.FLAG_ACTIVITY_NO_ANIMATION)
//            }

//            overridePendingTransition(0, 0)
//            startActivity(intent)
//            overridePendingTransition(0, 0)
        }
    }
}
