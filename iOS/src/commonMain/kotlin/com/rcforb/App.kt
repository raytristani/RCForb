package com.rcforb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimum-viable iOS root. The full RCForb UI (Login → Lobby → Radio) ports
 * over from android/ + macOS/ once the platform actuals (NSUserDefaults
 * persistence, AVFAudio bridge, libspeex cinterop) are wired in iosMain.
 *
 * For now this just proves the Compose Multiplatform iOS toolchain works
 * end-to-end: framework links, Xcode embeds it, app launches on device.
 */
@Composable
fun App() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F2417)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "RCForb",
                color = Color(0xFFD9D5B5),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "iOS port — scaffold",
                color = Color(0xFF8A8770),
                fontSize = 16.sp
            )
            Text(
                text = "v1.0.0",
                color = Color(0xFF555342),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
