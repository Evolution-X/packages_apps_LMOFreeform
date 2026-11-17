package com.libremobileos.sidebar.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive design tokens.
 */
object M3ExpressiveTokens {
    val CornerNone = RoundedCornerShape(0.dp)
    val CornerExtraSmall = RoundedCornerShape(4.dp)
    val CornerSmall = RoundedCornerShape(8.dp)
    val CornerMedium = RoundedCornerShape(12.dp)
    val CornerLarge = RoundedCornerShape(16.dp)
    val CornerLargeIncreased = RoundedCornerShape(20.dp)
    val CornerExtraLarge = RoundedCornerShape(28.dp)
    val CornerExtraLargeIncreased = RoundedCornerShape(32.dp)
    val CornerExtraExtraLarge = RoundedCornerShape(48.dp)
    val CornerFull = CircleShape

    val ElevationLevel0 = 0.dp
    val ElevationLevel1 = 1.dp
    val ElevationLevel2 = 3.dp
    val ElevationLevel3 = 6.dp
    val ElevationLevel4 = 8.dp
    val ElevationLevel5 = 12.dp

    // Spatial springs overshoot for physical bounce on geometry changes.
    fun <T> spatialDefault(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> spatialFast(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> spatialSlow(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)

    // Effects springs carry color/opacity and must not overshoot (damping = 1.0).
    fun <T> effectsDefault(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 1600f)
    fun <T> effectsFast(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 3800f)
    fun <T> effectsSlow(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 800f)
}

/**
 * M3 Expressive Emphasized Typescale.
 * Same size and line height as baseline; heavier weights and adjusted tracking.
 */
object M3ExpressiveTypography {
    val TitleMediumEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val TitleSmallEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    val LabelLargeEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    val LabelMediumEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val LabelSmallEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val BodyLargeEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val BodyMediumEmphasized = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
}

val ExpressiveShapes = Shapes(
    extraSmall = M3ExpressiveTokens.CornerExtraSmall,
    small = M3ExpressiveTokens.CornerSmall,
    medium = M3ExpressiveTokens.CornerMedium,
    large = M3ExpressiveTokens.CornerLarge,
    extraLarge = M3ExpressiveTokens.CornerExtraLarge
)

@Composable
fun SidebarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        content = content
    )
}
