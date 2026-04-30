package com.rcforb.android.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.CreamDark,
    secondary = AppColors.Secondary,
    background = AppColors.Background,
    surface = AppColors.Card,
    surfaceVariant = AppColors.Secondary,
    onPrimary = AppColors.TextDark,
    onSecondary = AppColors.Foreground,
    onBackground = AppColors.Foreground,
    onSurface = AppColors.Foreground,
    onSurfaceVariant = AppColors.MutedForeground,
    outline = AppColors.Border,
    outlineVariant = AppColors.MetalDarkBorder,
    error = AppColors.ErrorDismiss,
)

@Composable
fun RCForbTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}
