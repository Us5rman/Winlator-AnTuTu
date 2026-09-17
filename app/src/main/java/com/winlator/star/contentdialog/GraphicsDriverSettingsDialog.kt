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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsDriverSettingsDialog(
    graphicsDriver: String,
    initialConfig: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parsedConfig = remember { 
        GraphicsDriverConfigDialog.parseGraphicsDriverConfig(initialConfig) 
    }

    // Wrapper & Turnip Config States
    var gpuName by remember { mutableStateOf(parsedConfig["gpuName"] ?: "Device") }
    var maxDeviceMemory by remember { mutableStateOf(parsedConfig["maxDeviceMemory"] ?: "0 (Default)") }
    var presentModes by remember { mutableStateOf(parsedConfig["presentModes"] ?: "mailbox") }
    var memoryResourceType by remember { mutableStateOf(parsedConfig["memoryResourceType"] ?: "auto") }
    var bcnEmulation by remember { mutableStateOf(parsedConfig["bcnEmulation"] ?: "auto") }
    var bcnEmulationType by remember { mutableStateOf(parsedConfig["bcnEmulationType"] ?: "compute") }
    var bcnEmulationCache by remember { mutableStateOf(parsedConfig["bcnEmulationCache"] ?: "0") }
    
    var syncEveryFrame by remember { mutableStateOf(parsedConfig["syncEveryFrame"]?.toBoolean() ?: false) }
    var disableKhrPresentWait by remember { mutableStateOf(parsedConfig["disableKhrPresentWait"]?.toBoolean() ?: true) }
    var oneUiHyperOsFix by remember { mutableStateOf(parsedConfig["oneUiHyperOsFix"]?.toBoolean() ?: false) }

    // Texture Compression States
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
        title = { Text("Turnip/Wrapper Driver Configuration") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // GPU Name field
                OutlinedTextField(
                    value = gpuName,
                    onValueChange = { gpuName = it },
                    label = { Text("GPU Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Present Modes field
                OutlinedTextField(
                    value = presentModes,
                    onValueChange = { presentModes = it },
                    label = { Text("Present Modes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Memory Resource Type field
                OutlinedTextField(
                    value = memoryResourceType,
                    onValueChange = { memoryResourceType = it },
                    label = { Text("Memory Resource Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                // BCn Emulation field
                OutlinedTextField(
                    value = bcnEmulation,
                    onValueChange = { bcnEmulation = it },
                    label = { Text("BCn Emulation") },
                    modifier = Modifier.fillMaxWidth()
                )

                // BCn Emulation Type field
                OutlinedTextField(
                    value = bcnEmulationType,
                    onValueChange = { bcnEmulationType = it },
                    label = { Text("BCn Emulation Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Toggles & Fixes", style = MaterialTheme.typography.titleSmall)

                // Sync Every Frame
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { syncEveryFrame = !syncEveryFrame }
                ) {
                    Checkbox(checked = syncEveryFrame, onCheckedChange = { syncEveryFrame = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Every Frame")
                }

                // Disable KHR_present_wait
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { disableKhrPresentWait = !disableKhrPresentWait }
                ) {
                    Checkbox(checked = disableKhrPresentWait, onCheckedChange = { disableKhrPresentWait = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable KHR_present_wait")
                }

                // OneUI / HyperOS Fix
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { oneUiHyperOsFix = !oneUiHyperOsFix }
                ) {
                    Checkbox(checked = oneUiHyperOsFix, onCheckedChange = { oneUiHyperOsFix = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OneUI / HyperOS Fix")
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Texture Compression Settings", style = MaterialTheme.typography.titleSmall)

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
                            2 -> "5x5 (High Quality)"
                            3 -> "6x6 (Balanced)"
                            4 -> "8x8 (High Compression)"
                            5 -> "12x12 (Max compression)"
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
                parsedConfig["gpuName"] = gpuName
                parsedConfig["maxDeviceMemory"] = maxDeviceMemory
                parsedConfig["presentModes"] = presentModes
                parsedConfig["memoryResourceType"] = memoryResourceType
                parsedConfig["bcnEmulation"] = bcnEmulation
                parsedConfig["bcnEmulationType"] = bcnEmulationType
                parsedConfig["bcnEmulationCache"] = bcnEmulationCache
                parsedConfig["syncEveryFrame"] = syncEveryFrame.toString()
                parsedConfig["disableKhrPresentWait"] = disableKhrPresentWait.toString()
                parsedConfig["oneUiHyperOsFix"] = oneUiHyperOsFix.toString()
                
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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
