/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.view.AppColors

@Immutable
data class CustomTextColors(
    val lightText: Color,
    val darkText: Color,
    val primaryText: Color,
    val uploadText: Color,
)

@Immutable
data class CustomTypography(
    val small: TextStyle,
    val regular: TextStyle,
    val medium: TextStyle,
    val large: TextStyle
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomTextColors(
        lightText = Color.Unspecified,
        darkText = Color.Unspecified,
        primaryText = Color.Unspecified,
        uploadText = Color.Unspecified
    )
}
val LocalCustomTypography = staticCompositionLocalOf {
    CustomTypography(
        small = TextStyle.Default,
        regular = TextStyle.Default,
        medium = TextStyle.Default,
        large = TextStyle.Default
    )
}

object CustomTheme {
    val textColors: CustomTextColors
        @Composable
        get() = LocalCustomColors.current
    val typography: CustomTypography
        @Composable
        get() = LocalCustomTypography.current
}

@Composable
fun CustomTheme(
    textColors: CustomTextColors = CustomTheme.textColors,
    typography: CustomTypography = CustomTheme.typography,
    content: @Composable () -> Unit
) {
    val montserrat = FontFamily(
        Font(R.font.montserrat),
        Font(R.font.montserrat_bold, FontWeight.Bold)
    )
    val textColors = CustomTextColors(
        lightText = Color(0xFFF0F0F0),
        primaryText = AppColors.Green,
        darkText = Color(0xFF191C1F),
        uploadText = Color(0xFFF19400)
    )
    val customTypography = CustomTypography(
        small = TextStyle(fontSize = 12.sp, fontFamily = montserrat),
        regular = TextStyle(fontSize = 14.sp, fontFamily = montserrat),
        medium = TextStyle(fontSize = 16.sp, fontFamily = montserrat),
        large = TextStyle(fontSize = 24.sp, fontFamily = montserrat)
    )
    CompositionLocalProvider(
        LocalCustomColors provides textColors,
        LocalCustomTypography provides customTypography,
        content = content
    )
}