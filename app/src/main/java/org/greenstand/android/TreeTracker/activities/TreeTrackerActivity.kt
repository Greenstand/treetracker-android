package org.greenstand.android.TreeTracker.activities

import android.app.PendingIntent.getActivity
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.CustomDialog
import org.koin.android.ext.android.inject

class TreeTrackerActivity : ComponentActivity() {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()

    @ExperimentalComposeApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FeatureFlags.USE_SWAHILI) {
            languageSwitcher.setLanguage(Language.SWAHILI, resources)
        } else {
            languageSwitcher.applyCurrentLanguage(this)
        }
        if (hasGPSDevice(this)) {
            setContent {
                CustomTheme {
                    Root(viewModelFactory)
                }
            }
        } else {
            setContent {
                Surface(color = AppColors.Gray) {
                    CustomTheme {
                        NoGPSDeviceDialog(onPositiveClick = { finishAndRemoveTask() })
                    }
                }
            }
        }
    }

    fun hasGPSDevice(context: Context): Boolean {
        val mgr = context.getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = mgr.allProviders
        return providers.contains(LocationManager.GPS_PROVIDER)
    }
}

@Composable
fun NoGPSDeviceDialog(onPositiveClick: () -> Unit){
    CustomDialog(
        dialogIcon = painterResource(id = R.drawable.error_outline),
        title = stringResource(R.string.no_gps_device_header),
        textContent = stringResource(R.string.no_gps_device_content),
        onPositiveClick = onPositiveClick
    )
}