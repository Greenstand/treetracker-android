package org.greenstand.android.TreeTracker.capture

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role.Companion.Checkbox
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.*

private val openDialog =  mutableStateOf(false)

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
                    onClick = {
                        openDialog.value = true
                    }
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
                    modifier = Modifier
                        .size(width = 54.dp, height = 54.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cancel_image),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 54.dp, height = 54.dp)
                            .align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(30.dp))
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
                    modifier = Modifier
                        .size(width = 54.dp, height = 54.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.check_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 54.dp, height = 54.dp)
                            .align(Alignment.Center)
                    )
                }

            }
        }
            )
        }
    ) {
        addNoteDialog()
        LocalImage(
            modifier = Modifier.fillMaxSize(),
            imagePath = photoPath,
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }

}
@Composable
fun addNoteDialog() {
    val treeTrackerViewModel: TreeCaptureViewModel =
        viewModel(factory = TreeCaptureViewModelFactory(""))
    var text by remember { mutableStateOf("") }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                Text(text = "Add Note to tree")
            },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },

                    )
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    DepthButton(
                        onClick = {
                            treeTrackerViewModel.addNote(text)
                            openDialog.value = false
                        },
                        colors = DepthButtonColors(
                            color = Color.Green,
                            shadowColor = AppColors.GreenShadow,
                            disabledColor = AppColors.GreenDisabled,
                            disabledShadowColor = AppColors.GreenShadowDisabled
                        ),
                        modifier = Modifier
                            .size(width = 54.dp, height = 54.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.check_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 54.dp, height = 54.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        )
    }
}

