package org.greenstand.android.TreeTracker.view

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration

class ConsumableSnackBar(
    private val value: Int,
    private val duration: SnackbarDuration = SnackbarDuration.Short
) {
    private var isConsumed = false

    suspend fun show(context: Context, scaffoldState: ScaffoldState) {
        if (!isConsumed) {
            isConsumed = true
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState.showSnackbar(
                message = context.getString(value),
                duration = duration
            )
        }
    }
}