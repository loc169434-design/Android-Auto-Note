package com.example.androidautonote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.androidautonote.util.AppTheme
import com.example.androidautonote.util.ThemePreferences

// === Color Schemes for each theme ===

private fun getColorScheme(theme: AppTheme, isDark: Boolean): ColorScheme {
    return when (theme) {
        AppTheme.OCEAN_BLUE -> if (isDark) darkColorScheme(
            primary = OceanBlueDark,
            secondary = OceanBlueSecondaryDark,
            tertiary = OceanBlueTertiaryDark
        ) else lightColorScheme(
            primary = OceanBlueLight,
            secondary = OceanBlueSecondaryLight,
            tertiary = OceanBlueTertiaryLight
        )

        AppTheme.FOREST_GREEN -> if (isDark) darkColorScheme(
            primary = ForestGreenDark,
            secondary = ForestGreenSecondaryDark,
            tertiary = ForestGreenTertiaryDark
        ) else lightColorScheme(
            primary = ForestGreenLight,
            secondary = ForestGreenSecondaryLight,
            tertiary = ForestGreenTertiaryLight
        )

        AppTheme.SUNSET_ORANGE -> if (isDark) darkColorScheme(
            primary = SunsetOrangeDark,
            secondary = SunsetOrangeSecondaryDark,
            tertiary = SunsetOrangeTertiaryDark
        ) else lightColorScheme(
            primary = SunsetOrangeLight,
            secondary = SunsetOrangeSecondaryLight,
            tertiary = SunsetOrangeTertiaryLight
        )

        AppTheme.LAVENDER -> if (isDark) darkColorScheme(
            primary = LavenderDark,
            secondary = LavenderSecondaryDark,
            tertiary = LavenderTertiaryDark
        ) else lightColorScheme(
            primary = LavenderLight,
            secondary = LavenderSecondaryLight,
            tertiary = LavenderTertiaryLight
        )

        AppTheme.ROSE_PINK -> if (isDark) darkColorScheme(
            primary = RosePinkDark,
            secondary = RosePinkSecondaryDark,
            tertiary = RosePinkTertiaryDark
        ) else lightColorScheme(
            primary = RosePinkLight,
            secondary = RosePinkSecondaryLight,
            tertiary = RosePinkTertiaryLight
        )

        AppTheme.MIDNIGHT -> if (isDark) darkColorScheme(
            primary = MidnightDark,
            secondary = MidnightSecondaryDark,
            tertiary = MidnightTertiaryDark
        ) else lightColorScheme(
            primary = MidnightLight,
            secondary = MidnightSecondaryLight,
            tertiary = MidnightTertiaryLight
        )

        AppTheme.COFFEE -> if (isDark) darkColorScheme(
            primary = CoffeeDark,
            secondary = CoffeeSecondaryDark,
            tertiary = CoffeeTertiaryDark
        ) else lightColorScheme(
            primary = CoffeeLight,
            secondary = CoffeeSecondaryLight,
            tertiary = CoffeeTertiaryLight
        )
    }
}

@Composable
fun AndroidAutoNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val selectedTheme by ThemePreferences.currentTheme.collectAsState()
    val colorScheme = getColorScheme(selectedTheme, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}