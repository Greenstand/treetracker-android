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
        topBar = {
            ActionBar(
                centerAction = {
                    DepthButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 100.dp, 60.dp),
                        onClick = { }
                    ) {
                        Text("NOTE")
                    }
                }
            )
        },
        bottomBar = {
            ActionBar(
                centerAction = {
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DepthButton(
                            onClick = {
                                navController.navigate(NavRoute.Selfie.route) {
                                    launchSingleTop = true
                                    popUpTo(NavRoute.Selfie.route) { inclusive = true }
                                }
                            },
                            colors = DepthButtonColors(
                                color = AppColors.Red,
                                shadowColor = AppColors.RedShadow,
                                disabledColor = AppColors.RedShadow,
                                disabledShadowColor = AppColors.RedShadow
                            ),
                            modifier = Modifier.size(width = 54.dp, height = 54.dp)
                                .align(CenterVertically)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cancel_image),
                                contentDescription = null,
                                modifier = Modifier.size(width = 54.dp, height = 54.dp)
                                    .align(Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(30.dp))
                        DepthButton(
                            onClick = {
                                val data = Intent().apply {
                                    putExtra(CaptureImageContract.TAKEN_IMAGE_PATH, photoPath)
                                }
                                activity.setResult(AppCompatActivity.RESULT_OK, data)
                                activity.finish()

                            },
                            colors = DepthButtonColors(
                                color = Color.Green,
                                shadowColor = AppColors.GreenShadow,
                                disabledColor = AppColors.GreenDisabled,
                                disabledShadowColor = AppColors.GreenShadowDisabled
                            ),
                            modifier = Modifier.size(width = 54.dp, height = 54.dp)
                                .align(CenterVertically)

                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.check_icon),
                                contentDescription = null,
                                modifier = Modifier.size(width = 54.dp, height = 54.dp)
                                    .align(Center)


                            )
                        }
                    }
                }
            )
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