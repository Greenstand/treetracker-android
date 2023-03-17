package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.ButtonColors
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.dashboard.components.DisplayAlertDialog
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utils.PreviewDependencies
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ConsumableSnackBar
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TopBarTitle
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButtonShape

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.state.observeAsState(DashboardState())
    val snackBar by viewModel.snackBar.observeAsState()
    val navController = LocalNavHostController.current
    var dialogOpened by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog){
        DisplayAlertDialog(
            title = "Upload Trees Soon",
            message ="You have over 2000 trees, please upload as soon as you can.",
            dialogOpened = !dialogOpened ,
            onOkayClicked = {
                dialogOpened = false
                navController.navigate(NavRoute.UserSelect.route)
            }
        )
    }
    Dashboard(
        state = state,
        snackBar = snackBar,
        onSyncClicked = { viewModel.sync() },
        onOrgClicked = { navController.navigate(NavRoute.Org.route) },
        onCaptureClicked = {
            if (state.totalTreesToSync >=2000){
                showDialog = true
            }else navController.navigate(NavRoute.UserSelect.route)
            },
        onMessagesClicked = {
            viewModel.syncMessages()
            navController.navigate(NavRoute.MessagesUserSelect.route)
        },
    )
}

@OptIn(ExperimentalComposeApi::class)
@Composable
fun Dashboard(
    state: DashboardState,
    snackBar: ConsumableSnackBar? = null,
    onSyncClicked: () -> Unit = { },
    onOrgClicked: () -> Unit = { },
    onCaptureClicked: () -> Unit = { },
    onMessagesClicked: () -> Unit = { },
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(snackBar) {
        snackBar?.show(context, scaffoldState)
    }

    Scaffold(
        topBar = {
            DashboardTopBar(state, onOrgClicked)
        },
        scaffoldState = scaffoldState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.weight(.3f),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.yellow_leafs_placeholder),
                    contentDescription = "",
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(top = 5.dp, bottom = 10.dp, end = 10.dp)
                        .size(width = 30.dp, height = 30.dp)
                )
                Text(
                    modifier = Modifier.align(CenterVertically),
                    text = state.treesSynced.toString(),
                    fontWeight = FontWeight.Bold,
                    color = CustomTheme.textColors.uploadText,
                    style = CustomTheme.typography.large
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Upload indicator.
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    DashboardUploadProgressBar(
                        progress = (state.treesRemainingToSync)
                            .toFloat() / (state.totalTreesToSync),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = (state.treesRemainingToSync).toString(),
                        modifier = Modifier.weight(1f),
                        color = CustomTheme.textColors.lightText,
                        style = CustomTheme.typography.medium,
                    )
                }
                Spacer(modifier = Modifier.size(width = 16.dp, height = 0.dp))
                DashBoardButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .weight(1f),
                    text = stringResource(R.string.upload),
                    colors = AppButtonColors.UploadOrange,
                    onClick = onSyncClicked,
                    shape = TreeTrackerButtonShape.Circle,
                    image = painterResource(id = R.drawable.upload_icon)
                )
            }

            DashBoardButton(
                text = stringResource(R.string.messages),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxSize(),
                colors = AppButtonColors.MessagePurple,
                onClick = onMessagesClicked,
                image = painterResource(id = R.drawable.announcement_icon),
                showUnreadNotification = state.showUnreadMessageNotification
            )

            DashBoardButton(
                text = stringResource(R.string.track),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxSize(),
                colors = AppButtonColors.ProgressGreen,
                onClick = onCaptureClicked,
                image = painterResource(id = R.drawable.track_icon)
            )
        }
    }
}

@Composable
fun DashboardTopBar(state: DashboardState, onOrgClicked: () -> Unit) {
    ActionBar(
        leftAction = {
            if (!state.isOrgButtonEnabled) {
                return@ActionBar
            }
            TreeTrackerButton(
                colors = AppButtonColors.ProgressGreen,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 100.dp, 60.dp),
                onClick = onOrgClicked,
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Organization",
                    fontWeight = FontWeight.Bold,
                    color = CustomTheme.textColors.darkText,
                    style = CustomTheme.typography.regular
                )
            }
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
    colors: ButtonColors,
    shape: TreeTrackerButtonShape = TreeTrackerButtonShape.Rectangle,
    image: Painter,
    showUnreadNotification: Boolean = false,
) {
    TreeTrackerButton(
        modifier = modifier,
        colors = colors,
        onClick = onClick,
        shape = shape,
    ) {
        Image(
            painter = image,
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 25.dp, bottom = 40.dp)
        )
        Text(
            text = text,
            style = CustomTheme.typography.medium,
            fontWeight = FontWeight.Bold,
            color = CustomTheme.textColors.darkText,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            textAlign = TextAlign.Center,
        )
        if (showUnreadNotification){
            Image(
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 8.dp)
                    .size(33.dp)
                    .align(Alignment.BottomEnd),
                painter = painterResource(id = R.drawable.notification_icon),
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
fun DashboardPreview() {
    PreviewDependencies {
        Dashboard(
            state = DashboardState(
                treesRemainingToSync = 51,
                treesSynced = 146,
                totalTreesToSync = 200,
                isOrgButtonEnabled = true,
                showUnreadMessageNotification = true,
            ),
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
