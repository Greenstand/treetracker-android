package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.theme.CustomTheme

@Composable
fun ProfileField(
    label: String,
    value: String,
    editable: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Text(label, style = CustomTheme.typography.medium, color = CustomTheme.textColors.lightText)
        if (editable) {
            BorderedTextField(
                padding = PaddingValues(top = 8.dp),
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(value, style = CustomTheme.typography.large)
        }
    }
}