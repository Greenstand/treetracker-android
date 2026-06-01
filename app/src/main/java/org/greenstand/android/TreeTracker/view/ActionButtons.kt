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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R

/**
 * @param onClick The callback function for click event.
 * @param modifier The modifier to be applied to the layout.
 * @param approval Set the type of button to display(if approval is true, shows green thumps up button )
 */
@Composable
fun ApprovalButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    approval: Boolean,
) {
    val color = if (approval) AppButtonColors.ProgressGreen else AppButtonColors.DeclineRed
    val image =
        if (approval) painterResource(id = R.drawable.check) else painterResource(id = R.drawable.close)
    TreeTrackerButton(
        colors = color,
        modifier =
            modifier
                .size(height = 60.dp, width = 60.dp),
        onClick = onClick,
    ) {
        Image(
            painter = image,
            contentDescription = null,
        )
    }
}

@Suppress("UnusedParameter")
@Composable
fun InfoButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    shape: TreeTrackerButtonShape = TreeTrackerButtonShape.Circle,
) {
    TreeTrackerButton(
        modifier = modifier.size(60.dp),
        colors = AppButtonColors.WhiteLight,
        onClick = onClick,
        shape = TreeTrackerButtonShape.Circle,
        depth = 6f,
    ) {
        Image(
            painter = painterResource(id = R.drawable.info_icon),
            modifier =
                Modifier
                    .size(80.dp),
            contentDescription = null,
        )
    }
}
