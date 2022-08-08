package org.greenstand.android.TreeTracker.view

import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

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

    val Yellow = Color(0XFFFFC108)
    val YellowShadow = Color(0XFFA37A02)

    val SkyBlue = Color(0XFF03A9F6)
    val SkyBlueShadow = Color(0XFF0272A5)

    val RedDisabled = Color(0xFF590707)
    val RedShadowDisabled = Color(0xFF430404)


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
