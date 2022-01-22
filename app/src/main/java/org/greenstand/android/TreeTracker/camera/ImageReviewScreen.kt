package org.greenstand.android.TreeTracker.camera

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.view.*

@Composable
fun ImageReviewScreen(photoPath: String) {

    val navController = LocalNavHostController.current
    val activity = LocalContext.current as Activity

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                DeclineButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = {
                    navController.navigate(NavRoute.Selfie.route) {
                        launchSingleTop = true
                        popUpTo(NavRoute.Selfie.route) { inclusive = true }
                    }
                }
                )
                AcceptButton(
                    onClick = {
                        val data = Intent().apply {
                            putExtra(CaptureImageContract.TAKEN_IMAGE_PATH, photoPath)
                        }
                        activity.setResult(AppCompatActivity.RESULT_OK, data)
                        activity.finish()
                    },
                )
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