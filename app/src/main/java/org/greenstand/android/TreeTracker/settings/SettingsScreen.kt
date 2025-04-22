package org.greenstand.android.TreeTracker.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.signup.Credential
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.ActionBar
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.greenstand.android.TreeTracker.view.LanguageButton
import org.greenstand.android.TreeTracker.view.TopBarTitle

@Composable
fun SettingsScreen() {
    val navController = LocalNavHostController.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ActionBar(
                centerAction = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = AppColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = CustomTheme.typography.large,
                        textAlign = TextAlign.Center,
                    )
                },

            )
        },
        bottomBar = {
                ActionBar(
                    leftAction = {
                        ArrowButton(isLeft = true) {
                            navController.popBackStack()
                        }
                    },

                )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp,end= 20.dp)
        ) {
            SettingsItem(
                iconResId = R.drawable.account, // Replace with your profile icon
                titleResId = R.string.profile_title,
                descriptionResId = R.string.profile_description,
                onClick = {
                    navController.navigate(NavRoute.ProfileSelect.route)
                }
            )
            Divider(color = Color.White)

            SettingsItem(
                iconResId = R.drawable.privacy_policy, // Replace with your privacy icon
                titleResId = R.string.privacy_title,
                descriptionResId = R.string.privacy_description,
                onClick = { /* Handle privacy click */ }
            )
            Divider(color = Color.White)

            SettingsItem(
                iconResId = R.drawable.logout, // Replace with your logout icon
                titleResId = R.string.logout_title,
                descriptionResId = R.string.logout_description,
                onClick = { /* Handle logout click */ }
            )
            Divider(color = Color.White)

            SettingsItem(
                iconResId = R.drawable.delete, // Replace with your delete icon
                titleResId = R.string.delete_account_title,
                descriptionResId = R.string.delete_account_description,
                onClick = { /* Handle delete account click */ }
            )
        }
    }
}

@Composable
fun SettingsItem(
    iconResId: Int,
    titleResId: Int,
    descriptionResId: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null, // decorative element
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(id = titleResId),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White

            )
            Text(
                text = stringResource(id = descriptionResId),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}