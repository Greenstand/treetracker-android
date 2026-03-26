package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.GpsUtils
import org.greenstand.android.TreeTracker.view.NoGPSDeviceDialog
import org.koin.android.ext.android.inject

class TreeTrackerActivity : ComponentActivity() {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()
    private val gpsUtils: GpsUtils by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.argb(128, 0, 0, 0)
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.argb(128, 0, 0, 0)
            )
        )
        super.onCreate(savedInstanceState)

        languageSwitcher.applyCurrentLanguage(this)

        setContent {
            CustomTheme {
                if (gpsUtils.hasGPSDevice()) {
                    Root(viewModelFactory)
                } else {
                    NoGPSDeviceDialog(onPositiveClick = { finishAndRemoveTask() })
                }
            }
        }
    }
}
