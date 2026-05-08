package com.rcforb.ui.radio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rcforb.ui.theme.AppColors
import com.rcforb.ui.theme.Digital7MonoFamily
import com.rcforb.ui.theme.noRippleClickable

@Composable
fun FrequencyDisplay(
    frequency: Int,
    vfo: String,
    large: Boolean,
    onSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var freqInput by remember { mutableStateOf("") }

    val formatted = run {
        val s = "%09d".format(maxOf(0, frequency))
        "${s.substring(0, 3)}.${s.substring(3, 6)}.${s.substring(6, 9)}"
    }

    Text(
        text = formatted,
        fontFamily = Digital7MonoFamily,
        fontSize = if (large) AppColors.sp38 else AppColors.sp24,
        color = Color(0xFF553300),
        maxLines = 1,
        modifier = modifier.noRippleClickable {
            freqInput = "%.6f".format(frequency / 1_000_000.0)
            showDialog = true
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter frequency (MHz) for VFO $vfo:", color = AppColors.CreamDark) },
            text = {
                OutlinedTextField(
                    value = freqInput,
                    onValueChange = { freqInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColors.Cream,
                        unfocusedTextColor = AppColors.Cream,
                        focusedBorderColor = AppColors.BtnBorder,
                        unfocusedBorderColor = AppColors.MetalDarkBorder,
                        focusedContainerColor = AppColors.Card,
                        unfocusedContainerColor = AppColors.Card,
                        cursorColor = AppColors.Cream
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        val mhz = freqInput.toDoubleOrNull()
                        if (mhz != null && mhz > 0) {
                            onSet((mhz * 1_000_000).toInt())
                        }
                        showDialog = false
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val mhz = freqInput.toDoubleOrNull()
                    if (mhz != null && mhz > 0) {
                        onSet((mhz * 1_000_000).toInt())
                    }
                    showDialog = false
                }) {
                    Text("Set", color = AppColors.Cream)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = AppColors.CreamDark)
                }
            },
            containerColor = AppColors.PanelBgTop,
            shape = RoundedCornerShape(6.dp)
        )
    }
}
