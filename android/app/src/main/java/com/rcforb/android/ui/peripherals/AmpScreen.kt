package com.rcforb.android.ui.peripherals

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcforb.android.protocol.CommandParser
import com.rcforb.android.services.ConnectionManagerViewModel
import com.rcforb.android.ui.components.ButtonGridView
import com.rcforb.android.ui.theme.AppColors

@Composable
fun AmpView(vm: ConnectionManagerViewModel) {
    val amp by vm.ampStateData.collectAsState()
    val ampData = amp ?: return
    if (ampData.buttonOrder.isEmpty()) return

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Amplifier", color = AppColors.Cream, fontSize = AppColors.sp18, fontWeight = FontWeight.Bold)
        ButtonGridView(buttons = ampData.buttons, order = ampData.buttonOrder) { name, value ->
            vm.sendCommand(CommandParser.ampButton(name, value.toString()))
        }
    }
}
