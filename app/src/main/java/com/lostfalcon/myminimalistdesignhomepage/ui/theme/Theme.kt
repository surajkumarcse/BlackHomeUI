package com.lostfalcon.myminimalistdesignhomepage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
)

@Composable
fun MyMinimalistDesignHomePageTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MinimalColorScheme,
        typography = Typography,
        content = content,
    )
}
