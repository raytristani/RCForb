package com.rcforb.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcforb.android.ui.theme.AppColors

@Composable
fun PanelView(
    title: String,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.PanelBgBottom)
            .border(1.dp, AppColors.PanelBorder, shape)
            .padding(6.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = AppColors.LabelDim,
            fontSize = AppColors.sp10,
            fontWeight = FontWeight.Bold,
            letterSpacing = AppColors.sp10 * 0.1f
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}
