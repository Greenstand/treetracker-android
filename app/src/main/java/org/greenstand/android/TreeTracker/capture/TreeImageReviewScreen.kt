package org.greenstand.android.TreeTracker.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.LocalImage

@Composable
fun TreeImageReviewScreen(
    photoPath: String,
    viewModel: TreeImageReviewViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

//    val state by viewModel.state.observeAsState(TreeImageReviewState())
    val navController = LocalNavHostController.current

    Scaffold(
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
//                    navController.popBackStack()
                    navController.navigate(NavRoute.TreeCapture.create(viewModel.profilePicPath)) {
                        launchSingleTop = true
                        popUpTo(NavRoute.TreeCapture.create(viewModel.profilePicPath)) { inclusive = true }
                    }
                }) {
                    Text("Retake")
                }
                Button(onClick = {
                    viewModel.approveImage()
//                    navController.popBackStack()
                    navController.navigate(NavRoute.TreeCapture.create(viewModel.profilePicPath)) {
                        launchSingleTop = true
                        popUpTo(NavRoute.TreeCapture.create(viewModel.profilePicPath)) { inclusive = true }
                    }
                }) {
                    Text("Accept")
                }
            }
        }
    ) {
        LocalImage(
            modifier = Modifier.fillMaxSize(),
            imagePath = photoPath,
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}
