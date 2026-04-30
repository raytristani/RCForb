package com.rcforb.android.ui.peripherals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rcforb.android.models.RotatorStateData
import com.rcforb.android.protocol.CommandParser
import com.rcforb.android.services.ConnectionManagerViewModel
import com.rcforb.android.ui.components.MetalButton
import com.rcforb.android.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RotatorView(vm: ConnectionManagerViewModel) {
    val rotator by vm.rotatorStateData.collectAsState()
    rotator ?: return

    var targetBearing by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Rotator", color = AppColors.Cream, fontSize = AppColors.sp18, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Compass dial
            CompassDial(bearing = rotator!!.bearing)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    Text("Bearing: ", color = AppColors.Cream, fontSize = AppColors.sp13)
                    Text("${rotator!!.bearing}\u00B0", color = AppColors.Cream, fontSize = AppColors.sp13, fontWeight = FontWeight.Bold)
                }
                Row {
                    Text("Elevation: ", color = AppColors.Cream, fontSize = AppColors.sp13)
                    Text("${rotator!!.elevation}\u00B0", color = AppColors.Cream, fontSize = AppColors.sp13, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (rotator!!.moving) "Moving..." else "Stopped",
                    color = AppColors.LabelSubtle,
                    fontSize = AppColors.sp11
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = targetBearing,
                        onValueChange = { targetBearing = it },
                        placeholder = { Text("Deg", color = AppColors.LabelDim) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppColors.Cream,
                            unfocusedTextColor = AppColors.Cream,
                            focusedBorderColor = AppColors.BtnBorder,
                            unfocusedBorderColor = AppColors.MetalDarkBorder,
                            focusedContainerColor = AppColors.InputBgTop,
                            unfocusedContainerColor = AppColors.InputBgTop,
                            cursorColor = AppColors.Cream
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            handleGo(targetBearing, vm)
                        }),
                        modifier = Modifier.width(80.dp).height(48.dp)
                    )
                    MetalButton(title = "Go", isOn = false) {
                        handleGo(targetBearing, vm)
                    }
                }
            }
        }
    }
}

private fun handleGo(targetBearing: String, vm: ConnectionManagerViewModel) {
    val deg = targetBearing.toIntOrNull()
    if (deg != null && deg in 0..359) {
        vm.sendCommand(CommandParser.rotatorBearing(deg.toString()))
        vm.sendCommand(CommandParser.rotatorStart())
    }
}

@Composable
private fun CompassDial(bearing: Int) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(AppColors.InputBgBottom)
            .border(2.dp, AppColors.BtnBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = 42.dp.toPx()

            listOf("N" to -90.0, "E" to 0.0, "S" to 90.0, "W" to 180.0).forEach { (_, angle) ->
                val rad = Math.toRadians(angle)
                val x = cx + (r * cos(rad)).toFloat()
                val y = cy + (r * sin(rad)).toFloat()
                // Direction labels drawn separately
            }

            // Needle
            rotate(bearing.toFloat(), Offset(cx, cy)) {
                drawLine(
                    color = AppColors.LedRed,
                    start = Offset(cx, cy),
                    end = Offset(cx, cy - 45.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Center dot
            drawCircle(AppColors.Foreground, radius = 4.dp.toPx(), center = Offset(cx, cy))
        }

        // Direction labels
        listOf("N" to -90, "E" to 0, "S" to 90, "W" to 180).forEach { (dir, angleDeg) ->
            val rad = Math.toRadians(angleDeg.toDouble())
            val offsetX = (42 * cos(rad)).dp
            val offsetY = (42 * sin(rad)).dp
            Text(
                dir,
                color = AppColors.LabelSubtle,
                fontSize = AppColors.sp10,
                modifier = Modifier.offset(offsetX, offsetY)
            )
        }
    }
}
