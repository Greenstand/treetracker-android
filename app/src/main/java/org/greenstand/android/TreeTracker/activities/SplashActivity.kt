package org.greenstand.android.TreeTracker.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R

import timber.log.Timber

class SplashActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}, url: ${BuildConfig.BASE_URL}")

        GlobalScope.launch {
            delay(1000)

            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                 Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                 Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }

            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}
