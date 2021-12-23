package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R

@OptIn(ExperimentalUnitApi::class)
object TextStyles {

    val DarkText = TextStyle(
        color = AppColors.GrayShadow,
        fontSize = TextUnit(24f, TextUnitType.Sp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun BoxScope.TopBarTitle() {
   Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "Treetracker icon",
        modifier = Modifier
            .height(100.dp)
            .width(100.dp)
            .align(Alignment.Center)
            .padding(all = 15.dp)
    )
}
