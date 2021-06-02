package org.greenstand.android.TreeTracker.view

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun LocalImage(
    modifier: Modifier = Modifier,
    imagePath: String,
    contentDescription: String? = null,
) {
    val bitmap by loadLocalImage(imagePath = imagePath)
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

@Composable
fun loadLocalImage(
    imagePath: String,
): State<ImageBitmap?> {
    return produceState(initialValue = null, imagePath) {
        try {
            value = BitmapFactory.decodeFile(imagePath).asImageBitmap()
        } catch (e: Exception) { }
    }
}
