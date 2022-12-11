package org.greenstand.android.TreeTracker.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog

@Composable
fun NoGPSDeviceDialog(onPositiveClick: () -> Unit){
    CustomDialog(
        dialogIcon = painterResource(id = R.drawable.error_outline),
        title = stringResource(R.string.no_gps_device_header),
        textContent = stringResource(R.string.no_gps_device_content),
        onPositiveClick = onPositiveClick
    )
}