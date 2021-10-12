package org.greenstand.android.TreeTracker.capture

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.*

@Composable
fun TreeImageReviewScreen(
    photoPath: String,
    viewModel: TreeImageReviewViewModel = viewModel(factory = LocalViewModelFactory.current)
) {

    val state by viewModel.state.observeAsState(TreeImageReviewState())
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
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
                            navController.popBackStack()
                        }
                    },
                    colors = DepthButtonColors(
                        color = AppColors.Red,
                        shadowColor = AppColors.RedShadow,
                        disabledColor = AppColors.RedShadow,
                        disabledShadowColor = AppColors.RedShadow
                    ),
                    modifier = Modifier.size(width = 54.dp, height = 54.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cancel_image),
                        contentDescription = null,
                        modifier = Modifier.size(width = 54.dp, height = 54.dp)
                            .align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(30.dp))
                DepthButton(
                    onClick = {
                        scope.launch {
                            viewModel.approveImage()
                            navController.popBackStack()
                        }
                    },
                    colors = DepthButtonColors(
                        color = Color.Green,
                        shadowColor = AppColors.GreenShadow,
                        disabledColor = AppColors.GreenDisabled,
                        disabledShadowColor = AppColors.GreenShadowDisabled
                    ),
                    modifier = Modifier.size(width = 54.dp, height = 54.dp)
                        .align(Alignment.CenterVertically)

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.check_icon),
                        contentDescription = null,
                        modifier = Modifier.size(width = 54.dp, height = 54.dp)
                            .align(Alignment.Center)
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