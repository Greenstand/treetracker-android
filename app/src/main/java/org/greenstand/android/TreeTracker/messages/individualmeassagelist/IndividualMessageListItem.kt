/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    iconPadding: PaddingValues? = null,
    onClick: () -> Unit
) {
    SelectableImageDetail(
        isSelected = isSelected,
        buttonColors = AppButtonColors.MessagePurple,
        selectedColor = AppColors.Purple,
        header = {
            Header(
                painter = painterResource(id = icon),
                messageTypeText = messageTypeText,
                iconPadding = iconPadding,
            )
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(3f),
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
                        .weight(1f),
                    painter = painterResource(id = R.drawable.notification_icon),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun Header(
    painter: Painter,
    messageTypeText: String,
    iconPadding: PaddingValues?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(AppColors.Purple),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = iconPadding?.let { Modifier.padding(iconPadding) } ?: Modifier,
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