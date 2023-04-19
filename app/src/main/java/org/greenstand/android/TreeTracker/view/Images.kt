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

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

@Composable
fun LocalImage(
    modifier: Modifier = Modifier,
    imagePath: String,
    contentDescription: String? = null,
    contentScale: ContentScale,
    placeHolder: @Composable (Modifier) -> Unit = { Box(modifier = it.background(Color.DarkGray)) },
) {
    val bitmap by loadLocalImage(imagePath = imagePath)
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } ?: placeHolder(modifier)
}

@Composable
fun loadLocalImage(
    imagePath: String,
): State<ImageBitmap?> {
    val scope = rememberCoroutineScope()
    return produceState<ImageBitmap?>(initialValue = null, imagePath) {
        scope.launch {
            try {
                value = BitmapFactory.decodeFile(imagePath).asImageBitmap()
            } catch (e: Exception) { }
        }
    }
}