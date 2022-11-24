package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.theme.CustomTheme

@Composable
fun NoMessages() {

    Column(
        modifier = Modifier
            .padding(bottom = 30.dp)
            .fillMaxSize()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.open_mailbox_with_lowered_flag),
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.no_messages_yet),
            color = CustomTheme.textColors.lightText,
            textAlign = TextAlign.Center,
            style = CustomTheme.typography.large,
            fontWeight = FontWeight.Bold,
        )
    }
}