package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BorderedTextField(
    borderSize: Dp = 1.dp,
    borderColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(8.dp),
    value: String,
    onValueChangeListener: (String) -> Unit,
    placeholder: @Composable (() -> Unit)? = null
) {
    TextField(
        modifier = Modifier.padding(top = 16.dp).border(BorderStroke(borderSize, SolidColor(borderColor)), shape),
        value = value,
        onValueChange = onValueChangeListener,
        placeholder = placeholder
    )
}
