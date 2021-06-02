package org.greenstand.android.TreeTracker.camera

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.LocalImage

@Composable
fun ImageReviewScreen(photoPath: String) {

    val navController = LocalNavHostController.current
    val activity = LocalContext.current as Activity

    Scaffold(
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    navController.navigate(NavRoute.Camera.create(isSelfieMode = true)) {
                        launchSingleTop = true
                        popUpTo(NavRoute.Camera.route) { inclusive = true }
                    }
                }) {
                    Text("Retake")
                }
                Button(onClick = {
                    val data = Intent().apply {
                        putExtra(CaptureImageContract.TAKEN_IMAGE_PATH, photoPath)
                    }
                    activity.setResult(AppCompatActivity.RESULT_OK, data)
                    activity.finish()
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
