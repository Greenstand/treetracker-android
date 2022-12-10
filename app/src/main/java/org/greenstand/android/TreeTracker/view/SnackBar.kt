package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.theme.CustomTheme

@Composable
fun CustomSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { },
    backGroundColor: Color = AppColors.Green,
) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                backgroundColor = backGroundColor,
                modifier = modifier.padding(start = 16.dp, end = 16.dp),
                content = {
                    Text(
                        text = data.message,
                        style = CustomTheme.typography.regular,
                        color = CustomTheme.textColors.darkText
                    )
                },
                action = {
                    data.actionLabel?.let { actionLabel ->
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = actionLabel,
                                style = CustomTheme.typography.regular,
                                color = CustomTheme.textColors.darkText
                            )
                        }
                    }
                }
            )
        }
    )
}