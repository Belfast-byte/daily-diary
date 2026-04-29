package com.example.dailydiary.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DiaryColors.Primary,
    secondary = DiaryColors.Secondary,
    tertiary = DiaryColors.Tertiary,
    background = DiaryColors.Background,
    surface = DiaryColors.Surface,
    onPrimary = DiaryColors.OnPrimary,
    onSecondary = DiaryColors.OnSecondary,
    onBackground = DiaryColors.OnBackground,
    onSurface = DiaryColors.OnSurface
)

private val DarkColors = darkColorScheme(
    primary = DiaryColors.DarkPrimary,
    secondary = DiaryColors.DarkSecondary,
    tertiary = DiaryColors.DarkTertiary,
    background = DiaryColors.DarkBackground,
    surface = DiaryColors.DarkSurface,
    onPrimary = DiaryColors.DarkOnPrimary,
    onSecondary = DiaryColors.DarkOnSecondary,
    onBackground = DiaryColors.DarkOnBackground,
    onSurface = DiaryColors.DarkOnSurface
)

@Composable
fun DailyDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = DailyDiaryTypography,
        content = content
    )
}
