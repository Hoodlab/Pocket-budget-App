package org.hoods.pocket.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object PocketColors {
    val Primary = Color(0xFF005136)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF006C49)
    val OnPrimaryContainer = Color(0xFF93EABE)
    val Surface = Color(0xFFEEFDF2)
    val OnSurface = Color(0xFF121E18)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFE8F7EC)
    val SurfaceContainer = Color(0xFFE3F2E7)
    val SurfaceContainerHigh = Color(0xFFDDECE1)
    val OnSurfaceVariant = Color(0xFF3F4943)
    val Outline = Color(0xFF6F7A72)
    val OutlineVariant = Color(0xFFBEC9C0)
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)

    // Semantic tokens
    val Income = Color(0xFF059669)
    val Expense = Color(0xFFBA1A1A)
}

private val LightColorScheme = lightColorScheme(
    primary = PocketColors.Primary,
    onPrimary = PocketColors.OnPrimary,
    primaryContainer = PocketColors.PrimaryContainer,
    onPrimaryContainer = PocketColors.OnPrimaryContainer,
    surface = PocketColors.Surface,
    onSurface = PocketColors.OnSurface,
    surfaceContainerLowest = PocketColors.SurfaceContainerLowest,
    surfaceContainer = PocketColors.SurfaceContainer,
    surfaceContainerHigh = PocketColors.SurfaceContainerHigh,
    onSurfaceVariant = PocketColors.OnSurfaceVariant,
    outline = PocketColors.Outline,
    outlineVariant = PocketColors.OutlineVariant,
    error = PocketColors.Error,
    onError = PocketColors.OnError,
)

private val PocketShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val PocketTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.05.em
    ),
)

@Composable
fun PocketTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = PocketShapes,
        typography = PocketTypography,
        content = content
    )
}
