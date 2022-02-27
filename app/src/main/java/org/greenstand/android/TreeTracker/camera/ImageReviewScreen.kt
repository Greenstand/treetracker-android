package org.greenstand.android.TreeTracker.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.signup.getComposeViewModelOwner
import org.greenstand.android.TreeTracker.view.*
import org.koin.androidx.compose.getKoin
import org.koin.androidx.compose.getViewModel
import org.koin.core.qualifier.named

@Composable
fun ImageReviewScreen(photoPath: String) {

    val navController = LocalNavHostController.current
    val scope = getKoin().getOrCreateScope("SIGN_UP_SCOPE", named("SIGN_UP"));
    val viewModel = getViewModel<SignupViewModel>(
        owner = getComposeViewModelOwner(),
        scope = scope
    )
    val scopeView = rememberCoroutineScope()

    Scaffold(
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
                        navController.navigate(NavRoute.Selfie.route) {
                            launchSingleTop = true
                            popUpTo(NavRoute.Selfie.route) { inclusive = true }
                        }
                    },
                    approval = false
                )
                ApprovalButton(
                    onClick = {
                        scopeView.launch {
                            viewModel.createUser(photoPath)?.let { user ->
                                if (user.isPowerUser) {
                                    // In initial signup flow, clear stack and go to dashboard
                                    navController.navigate(NavRoute.Dashboard.route) {
                                        popUpTo(NavRoute.Language.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    // In tracking flow, clear login stack and go to wallet selection flow
                                    navController.navigate(NavRoute.WalletSelect.create(user.id)) {
                                        popUpTo(NavRoute.SignupFlow.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    },
                    approval = true
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