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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.UserImageButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    imagePath: String,
) {
    TreeTrackerButton(
        modifier =
            modifier
                .align(Alignment.Center)
                .width(100.dp)
                .height(100.dp)
                .padding(
                    start = 15.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = 10.dp,
                ).aspectRatio(1.0f)
                .clip(RoundedCornerShape(10.dp)),
        onClick = onClick,
    ) {
        LocalImage(
            modifier =
                Modifier
                    .padding(bottom = 12.dp, end = 1.dp)
                    .fillMaxSize()
                    .aspectRatio(1.0f)
                    .clip(RoundedCornerShape(10.dp)),
            imagePath = imagePath,
            contentScale = ContentScale.Crop,
        )
    }
}
