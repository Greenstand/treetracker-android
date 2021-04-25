package org.greenstand.android.TreeTracker.splash


import android.Manifest
import androidx.activity.compose.registerForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigate
import androidx.navigation.compose.popUpTo
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val permissionRequester = registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            scope.launch {
                if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    && result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                    Timber.tag("BuildVariant").d("build variant: ${BuildConfig.BUILD_TYPE}")

                    viewModel.migratePreferences()

                    delay(1000)

                    if (viewModel.requiresInitialSetup()) {
                        navController.navigate(NavRoute.Language.create(isFromTopBar = false)) {
                            popUpTo(NavRoute.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(NavRoute.Dashboard.route) {
                            popUpTo(NavRoute.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(true) {
        permissionRequester.launch(arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION))
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
