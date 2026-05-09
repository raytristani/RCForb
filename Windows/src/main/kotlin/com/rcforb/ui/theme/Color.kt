package com.rcforb.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nova Olive dark theme — derived from shadcn/ui "radix-nova" preset with olive base.
 * Hex values are taken verbatim from the Android canonical AppColors.
 */
object AppColors {
    // Core backgrounds (dark olive)
    val Background = Color(0xFF252520)
    val Card = Color(0xFF373730)
    val Secondary = Color(0xFF45453A)
    val SurfaceDark = Background
    val DarkPanel = Color(0xFF2D2D28)
    val ChatBg = Card

    // Core foregrounds
    val Foreground = Color(0xFFFCFCFA)
    val MutedForeground = Color(0xFFB3B1A0)
    val Cream = Foreground
    val CreamDark = Color(0xFFECEADE)
    val TextDark = Color(0xFF373730)

    // Borders & inputs
    val Border = Color(0xFF3D3D36)
    val InputBg = Color(0xFF3D3D36)
    val PanelBorder = Border
    val MetalDarkBorder = Color(0xFF4A4A40)
    val BtnBorder = Color(0xFF4A4A40)

    // Buttons
    val MetalLightBottom = CreamDark
    val MetalLightTop = CreamDark
    val MetalDarkTop = Secondary
    val MetalDarkBottom = Secondary

    // Headers / toolbar
    val ChassisGradientFrom = Secondary
    val ChassisGradientTo = Secondary

    // Panels
    val PanelBgTop = Card
    val PanelBgBottom = Card

    // Inputs
    val InputBgTop = InputBg
    val InputBgBottom = InputBg

    // Labels
    val LabelDim = MutedForeground.copy(alpha = 0.6f)
    val LabelMuted = MutedForeground
    val LabelSubtle = MutedForeground.copy(alpha = 0.8f)

    // Functional colors
    val LcdText = Color(0xFF3A0500)
    val LcdGlow = Color(0xFFAA6633)
    val LedGreen = Color(0xFF44CC44)
    val LedRed = Color(0xFFCC4444)

    // Status
    val StatusActive = CreamDark

    // Error / destructive
    val ErrorBg = Color(0xCC6B2020.toInt())
    val ErrorText = Color(0xFFFCA5A5)
    val ErrorDismiss = Color(0xFFF87171)

    // Dp constants
    val dp2 = 2.dp
    val dp4 = 4.dp
    val dp6 = 6.dp
    val dp8 = 8.dp
    val dp12 = 12.dp
    val dp16 = 16.dp
    val dp24 = 24.dp
    val dp32 = 32.dp

    // Sp constants
    val sp9 = 9.sp
    val sp10 = 10.sp
    val sp11 = 11.sp
    val sp12 = 12.sp
    val sp13 = 13.sp
    val sp18 = 18.sp
    val sp24 = 24.sp
    val sp38 = 38.sp
}
