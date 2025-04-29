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
package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.theme.CustomTheme

@Composable
fun BorderedTextField(
    padding: PaddingValues = PaddingValues(0.dp),
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onFocusChanged: ((FocusState) -> Unit) = {},
    focusRequester: FocusRequester = FocusRequester.Default,
    autofocusEnabled: Boolean = false,
    onClick: () -> Unit = {},
    readOnly: Boolean = false,
    isEnabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .padding(padding)
            .border(
                BorderStroke(0.5.dp, SolidColor(Color.White)),
                RoundedCornerShape(16.dp),
            )
            .padding(padding)

    ) {
        TextField(
            modifier = Modifier
                .padding(8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged(onFocusChanged)
                .clickable { onClick() },
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                backgroundColor = MaterialTheme.colors.primary,
                cursorColor = CustomTheme.textColors.lightText
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            readOnly = readOnly,
            enabled = isEnabled
        )
    }

    LaunchedEffect(Unit) {
        if (autofocusEnabled) {
            focusRequester.requestFocus()
        }
    }
}