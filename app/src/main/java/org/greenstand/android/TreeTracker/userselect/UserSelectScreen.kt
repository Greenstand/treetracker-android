package org.greenstand.android.TreeTracker.userselect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.navigate
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.LocalNavHostController
import org.greenstand.android.TreeTracker.activities.LocalViewModelFactory
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.greenstand.android.TreeTracker.view.TextButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserSelectScreen(
    viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(UserSelectState())

    Scaffold(
        bottomBar = {
            ActionBar(
                centerAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.create_user,
                        onClick = { navController.navigate(NavRoute.SignupFlow.route) }
                    )
                },
                rightAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.next,
                        enabled = state.selectedPlanter != null,
                        onClick = {
                            state.selectedPlanter?.id?.let {
                                navController.navigate(NavRoute.WalletSelect.create(it))
                            }
                        }
                    )
                },
                leftAction = {
                    TextButton(
                        modifier = Modifier.align(Alignment.Center),
                        stringRes = R.string.back,
                        onClick = { navController.popBackStack() }
                    )
                }
            )
        }
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            modifier = Modifier.padding(it),  // Padding for bottom bar.
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp)
        ) {
            items(state.planters) { user ->
                UserButton(
                    user = user,
                    isSelected = state.selectedPlanter?.id == user.id,
                ) { viewModel.selectUser(user) }
            }
        }
    }
}

@Composable
fun UserButton(
    user: PlanterInfoEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DepthButton(
        onClick = onClick,
        isSelected = isSelected,

        modifier = Modifier
            .padding(8.dp)
            .width(156.dp)
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter,

        colors = DepthButtonColors(
            color = AppColors.Gray,
            shadowColor = if (isSelected) AppColors.GreenShadow else AppColors.GrayShadow,
            disabledColor = AppColors.GrayShadow,
            disabledShadowColor = AppColors.GrayShadow
        )
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp)  // Bottom side can be clipped by the button.
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // User profile picture.
            Box(  // TODO: Fetch user profile picture.
                modifier = Modifier
                    .padding(1.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.0f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(AppColors.LightGray)
            )  // Box placeholder for user profile picture.

            // User text data: Name, phone number, and token count.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // User name and phone number.
                Text(
                    text = "${user.firstName}\n${user.phone}",
                    color = AppColors.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,  // TODO: Change font to Montserrat.
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(  // TODO: Change into 'Plant' icon.
                        modifier = Modifier
                            .size(width = 20.dp, height = 22.dp)
                            .background(AppColors.LightGray)
                    )  // Box placeholder for 'Plant' icon.

                    Text(
                        text = "1,234",  // TODO: Fetch user's token count.
                        modifier = Modifier.padding(start = 4.dp),

                        color = AppColors.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                    )  // Text placeholder for number of tokens.
                }
            }
        }
    }
}
