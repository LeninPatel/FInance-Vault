package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.local.ThemeMode

private val FinanceVaultDarkScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF032218),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF041E2E),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF281101),
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = Color(0xFFF43F5E),
    onError = Color.White,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFECDD3),
    background = Color(0xFF070C16),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0E1626),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF141F35),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF253659)
)

private val FinanceVaultLightScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
    background = Color(0xFFF3F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EFF8),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    VaultThemeState.isLightTheme = !useDarkTheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) FinanceVaultDarkScheme else FinanceVaultLightScheme,
        typography = Typography,
        content = content
    )
}
