package org.greenstand.android.TreeTracker.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent

import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.BuildConfig

import timber.log.Timber

class SplashActivity : Activity() {

    var mSplashThread: Thread? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)
        Timber.tag("BuildVariant").d("build variant: " + BuildConfig.BUILD_TYPE + ", url: " + BuildConfig.BASE_URL)


        // The thread to wait for splash screen events
        mSplashThread = object : Thread() {
            override fun run() {
                try {
                    Thread.sleep(ValueHelper.SPLASH_SCREEN_DURATION)
                } catch (ex: InterruptedException) {
                }

                // Run next activity
                val intent = Intent(this@SplashActivity, MainActivity::class.java!!)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                overridePendingTransition(0, 0)

                finish()
                overridePendingTransition(0, 0)


            }
        }

        mSplashThread?.start()
    }



}
