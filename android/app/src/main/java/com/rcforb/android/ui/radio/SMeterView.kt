package com.rcforb.android.ui.radio

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rcforb.android.R
import com.rcforb.android.ui.theme.AppColors

val Digital7MonoFamily = FontFamily(Font(R.font.digital_7_mono))

@Composable
fun SMeterView(value: Double, label: String) {
    val pct = ((value / 19.0) * 100.0).coerceIn(0.0, 100.0)
    val animatedPct by animateFloatAsState(
        targetValue = pct.toFloat(),
        animationSpec = tween(300),
        label = "smeter"
    )

    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(shape)
            .background(Color(0xFFC8B868))
            .border(1.dp, Color(0xFFAA9944), shape),
        contentAlignment = Alignment.Center
    ) {
        // Bar
        val barColors = if (pct > 60) {
            listOf(Color(0xFF669933), Color(0xFFCC4422))
        } else {
            listOf(Color(0xFF669933), Color(0xFF88AA44))
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedPct / 100f)
                .background(Brush.horizontalGradient(barColors))
                .align(Alignment.CenterStart)
        )

        // Label
        Text(
            text = label,
            color = Color(0xFF553300),
            fontSize = AppColors.sp13,
            fontFamily = Digital7MonoFamily,
            lineHeight = AppColors.sp13,
            maxLines = 1
        )
    }
}
