package org.greenstand.android.TreeTracker.view

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R

@Composable
fun Bar() {
    ActionBar(
        leftAction = {
            TextButton(
                modifier = Modifier.align(Alignment.Center),
                stringRes = R.string.language,
                onClick = {  }
            )
                     },
        centerAction = {
            Text("HELLO!", modifier = Modifier.align(Alignment.Center),)
                       },
        rightAction = {
            TextButton(
                modifier = Modifier.align(Alignment.Center),
                stringRes = R.string.language,
                onClick = {  }
            )
        })
}

@Composable
fun ActionBar(
    leftAction: @Composable (BoxScope.() -> Unit),
    centerAction: @Composable (BoxScope.() -> Unit),
    rightAction: @Composable (BoxScope.() -> Unit),
) {
    BottomAppBar(
        elevation = 16.dp,
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


@Composable
fun TextButton(
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