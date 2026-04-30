package com.rcforb.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nova Olive dark theme — derived from shadcn/ui "radix-nova" preset with olive base.
 * OKLCH values converted to sRGB hex.
 */
object AppColors {
    // Core backgrounds (dark olive)
    val Background = Color(0xFF252520)       // oklch(0.153 0.006 107.1) — app background
    val Card = Color(0xFF373730)             // oklch(0.228 0.013 107.4) — card/panel/popover bg
    val Secondary = Color(0xFF45453A)        // oklch(0.286 0.016 107.4) — muted/accent/secondary bg
    val SurfaceDark = Background             // alias
    val DarkPanel = Color(0xFF2D2D28)        // between bg and card
    val ChatBg = Card                        // sidebar backgrounds

    // Core foregrounds
    val Foreground = Color(0xFFFCFCFA)       // oklch(0.988 0.003 106.5) — primary text
    val MutedForeground = Color(0xFFB3B1A0)  // oklch(0.737 0.021 106.9) — secondary/dimmed text
    val Cream = Foreground                   // alias for backward compat
    val CreamDark = Color(0xFFECEADE)        // oklch(0.93 0.007 106.5) — primary button bg
    val TextDark = Color(0xFF373730)         // dark text on light bg

    // Borders & inputs
    val Border = Color(0xFF3D3D36)           // white 10% on dark bg
    val InputBg = Color(0xFF3D3D36)          // white ~12% on dark bg
    val PanelBorder = Border
    val MetalDarkBorder = Color(0xFF4A4A40)
    val BtnBorder = Color(0xFF4A4A40)

    // Buttons
    val MetalLightBottom = CreamDark         // primary button bg (light)
    val MetalLightTop = CreamDark            // kept for compat
    val MetalDarkTop = Secondary             // secondary/dark button bg
    val MetalDarkBottom = Secondary           // kept for compat

    // Headers / toolbar
    val ChassisGradientFrom = Secondary      // flat header bg
    val ChassisGradientTo = Secondary        // flat header bg

    // Panels
    val PanelBgTop = Card                    // panel bg
    val PanelBgBottom = Card                 // panel bg

    // Inputs
    val InputBgTop = InputBg
    val InputBgBottom = InputBg

    // Labels (muted foreground variants)
    val LabelDim = MutedForeground.copy(alpha = 0.6f)
    val LabelMuted = MutedForeground
    val LabelSubtle = MutedForeground.copy(alpha = 0.8f)

    // Functional colors — preserved
    val LcdText = Color(0xFF3A0500)
    val LcdGlow = Color(0xFFAA6633)
    val LedGreen = Color(0xFF44CC44)
    val LedRed = Color(0xFFCC4444)

    // Status
    val StatusActive = CreamDark             // active pills use primary color

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

fun hexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    return Color(android.graphics.Color.parseColor("#$clean"))
}
