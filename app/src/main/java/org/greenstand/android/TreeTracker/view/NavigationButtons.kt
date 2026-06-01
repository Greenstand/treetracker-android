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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R

@Composable
fun BoxScope.ArrowButton(
    isEnabled: Boolean = true,
    colors: ButtonColors = AppButtonColors.ProgressGreen,
    isLeft: Boolean,
    onClick: () -> Unit,
) {
    TreeTrackerButton(
        isEnabled = isEnabled,
        colors = colors,
        modifier =
            Modifier
                .align(Alignment.Center)
                .size(height = 62.dp, width = 62.dp),
        onClick = onClick,
    ) {
        Image(
            modifier =
                Modifier
                    .size(height = 45.dp, width = 45.dp),
            painter = arrowPainter(isLeft = isLeft),
            contentDescription = null,
        )
    }
}

@Composable
private fun arrowPainter(isLeft: Boolean): Painter =
    if (isLeft) {
        painterResource(id = R.drawable.arrow_backward)
    } else {
        painterResource(id = R.drawable.arrow_forward)
    }
