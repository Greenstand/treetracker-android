package org.greenstand.android.TreeTracker.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.addNavRoute
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme


class CaptureImageContract : ActivityResultContract<Boolean, String?>() {

    companion object {
        const val SELFIE_MODE = "SELFIE_MODE"
        const val FOCUS_METRIC_VALUE = "FOCUS_METRIC_VALUE"
        const val TAKEN_IMAGE_PATH = "TAKEN_IMAGE_PATH"
    }

    override fun createIntent(context: Context, selfieMode: Boolean): Intent {
        return Intent(context, ImageCaptureActivity::class.java).apply {
            putExtra(SELFIE_MODE, selfieMode)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode == Activity.RESULT_OK) {
            return intent?.getStringExtra(TAKEN_IMAGE_PATH)
        }
        return null
    }

}

class ImageCaptureActivity : AppCompatActivity() {

//    private lateinit var viewFinder: TextureView
//    private lateinit var imageCaptureButton: ImageButton
//    private lateinit var toolbarTitle: TextView
//    private val deviceOrientation by inject<DeviceOrientation>()
//
    companion object {
        private const val SELFIE_MODE = "SELFIE_MODE"

        const val FOCUS_METRIC_VALUE = "FOCUS_METRIC_VALUE"
        const val TAKEN_IMAGE_PATH = "TAKEN_IMAGE_PATH"

        fun createIntent(context: Context, selfieMode: Boolean = false): Intent {
            return Intent(context, ImageCaptureActivity::class.java).apply {
                putExtra(SELFIE_MODE, selfieMode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val captureSelfie = intent.extras?.getBoolean(SELFIE_MODE, false) ?: false

        setContent {
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalNavHostController provides navController
            ) {
                TreeTrackerTheme {
                    NavHost(navController, startDestination = NavRoute.Selfie.route) {
                        listOf(
                            NavRoute.Selfie,
                            NavRoute.ImageReview,
                        ).forEach { addNavRoute(it) }
                    }
                }
            }
        }
    }
}
