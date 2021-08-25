package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.ButtonColors
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TextButton
import org.greenstand.android.TreeTracker.view.TextStyles
import org.greenstand.android.TreeTracker.view.TopBarTitle

@OptIn(ExperimentalComposeApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val context = LocalContext.current
    val navController = LocalNavHostController.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    viewModel.showSnackBar = { stringRes ->
        scope.launch {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState.showSnackbar(
                message = context.getString(stringRes),
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            DashboardTopBar(navController)
        },
        scaffoldState = scaffoldState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Upload indicator and button.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top,
            ) {
                // Upload indicator.
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .padding(start = 20.dp, top = 20.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    DashboardUploadProgressBar(
                        progress = (viewModel.state.value?.treesSynced ?: 1)
                            .toFloat() / (viewModel.state.value?.totalTrees ?: 1),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = (viewModel.state.value?.treesToSync ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        color = AppColors.MediumGray,
                        fontSize = 16.sp,
                    )
                }

                DashBoardButton(
                    text = "Upload",
                    colors = AppButtonColors.UploadOrange,
                    onClick = {
                        viewModel.sync()
                    }
                )
            }

            DashBoardButton(
                text = "Messages",
                modifier = Modifier.weight(1f),
                colors = AppButtonColors.MessagePurple,
                onClick = {
                    navController.navigate(NavRoute.MessagesUserSelect.route)
                }
            )

            DashBoardButton(
                text = "Track",
                modifier = Modifier.weight(1f),
                colors = AppButtonColors.ProgressGreen,
                onClick = {
                    navController.navigate(NavRoute.UserSelect.route)
                }
            )
        }
    }
}

@Composable
fun DashboardTopBar(navController: NavController) {
    ActionBar(
        leftAction = {
            TextButton(
                modifier = Modifier.align(Alignment.Center),
                stringRes = R.string.organization,
                onClick = { navController.navigate(NavRoute.Org.route) }
            )
        },
        centerAction = { TopBarTitle() },
        rightAction = { LanguageButton() }
    )
}

@ExperimentalComposeApi
@Composable
fun DashboardUploadProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val strokeWidth = 8.dp
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
    }

    Canvas(
        modifier = modifier
            .progressSemantics(progress)
            .fillMaxWidth()
    ) {
        val diameterOffset = stroke.width / 2
        val arcDimension = size.width - 2 * diameterOffset

        // Function to draw a default styled arc.
        fun drawProgress(
            color: Color,
            sweepAngle: Float
        ) = drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(diameterOffset, diameterOffset),
            size = Size(arcDimension, arcDimension),
            style = stroke
        )

        drawProgress(AppColors.MediumGray, 180f)  // Background progress.
        drawProgress(AppColors.Orange, progress * 180f)  // Foreground progress.
    }
}

@ExperimentalComposeApi
@Composable
fun DashBoardButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    colors: ButtonColors
) {
    DepthButton(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        colors = colors,
        onClick = onClick
    ) {
        Text(
            text = text,
            style = TextStyles.DarkText
        )
    }
}

@ExperimentalComposeApi
@Preview
@Composable
fun DashboardScreen_Preview(
    @PreviewParameter(DashboardPreviewParameter::class) viewModel: DashboardViewModel
) {
    DashboardScreen(viewModel = viewModel)
}
