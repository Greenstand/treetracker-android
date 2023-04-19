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
package org.greenstand.android.TreeTracker.view.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.ApprovalButton

@Composable
/**
 * @param dialogIcon Icon to be displayed in the dialog.
 * @param title The Dialog's title text.
 * @param textContent The text content of the dialog. Can be left empty if it is an input dialog
 * @param content Composable's content. Allows you display other composable in the dialog as content
 * @param onPositiveClick The callback action for clicking the positive approval button.
 * @param onNegativeClick The callback action for clicking the negative approval button.
 * @param textInputValue The text content of the dialog. Can be left empty if it is an input dialog
 */
fun CustomDialog(
    dialogIcon: Painter? = painterResource(id = R.drawable.greenstand_logo),
    backgroundModifier: Modifier = Modifier,
    title: String = "",
    textContent: String? = null,
    content: @Composable() (() -> Unit)? = null,
    onPositiveClick: (() -> Unit)? = null,
    onNegativeClick: (() -> Unit)? = null,
    textInputValue: String = "",
    onTextInputValueChange: ((String) -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dialogIcon?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 16.dp, height = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    color = CustomTheme.textColors.primaryText,
                    style = CustomTheme.typography.medium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        modifier = backgroundModifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(2.dp)
            .border(1.dp, color = AppColors.Green, shape = RoundedCornerShape(percent = 10))
            .clip(RoundedCornerShape(percent = 10)),
        backgroundColor = AppColors.Gray,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                textContent?.let {
                    Text(
                        text = it,
                        color = CustomTheme.textColors.primaryText,
                        style = CustomTheme.typography.regular,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                onTextInputValueChange?.let {
                    TextField(
                        value = textInputValue,
                        modifier = Modifier.wrapContentHeight(),
                        onValueChange = onTextInputValueChange
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    content?.let { it() }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                onNegativeClick?.let {
                    ApprovalButton(
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .size(40.dp),
                        onClick = it,
                        approval = false
                    )
                }

                onPositiveClick?.let {
                    ApprovalButton(
                        modifier = Modifier
                            .size(40.dp),
                        onClick = it,
                        approval = true
                    )
                }
            }
        },
    )
}