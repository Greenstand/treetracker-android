package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

@ExperimentalComposeApi
object TextStyles {

    val DarkText = TextStyle(
        color = AppColors.GrayShadow,
        fontSize = TextUnit(24f, TextUnitType.Sp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun BoxScope.TopBarTitle() {
    Text(
        "TREETRACKER",
        modifier = Modifier.align(Alignment.Center),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}
