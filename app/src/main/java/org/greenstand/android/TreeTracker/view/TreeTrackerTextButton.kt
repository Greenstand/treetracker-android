package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun TreeTrackerTextButton(
    modifier: Modifier = Modifier,
    stringRes: Int,
    onClick: () -> Unit,
) {
    // TODO customize button visuals
    Button(
        onClick = onClick,
        modifier = modifier.size(height = 46.dp, width = 110.dp)
    ) {
        Text(
            text = stringResource(id = stringRes)
        )
    }
}