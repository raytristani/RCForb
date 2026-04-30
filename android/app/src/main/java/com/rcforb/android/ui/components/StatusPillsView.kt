package com.rcforb.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rcforb.android.ui.theme.AppColors

@Composable
fun StatusPillsView(
    statuses: Map<String, Boolean>,
    order: List<String>
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        order.forEach { name ->
            val isOn = statuses[name] ?: false
            val shape = RoundedCornerShape(6.dp)
            val bgColor = if (isOn) AppColors.StatusActive else AppColors.MetalDarkTop

            Text(
                text = name,
                color = if (isOn) AppColors.TextDark else AppColors.LabelDim,
                fontSize = AppColors.sp10,
                fontFamily = FontFamily.Monospace,
                lineHeight = AppColors.sp10,
                modifier = Modifier
                    .alpha(if (isOn) 0.9f else 0.6f)
                    .clip(shape)
                    .background(bgColor)
                    .border(0.5.dp, AppColors.StatusActive, shape)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
