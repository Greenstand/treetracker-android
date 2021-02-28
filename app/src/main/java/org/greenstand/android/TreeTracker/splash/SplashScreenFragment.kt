package org.greenstand.android.TreeTracker.splash

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.di.appModule
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.utilities.createCompose
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import timber.log.Timber

class SplashScreenFragment : Fragment() {

    private val preferencesMigrator: PreferencesMigrator by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createCompose(1) {
            TreeTrackerTheme {
                SplashScreen(preferencesMigrator, findNavController())
            }
        }
    }
}

@Composable
fun SplashScreen(preferencesMigrator: PreferencesMigrator, navController: NavController?) {
    val context = LocalContext.current
    LaunchedEffect(true) {
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}")
        preferencesMigrator.migrateIfNeeded()
        delay(100)

        val hasUserSetup = false

        if (!hasUserSetup) {
            navController?.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToLanguagePickerFragment(isFromTopBar = false))
        } else {
            Toast.makeText(context, "Navigating to Dashboard is under construction", Toast.LENGTH_LONG)
                .show()
        }
    }

    Image(
        painter = painterResource(id = R.drawable.splash),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

@Preview
@Composable
fun SplashScreenPreview() {
    TreeTrackerTheme {
        SplashScreen(GlobalContext.get().koin.get(), null)
    }
}