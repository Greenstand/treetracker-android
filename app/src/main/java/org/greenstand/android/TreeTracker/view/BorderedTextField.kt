package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BorderedTextField(
    padding: PaddingValues = PaddingValues(0.dp),
    borderSize: Dp = 0.5.dp,
    borderColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(16.dp),
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.padding(padding).border(BorderStroke(borderSize, SolidColor(borderColor)), shape)
    ) {
        TextField(
            modifier = Modifier.padding(8.dp),
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent
            )
        )
    }
}