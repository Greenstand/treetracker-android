package org.greenstand.android.TreeTracker.camera

import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import org.greenstand.android.TreeTracker.activities.LocalNavHostController

@Composable
fun CameraScreen(
    isSelfieMode: Boolean,
    onImageResult: (String) -> Unit = {}
) {

    val cameraControl = remember { CameraControl() }
    Camera(
        isSelfieMode = isSelfieMode,
        cameraControl = cameraControl,
        onImageCaptured = {
            Log.d("JONATHAN", "Image taken: $it")
            onImageResult(it)
//            navBackStackEntry?.savedStateHandle?.set<String>("imagePath", it)
//            val path = navBackStackEntry?.savedStateHandle?.get<String>("imagePath")
//            Log.d("JONATHAN", "$path")
//            navController.popBackStack()
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