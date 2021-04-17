package org.greenstand.android.TreeTracker.view

import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

object AppColors {
    val Gray = Color(0XFF191C1F)
    val GrayShadow = Color.Black

    val Green = Color(0xFF75B926)
    val GreenShadow = Color(0xFF507924)

    val GreenDisabled = Color(0xAD52811A)
    val GreenShadowDisabled = Color(0xAA273C12)

    val Purple = Color(0XFFC614A7)
    val PurpleShadow = Color(0XFF7C0868)

    val Red = Color(0XFFEA2525)
    val RedShadow = Color(0XFFA20000)

    val Orange = Color(0XFFF19400)
    val OrangeShadow = Color(0XFFEA6225)
}

object AppButtonColors {

    val Default = DepthButtonColors(
        color = AppColors.Gray,
        shadowColor = AppColors.GrayShadow,
        disabledColor = AppColors.GrayShadow,
        disabledShadowColor = AppColors.GrayShadow
    )

    val ProgressGreen = DepthButtonColors(
        color = AppColors.Green,
        shadowColor = AppColors.GreenShadow,
        disabledColor = AppColors.GreenDisabled,
        disabledShadowColor = AppColors.GreenShadowDisabled
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