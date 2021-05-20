package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.TopBarTitle() {
    Text(
        "TREETRACKER",
        modifier = Modifier.align(Alignment.Center),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}
