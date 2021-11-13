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
