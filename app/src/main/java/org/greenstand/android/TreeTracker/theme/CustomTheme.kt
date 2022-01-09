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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.view.AppColors

@Immutable
data class CustomColors(
    val white: Color,
    val black: Color,
    val green: Color,
)

@Immutable
data class CustomTypography(
    val small: TextStyle,
    val medium: TextStyle
)

@Immutable
data class CustomElevation(
    val default: Dp,
    val pressed: Dp
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        white = Color.Unspecified,
        black = Color.Unspecified,
        green = Color.Unspecified
    )
}
val LocalCustomTypography = staticCompositionLocalOf {
    CustomTypography(
        small = TextStyle.Default,
        medium = TextStyle.Default
    )
}
val LocalCustomElevation = staticCompositionLocalOf {
    CustomElevation(
        default = Dp.Unspecified,
        pressed = Dp.Unspecified
    )
}

@Composable
fun CustomTheme(
    /* ... */
    content: @Composable () -> Unit
) {
    val montserrat = FontFamily(
        Font(R.font.montserrat),
        Font(R.font.montserrat_bold, FontWeight.Bold)
    )
    val textColors = CustomColors(
        white = Color(0xFFF0F0F0),
        green = AppColors.Green,
        black = Color(0xFF191C1F)
    )
    val customTypography = CustomTypography(
        small = TextStyle(fontSize = 12.sp, fontFamily = montserrat),
        medium = TextStyle(fontSize = 16.sp)
    )
    val customElevation = CustomElevation(
        default = 4.dp,
        pressed = 8.dp
    )
    CompositionLocalProvider(
        LocalCustomColors provides textColors,
        LocalCustomTypography provides customTypography,
        LocalCustomElevation provides customElevation,
        content = content
    )
}


object CustomTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
    val typography: CustomTypography
        @Composable
        get() = LocalCustomTypography.current
    val elevation: CustomElevation
        @Composable
        get() = LocalCustomElevation.current
}