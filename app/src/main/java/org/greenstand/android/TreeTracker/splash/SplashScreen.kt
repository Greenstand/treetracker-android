package org.greenstand.android.TreeTracker.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.models.NavRoute
import timber.log.Timber

@Composable
fun SplashScreen(
    viewModel: SplashScreenViewModel = viewModel(factory = LocalViewModelFactory.current),
    navController: NavHostController = LocalNavHostController.current
) {

    LaunchedEffect(true) {
        Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}")
        viewModel.migratePreferences()
        delay(1000)

        val hasUserSetup = false // fixme: Change this back to true when we want to go to the DashBoard

        if (!hasUserSetup) {
            navController.navigate(NavRoute.Language.create(isFromTopBar = false))
        } else {
            navController.navigate(NavRoute.Dashboard.route)
        }
    }

    Image(
        painter = painterResource(id = R.drawable.splash),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
fun SplashScreenPreview(@PreviewParameter(SplashScreenPreviewProvider::class) viewModel: SplashScreenViewModel) {
    SplashScreen(
        viewModel = viewModel,
        navController = rememberNavController()
    )
}
