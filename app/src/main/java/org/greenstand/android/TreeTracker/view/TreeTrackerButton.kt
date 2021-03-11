package org.greenstand.android.TreeTracker.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.utilities.Padding

@Composable
fun TreeTrackerButton(
    onClick: () -> Unit,
    padding: Padding = Padding(),
    @StringRes textRes: Int,
    isEnabled: Boolean = true
) {

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                top = padding.top,
                start = padding.start,
                end = padding.end,
                bottom = padding.bottom
            ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = treeTrackerGreen
        ),
        shape = RoundedCornerShape(5.dp),
        enabled = isEnabled

    ) {
        Text(text = stringResource(id = textRes))
    }
}

@Composable
@Preview
private fun TreeTrackerButton_Preview() {
    TreeTrackerButton(
        onClick = {},
        padding = Padding(),
        textRes = R.string.hello_world
    )
}
