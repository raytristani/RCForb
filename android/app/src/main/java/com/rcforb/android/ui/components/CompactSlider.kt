package com.rcforb.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rcforb.android.ui.theme.AppColors

@Composable
fun CompactSlider(
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AppColors.Cream,
    inactiveColor: Color = AppColors.MetalDarkBorder,
    thumbColor: Color = AppColors.Cream
) {
    val range = (max - min).coerceAtLeast(1f)
    var fraction by remember(value, min, max) {
        mutableFloatStateOf(((value - min) / range).coerceIn(0f, 1f))
    }

    Canvas(
        modifier = modifier
            .height(16.dp)
            .fillMaxWidth()
            .pointerInput(min, max) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(min + fraction * range)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(min + fraction * range)
                    }
                )
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val thumbRadius = 6.dp.toPx()
        val trackY = (size.height - trackHeight) / 2
        val fillWidth = size.width * fraction

        // Inactive track
        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(0f, trackY),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )

        // Active track
        if (fillWidth > 0) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, trackY),
                size = Size(fillWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )
        }

        // Thumb
        val thumbX = fillWidth.coerceIn(thumbRadius, size.width - thumbRadius)
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(thumbX, size.height / 2)
        )
    }
}
