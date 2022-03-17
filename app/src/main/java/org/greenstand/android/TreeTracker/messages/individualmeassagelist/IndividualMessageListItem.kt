package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.SelectableImageDetail

@Composable
fun IndividualMessageItem(
    isSelected: Boolean,
    isNotificationEnabled: Boolean,
    messageTypeText: String,
    text: String,
    icon: Int,
    onClick: () -> Unit
) {
    SelectableImageDetail(
        isSelected = isSelected,
        buttonColors = AppButtonColors.MessagePurple,
        selectedColor = AppColors.Purple,
        header = { header(painter = painterResource(id = icon), messageTypeText = messageTypeText) },
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                color = CustomTheme.textColors.lightText,
                style = CustomTheme.typography.small,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isNotificationEnabled) {
                Image(
                    modifier = Modifier
                        .size(33.dp)
                        .align(Alignment.TopEnd),
                    painter = painterResource(id = R.drawable.notification_icon),
                    contentDescription = null
                )
            }
        }
    }
}
@Composable
fun header(painter: Painter,
           messageTypeText: String,){
    Box(
        modifier = Modifier
            .background(AppColors.Purple)
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Inside
            )
            Text(
                text = messageTypeText.uppercase(),
                color = CustomTheme.textColors.darkText,
                fontWeight = FontWeight.Bold,
                style = CustomTheme.typography.regular,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 110.dp)
            )
    }

}