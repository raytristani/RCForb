package com.rcforb.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rcforb.android.ui.theme.AppColors

@Composable
fun ButtonGridView(
    buttons: Map<String, Int>,
    order: List<String>,
    onToggle: (String, Int) -> Unit
) {
    val chunked = order.filter { it.isNotEmpty() }.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { name ->
                    val isOn = (buttons[name] ?: 0) != 0
                    MetalButton(
                        title = name,
                        isOn = isOn,
                        width = 54.dp,
                        height = 20.dp,
                        fontSize = if (name.length > 6) AppColors.sp9 else AppColors.sp10
                    ) {
                        onToggle(name, if (isOn) 0 else 1)
                    }
                }
            }
        }
    }
}
