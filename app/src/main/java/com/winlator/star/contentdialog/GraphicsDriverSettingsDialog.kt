package com.winlator.star.contentdialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GraphicsDriverSettingsDialog(
    graphicsDriver: String,
    initialConfig: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse using your Java helper class
    val parsedConfig = remember { 
        GraphicsDriverConfigDialog.parseGraphicsDriverConfig(initialConfig) 
    }

    var etc1 by remember { mutableStateOf(parsedConfig["etc1"]?.toBoolean() ?: false) }
    var etc2 by remember { mutableStateOf(parsedConfig["etc2"]?.toBoolean() ?: false) }
    var astc by remember { mutableStateOf(parsedConfig["astc"]?.toBoolean() ?: false) }
    var astcBlockSize by remember { mutableStateOf(parsedConfig["astcBlockSize"] ?: "6x6") }

    var astcStep by remember {
        mutableStateOf(
            when (astcBlockSize) {
                "4x4" -> 1f
                "5x5" -> 2f
                "6x6" -> 3f
                "8x8" -> 4f
                "12x12" -> 5f
                else -> 3f
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Texture Compression Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ETC1 Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { etc1 = !etc1 }
                ) {
                    Checkbox(checked = etc1, onCheckedChange = { etc1 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ETC1 (RGB)")
                }

                // ETC2 Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { etc2 = !etc2 }
                ) {
                    Checkbox(checked = etc2, onCheckedChange = { etc2 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ETC2 (RGB/RGBA)")
                }

                // ASTC Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { astc = !astc }
                ) {
                    Checkbox(checked = astc, onCheckedChange = { astc = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ASTC Support")
                }

                // Conditional ASTC Slider
                if (astc) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 8.dp)
                    ) {
                        val currentLabel = when (astcStep.toInt()) {
                            1 -> "4x4 (Max Quality)"
                            2 -> "5x5"
                            3 -> "6x6 (Balanced)"
                            4 -> "8x8 (High Compression)"
                            5 -> "12x12 (Max Performance)"
                            else -> "6x6"
                        }
                        Text("ASTC Block Size: $currentLabel", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = astcStep,
                            onValueChange = { astcStep = it },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                parsedConfig["etc1"] = etc1.toString()
                parsedConfig["etc2"] = etc2.toString()
                parsedConfig["astc"] = astc.toString()
                parsedConfig["astcBlockSize"] = when (astcStep.toInt()) {
                    1 -> "4x4"
                    2 -> "5x5"
                    3 -> "6x6"
                    4 -> "8x8"
                    5 -> "12x12"
                    else -> "6x6"
                }

                val finalConfigString = GraphicsDriverConfigDialog.toGraphicsDriverConfig(parsedConfig)
                onConfirm(finalConfigString)
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
