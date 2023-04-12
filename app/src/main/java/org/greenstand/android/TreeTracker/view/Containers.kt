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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun RoundedImageContainer(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable (() -> Unit),
) {
    Box(
        modifier = Modifier
            .width(70.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .clipToBounds() then modifier,
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

@Composable
fun RoundedLocalImageContainer(
    modifier: Modifier = Modifier,
    imagePath: String,
    contentAlignment: Alignment = Alignment.Center,
) {
    RoundedImageContainer(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        LocalImage(
            modifier = Modifier
                .fillMaxSize(),
            imagePath = imagePath,
            contentScale = ContentScale.Crop
        )
    }
}