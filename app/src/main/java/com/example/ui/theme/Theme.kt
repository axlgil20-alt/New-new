package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = EmeraldGreenLight,
    onPrimaryContainer = EmeraldGreenDark,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = EmeraldGreenDark,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = SlateDark,
    onSurface = SlateDark,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = SlateMedium
)

val ClassicBlueColorScheme = lightColorScheme(
    primary = ClassicBlue,
    onPrimary = Color.White,
    primaryContainer = ClassicBlueLight,
    onPrimaryContainer = Color(0xFF084298),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = SlateDark,
    onSurface = SlateDark
)

val GoldColorScheme = lightColorScheme(
    primary = LuxuryGold,
    onPrimary = Color.White,
    primaryContainer = LuxuryGoldLight,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = SlateDark,
    onSurface = SlateDark
)

val SlateColorScheme = lightColorScheme(
    primary = SlateDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = SlateMedium,
    onSecondary = Color.White,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = SlateDark,
    onSurface = SlateDark
)

// Esquemas de Color para Modo Nocturno (Dark Theme)
val DarkEmeraldColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF04382A),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF032F23),
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

val DarkClassicBlueColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0C4A6E),
    background = Color(0xFF0B132B),
    surface = Color(0xFF1C2541),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

val DarkGoldColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF78350F),
    primaryContainer = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF78350F),
    background = Color(0xFF1C1917),
    surface = Color(0xFF292524),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

val DarkSlateColorScheme = darkColorScheme(
    primary = Color(0xFF94A3B8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF090D16),
    surface = Color(0xFF151D2A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun PresupuestoTheme(
    themeIndex: Int = 0,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) {
        when (themeIndex) {
            1 -> DarkClassicBlueColorScheme
            2 -> DarkGoldColorScheme
            3 -> DarkSlateColorScheme
            else -> DarkEmeraldColorScheme
        }
    } else {
        when (themeIndex) {
            1 -> ClassicBlueColorScheme
            2 -> GoldColorScheme
            3 -> SlateColorScheme
            else -> EmeraldColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
