package com.rcforb.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.rcforb.android.ui.theme.AppColors
import com.rcforb.android.ui.theme.noRippleClickable

enum class MetalButtonStyle { LIGHT, DARK }

@Composable
fun MetalButton(
    title: String,
    isOn: Boolean,
    style: MetalButtonStyle = MetalButtonStyle.DARK,
    width: Dp? = null,
    height: Dp = 22.dp,
    fontSize: TextUnit = AppColors.sp12,
    onClick: () -> Unit
) {
    val isLight = isOn || style == MetalButtonStyle.LIGHT
    val bgColor = if (isLight) AppColors.MetalLightBottom else AppColors.MetalDarkTop
    val fgColor = if (isLight) AppColors.TextDark else AppColors.Cream
    val borderColor = if (isLight) AppColors.Cream else AppColors.MetalDarkBorder
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .noRippleClickable(onClick)
            .then(if (width != null) Modifier else Modifier.padding(horizontal = 8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = fgColor,
            fontSize = fontSize,
            fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = fontSize,
            textAlign = TextAlign.Center
        )
    }
}
