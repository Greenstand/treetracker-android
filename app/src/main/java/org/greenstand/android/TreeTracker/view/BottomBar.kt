package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.ui.colorPrimaryDark

@Composable
fun ActionBar(
    leftAction: @Composable (BoxScope.() -> Unit) = { },
    centerAction: @Composable (BoxScope.() -> Unit) = { },
    rightAction: @Composable (BoxScope.() -> Unit) = { },
) {
    BottomAppBar(
        elevation = 16.dp,
        backgroundColor = colorPrimaryDark,
        contentColor = colorPrimaryDark
    ) {
        Box(modifier = Modifier.weight(1f)) {
            leftAction()
        }
        Box(modifier = Modifier.weight(1f)) {
            centerAction()
        }
        Box(modifier = Modifier.weight(1f)) {
            rightAction()
        }
    }
}
