package com.rcforb.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rcforb.android.ui.theme.AppColors
import com.rcforb.android.ui.theme.noRippleClickable

@Composable
fun MetalDropdown(
    value: String,
    options: List<String>,
    onChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(shape)
                .background(AppColors.ChassisGradientTo)
                .border(1.dp, AppColors.BtnBorder, shape)
                .noRippleClickable { expanded = true }
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value.ifEmpty { "---" },
                color = AppColors.Cream,
                fontSize = AppColors.sp12,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.PanelBgTop)
        ) {
            val items = options.ifEmpty { listOf(value.ifEmpty { "---" }) }
            items.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt,
                            color = if (opt == value) AppColors.Cream else AppColors.CreamDark,
                            fontSize = AppColors.sp12
                        )
                    },
                    onClick = {
                        expanded = false
                        onChange(opt)
                    }
                )
            }
        }
    }
}
