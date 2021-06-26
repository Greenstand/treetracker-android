package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppButtonColors
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DashBoardButton(
                text = "Upload",
                colors = AppButtonColors.UploadOrange,
                onClick = {
                    viewModel.sync()
                }
            )
            DashBoardButton(
                text = "Messages",
                colors = AppButtonColors.MessagePurple,
                onClick = {
                    navController.navigate(NavRoute.MessagesUserSelect.route)
                }
            )
            DashBoardButton(
                text = "Track",
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
fun DashBoardButton(text: String, onClick: () -> Unit, colors: ButtonColors) {
    DepthButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
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
