package org.greenstand.android.TreeTracker.splash

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.koin.core.context.GlobalContext
import timber.log.Timber


@Composable
fun SplashScreen(preferencesMigrator: PreferencesMigrator, navController: NavController?) {
    LaunchedEffect(true) {
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}")
        preferencesMigrator.migrateIfNeeded()
        delay(100)

        val hasUserSetup = true

        if (!hasUserSetup) {
            navController?.navigate(SplashScreenFragmentDirections.actionSplashScreenFragmentToLanguagePickerFragment(isFromTopBar = false))
        } else {
            navController?.navigate(SplashScreenFragmentDirections.actionGlobalDashboardFragment())
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