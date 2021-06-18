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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.DepthButton
import org.greenstand.android.TreeTracker.view.DepthButtonColors
import org.greenstand.android.TreeTracker.view.LocalImage
import org.greenstand.android.TreeTracker.view.TextButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserSelect(
    navigationButtonColors: DepthButtonColors,
    isCreateUserEnabled: Boolean,
    isNotificationEnabled: Boolean,
    onNextRoute: (User) -> String,
) {
    val viewModel: UserSelectViewModel = viewModel(factory = LocalViewModelFactory.current)
    val navController = LocalNavHostController.current
    val state by viewModel.state.observeAsState(UserSelectState())

    Scaffold(
        bottomBar = {
            ActionBar(
                centerAction = {
                    if (isCreateUserEnabled) {
                        TextButton(
                            modifier = Modifier.align(Alignment.Center),
                            stringRes = R.string.create_user,
                            onClick = { navController.navigate(NavRoute.SignupFlow.route) }
                        )
                    }
                },
                rightAction = {
                    ArrowButton(
                        isLeft = false,
                        isEnabled = state.selectedUser != null,
                        colors = navigationButtonColors,
                        onClick = {
                            state.selectedUser?.let {
                                navController.navigate(onNextRoute(it))
                            }
                        }
                    )
                },
                leftAction = {
                    ArrowButton(
                        isLeft = true,
                        colors = navigationButtonColors,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                }
            )
        }
    ) {
        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            modifier = Modifier.padding(it), // Padding for bottom bar.
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp)
        ) {
            items(state.users) { user ->
                UserButton(
                    user = user,
                    isSelected = state.selectedUser?.id == user.id,
                ) { viewModel.selectUser(user) }
            }
        }
    }
}

@Composable
fun UserButton(
    user: User,
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
                .padding(bottom = 12.dp) // Bottom side can be clipped by the button.
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // User profile picture.
            LocalImage(
                imagePath = user.photoPath,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .aspectRatio(1.0f)
                    .clip(RoundedCornerShape(percent = 10)),
            )

            // User text data: Name, phone number, and token count.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // User name and phone number.
                Text(
                    text = "${user.firstName} ${user.lastName}\n${user.wallet}",
                    color = AppColors.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif, // TODO: Change font to Montserrat.
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box( // TODO: Change into 'Plant' icon.
                        modifier = Modifier
                            .size(width = 20.dp, height = 22.dp)
                            .background(AppColors.LightGray)
                    ) // Box placeholder for 'Plant' icon.

                    Text(
                        text = "1,234", // TODO: Fetch user's token count.
                        modifier = Modifier.padding(start = 4.dp),

                        color = AppColors.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                    ) // Text placeholder for number of tokens.
                }
            }
        }
    }
}
