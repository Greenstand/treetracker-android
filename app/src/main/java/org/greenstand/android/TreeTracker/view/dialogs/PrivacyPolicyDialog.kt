package org.greenstand.android.TreeTracker.view.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.settings.SettingsViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ApprovalButton

@Composable
fun PrivacyPolicyDialog(
    signupViewModel: SignupViewModel? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    Column(
        modifier = Modifier.Companion
            .padding(start = 30.dp, end = 30.dp, top = 10.dp, bottom = 40.dp)
            .fillMaxSize()
            .padding(2.dp)
            .border(1.dp, color = AppColors.Green, shape = RoundedCornerShape(percent = 10))
            .clip(RoundedCornerShape(percent = 10))
            .background(color = AppColors.Gray)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.Companion
                .weight(0.8f)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.greenstand_logo),
                contentDescription = null,
                modifier = Modifier.Companion
                    .size(width = 20.dp, height = 20.dp)
            )
            Spacer(modifier = Modifier.Companion.width(5.dp))
            Text(
                text = stringResource(R.string.privacy_policy),
                color = CustomTheme.textColors.primaryText,
                style = CustomTheme.typography.large,
                fontWeight = FontWeight.Companion.Bold,
            )
        }
        Text(
            text = stringResource(id = R.string.policy_text_blob),
            modifier = Modifier.Companion
                .padding(bottom = 15.dp)
                .weight(9f)
                .verticalScroll(rememberScrollState())
        )
        ApprovalButton(
            modifier = Modifier.Companion
                .weight(0.8f)
                .size(50.dp)
                .align(Alignment.Companion.CenterHorizontally),
            onClick = {
                if (signupViewModel != null) {
                    signupViewModel.closePrivacyPolicyDialog()
                } else {
                    settingsViewModel?.setPrivacyDialogVisibility(false)
                }
            },
            approval = true
        )
    }
}