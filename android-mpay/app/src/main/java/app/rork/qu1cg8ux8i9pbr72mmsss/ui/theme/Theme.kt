package app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Mpay {
    val Canvas = Color(0xFF0A0C10)
    val Surface = Color(0xFF161A22)
    val SurfaceHigh = Color(0xFF1F2430)
    val Outline = Color(0xFF2A303C)
    val TextPrimary = Color(0xFFF2F5F9)
    val TextSecondary = Color(0xFF8A93A3)
    val Lime = Color(0xFFC6FF3D)
    val LimeDim = Color(0xFF3A4A14)
    val OnLime = Color(0xFF0A0C10)
    val Expense = Color(0xFFFF5C6C)
    val Income = Color(0xFF4ADE80)
}

private val MpayColors = darkColorScheme(
    primary = Mpay.Lime,
    onPrimary = Mpay.OnLime,
    primaryContainer = Mpay.LimeDim,
    onPrimaryContainer = Mpay.Lime,
    secondary = Mpay.TextSecondary,
    onSecondary = Mpay.Canvas,
    secondaryContainer = Mpay.LimeDim,
    onSecondaryContainer = Mpay.Lime,
    background = Mpay.Canvas,
    onBackground = Mpay.TextPrimary,
    surface = Mpay.Canvas,
    onSurface = Mpay.TextPrimary,
    surfaceVariant = Mpay.Surface,
    onSurfaceVariant = Mpay.TextSecondary,
    surfaceContainerLowest = Mpay.Canvas,
    surfaceContainerLow = Mpay.Surface,
    surfaceContainer = Mpay.Surface,
    surfaceContainerHigh = Mpay.SurfaceHigh,
    surfaceContainerHighest = Mpay.SurfaceHigh,
    outline = Mpay.Outline,
    outlineVariant = Mpay.Outline,
    error = Mpay.Expense,
    onError = Mpay.Canvas,
)

private val MpayTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

val AmountStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MpayColors,
        typography = MpayTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
    ) {
        // Plain Text outside Surface/Scaffold must not fall back to black.
        CompositionLocalProvider(LocalContentColor provides Mpay.TextPrimary, content = content)
    }
}
