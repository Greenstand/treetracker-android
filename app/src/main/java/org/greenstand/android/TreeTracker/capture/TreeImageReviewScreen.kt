package org.greenstand.android.TreeTracker.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
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

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    DepthButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 100.dp, 60.dp),
                        onClick = {
                            viewModel.setDialogState(true)
                        }
                    ) {
                        Text(stringResource(R.string.note))
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ApprovalButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = {
                        navController.popBackStack()
                    },
                    approval = false
                )
                ApprovalButton(
                    onClick = {
                        scope.launch {
                            viewModel.approveImage()
                            navController.popBackStack()
                        }
                    },
                    approval = true
                )
            }
        }
    ) {
        if (state.isDialogOpen) {
            NoteDialog(state = state, viewModel = viewModel)
        }
        LocalImage(
            modifier = Modifier.fillMaxSize(),
            imagePath = photoPath,
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun NoteDialog(state: TreeImageReviewState, viewModel: TreeImageReviewViewModel) {
    var text by remember { mutableStateOf(state.note) }
    AlertDialog(
        onDismissRequest = {
            viewModel.setDialogState(false)
        },
        title = {
            Text(text = stringResource(R.string.add_note_to_tree))
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
                        viewModel.addNote(text)
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