package org.greenstand.android.TreeTracker.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.io.File

@Composable
fun CameraScreen(
    isSelfieMode: Boolean,
    onImageResult: (File) -> Unit = {}
) {

    val cameraControl = remember { CameraControl() }
    Camera(
        isSelfieMode = isSelfieMode,
        cameraControl = cameraControl,
        onImageCaptured = {
            onImageResult(it)
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Button(onClick = { cameraControl.captureImage() }) {
            Text("Take Picture")
        }
    }
}