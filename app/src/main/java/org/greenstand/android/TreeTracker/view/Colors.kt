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
package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

object AppColors {
    val Gray = Color(0XFF191C1F)
    val GrayShadow = Color.Black

    val LightGray = Color(0xFFD8D8D8)

    val Green = Color(0xFF75B926)
    val GreenShadow = Color(0xFF507924)
    val GreenDisabled = Color(0xAD52811A)
    val GreenShadowDisabled = Color(0xAA273C12)

    val Purple = Color(0XFFC614A7)
    val PurpleShadow = Color(0XFF7C0868)
    val PurpleDisabled = Color(0XFF5E0753)
    val PurpleShadowDisabled = Color(0XFF33032D)

    val Red = Color(0XFFEA2525)
    val RedShadow = Color(0XFFA20000)
    val RedDisabled = Color(0xFF590707)
    val RedShadowDisabled = Color(0xFF430404)

    val Yellow = Color(0XFFFFC108)
    val YellowShadow = Color(0XFFA37A02)

    val SkyBlue = Color(0XFF03A9F6)
    val SkyBlueShadow = Color(0XFF0272A5)

    val Orange = Color(0XFFF19400)
    val OrangeShadow = Color(0XFFEA6225)

    val MessageAuthorBackground = Color(0XFF61892F)
    val MessageReceivedBackground = Color(0XFF303131)

    val MediumGray = Color(0xFF9E9E9E)
    val DeepGray = Color(0xFF333333)
}

object AppButtonColors {

    val Default = DepthButtonColors(
        color = AppColors.Gray,
        shadowColor = AppColors.GrayShadow,
        disabledColor = AppColors.GrayShadow,
        disabledShadowColor = AppColors.GrayShadow
    )

    val WhiteLight = DepthButtonColors(
        color = Color.White,
        shadowColor = AppColors.MediumGray,
        disabledColor = AppColors.MediumGray,
        disabledShadowColor = AppColors.Gray
    )

    val ProgressGreen = DepthButtonColors(
        color = AppColors.Green,
        shadowColor = AppColors.GreenShadow,
        disabledColor = AppColors.GreenDisabled,
        disabledShadowColor = AppColors.GreenShadowDisabled
    )
    val DeclineRed = DepthButtonColors(
        color = AppColors.Red,
        shadowColor = AppColors.RedShadow,
        disabledColor = AppColors.RedDisabled,
        disabledShadowColor = AppColors.RedShadowDisabled
    )

    val MessagePurple = DepthButtonColors(
        color = AppColors.Purple,
        shadowColor = AppColors.PurpleShadow,
        disabledColor = AppColors.PurpleDisabled,
        disabledShadowColor = AppColors.PurpleShadowDisabled
    )

    val UploadOrange = DepthButtonColors(
        color = AppColors.Orange,
        shadowColor = AppColors.OrangeShadow,
        disabledColor = AppColors.GrayShadow,
        disabledShadowColor = AppColors.GrayShadow
    )
    val Yellow = DepthButtonColors(
        color = AppColors.Yellow,
        shadowColor = AppColors.YellowShadow,
        disabledColor = AppColors.GrayShadow,
        disabledShadowColor = AppColors.GrayShadow
    )
    val SkyBlue = DepthButtonColors(
        color = AppColors.SkyBlue,
        shadowColor = AppColors.SkyBlueShadow,
        disabledColor = AppColors.GrayShadow,
        disabledShadowColor = AppColors.GrayShadow
    )
}

val Colors = darkColors(
    primary = AppColors.Gray,
    primaryVariant = AppColors.GrayShadow,
    onPrimary = AppColors.Green,
    secondary = Color.Blue,
    onSecondary = AppColors.Green,
    background = AppColors.Gray,
    onBackground = AppColors.Green,
    surface = AppColors.Gray,
    onSurface = AppColors.Green,
    error = Color.Red,
    onError = AppColors.Green
)

@Preview
@Composable
fun ColorPalettePreview() {
    Column {
        Row {
            Palette("Gray", AppColors.Gray)
            Palette("GrayShadow", AppColors.GrayShadow)
            Palette("LightGray", AppColors.LightGray)
            Palette("MediumGray", AppColors.MediumGray)
        }
        Row {
            Palette("Gray", AppColors.Green)
            Palette("GrayShadow", AppColors.GreenDisabled)
            Palette("LightGray", AppColors.GreenShadow)
            Palette("MediumGray", AppColors.GreenShadowDisabled)
        }
        Row {
            Palette("Red", AppColors.Red)
            Palette("RedDisabled", AppColors.RedDisabled)
            Palette("RedShadow", AppColors.RedShadow)
            Palette("RedShadowDisabled", AppColors.RedShadowDisabled)
        }
        Row {
            Palette("Purple", AppColors.Purple)
            Palette("PurpleDisabled", AppColors.PurpleDisabled)
            Palette("PurpleShadow", AppColors.PurpleShadow)
            Palette("PurpleShadowDisabled", AppColors.PurpleShadowDisabled)
        }
        Row {
            Palette("Orange", AppColors.Orange)
            Palette("OrangeShadow", AppColors.OrangeShadow)
            Palette("Yellow", AppColors.Yellow)
            Palette("YellowShadow", AppColors.YellowShadow)
        }
        Row {
            Palette("SkyBlue", AppColors.SkyBlue)
            Palette("SkyBlueShadow", AppColors.SkyBlueShadow)
        }
    }
}

@Composable
private fun Palette(name: String, color: Color) {
    Column(
        modifier = Modifier
            .size(100.dp)
            .background(color)
    ) {
        Text(text = name)
        Text(text = "#${Integer.toHexString(color.toArgb()).uppercase()}")
    }
}