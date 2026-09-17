package com.winlator.star.contentdialog

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsDriverSettingsDialog(
    graphicsDriver: String,
    initialConfig: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val parsedConfig = remember { 
        GraphicsDriverConfigDialog.parseGraphicsDriverConfig(initialConfig) 
    }

    var vulkanVersion by remember { mutableStateOf(parsedConfig["vulkanVersion"] ?: "1.3") }
    var graphicsDriverVersion by remember { mutableStateOf(parsedConfig["graphicsDriverVersion"] ?: "System") }
    var showIncompatibleDrivers by remember { mutableStateOf(parsedConfig["showIncompatibleDrivers"]?.toBoolean() ?: false) }

    var showExtensionsDialog by remember { mutableStateOf(false) }
    
    val rawExtensions = parsedConfig["supportedExtensions"] ?: "VK_KHR_copy_commands2,VK_KHR_dedicated_allocation,VK_KHR_deferred_host_operations,VK_KHR_depth_stencil_resolve,VK_KHR_descriptor_update_template,VK_KHR_device_group,VK_KHR_draw_indirect_count,VK_KHR_driver_properties,VK_KHR_dynamic_rendering,VK_EXT_extended_dynamic_state,VK_EXT_extended_dynamic_state2,VK_KHR_external_fence,VK_KHR_external_fence_fd,VK_KHR_external_memory"
    
    val extensionsList = remember { 
        rawExtensions.split(",").map { it.trim() }.filter { it.isNotEmpty() } 
    }
    
    val extensionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            val enabledConfig = parsedConfig["enabledExtensions"] ?: ""
            val enabledSet = enabledConfig.split(",").map { it.trim() }.toSet()
            extensionsList.forEach { ext ->
                this[ext] = if (enabledConfig.isEmpty()) true else enabledSet.contains(ext)
            }
        }
    }
    
    val enabledCount = extensionStates.values.count { it }
    val totalCount = extensionsList.size

    val gpuCardsList = remember { loadGpuCardsFromAssets(context) }
    var gpuName by remember { mutableStateOf(parsedConfig["gpuName"] ?: "Device") }
    var expandedGpuDropdown by remember { mutableStateOf(false) }

    var maxDeviceMemory by remember { mutableStateOf(parsedConfig["maxDeviceMemory"] ?: "0 (Default)") }
    
    var presentModes by remember { mutableStateOf(parsedConfig["presentModes"] ?: "mailbox") }
    var expandedPresentModes by remember { mutableStateOf(false) }
    val presentModesList = listOf("mailbox", "fifo", "immediate", "relaxed")

    var memoryResourceType by remember { mutableStateOf(parsedConfig["memoryResourceType"] ?: "auto") }
    var expandedMemoryResource by remember { mutableStateOf(false) }
    val memoryResourceList = listOf("auto", "buffer", "image", "linear")

    var bcnEmulation by remember { mutableStateOf(parsedConfig["bcnEmulation"] ?: "auto") }
    var expandedBcnEmulation by remember { mutableStateOf(false) }
    val bcnEmulationList = listOf("auto", "on", "off")

    var bcnEmulationType by remember { mutableStateOf(parsedConfig["bcnEmulationType"] ?: "compute") }
    var expandedBcnType by remember { mutableStateOf(false) }
    val bcnEmulationTypeList = listOf("compute", "software")

    var bcnEmulationCache by remember { mutableStateOf(parsedConfig["bcnEmulationCache"] ?: "0") }
    
    var syncEveryFrame by remember { mutableStateOf(parsedConfig["syncEveryFrame"]?.toBoolean() ?: false) }
    var disableKhrPresentWait by remember { mutableStateOf(parsedConfig["disableKhrPresentWait"]?.toBoolean() ?: true) }
    var oneUiHyperOsFix by remember { mutableStateOf(parsedConfig["oneUiHyperOsFix"]?.toBoolean() ?: false) }

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
                OutlinedTextField(
                    value = vulkanVersion,
                    onValueChange = { vulkanVersion = it },
                    label = { Text("Vulkan Version") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = graphicsDriverVersion,
                    onValueChange = { graphicsDriverVersion = it },
                    label = { Text("Graphics Driver Version") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showIncompatibleDrivers = !showIncompatibleDrivers }
                ) {
                    Checkbox(checked = showIncompatibleDrivers, onCheckedChange = { showIncompatibleDrivers = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Show incompatible drivers")
                }

                OutlinedButton(
                    onClick = { showExtensionsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Available Extensions ($enabledCount/$totalCount)")
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedGpuDropdown,
                    onExpandedChange = { expandedGpuDropdown = !expandedGpuDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = gpuName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("GPU Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGpuDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGpuDropdown,
                        onDismissRequest = { expandedGpuDropdown = false }
                    ) {
                        gpuCardsList.forEach { card ->
                            DropdownMenuItem(
                                text = { Text(card) },
                                onClick = {
                                    gpuName = card
                                    expandedGpuDropdown = false
                                }
                            )
                        }
                    }
                }
                                OutlinedTextField(
                    value = maxDeviceMemory,
                    onValueChange = { maxDeviceMemory = it },
                    label = { Text("Max Device Memory") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedPresentModes,
                    onExpandedChange = { expandedPresentModes = !expandedPresentModes },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = presentModes,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Present Modes") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPresentModes) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPresentModes,
                        onDismissRequest = { expandedPresentModes = false }
                    ) {
                        presentModesList.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    presentModes = mode
                                    expandedPresentModes = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedMemoryResource,
                    onExpandedChange = { expandedMemoryResource = !expandedMemoryResource },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = memoryResourceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Memory Resource Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemoryResource) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMemoryResource,
                        onDismissRequest = { expandedMemoryResource = false }
                    ) {
                        memoryResourceList.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    memoryResourceType = type
                                    expandedMemoryResource = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedBcnEmulation,
                    onExpandedChange = { expandedBcnEmulation = !expandedBcnEmulation },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = bcnEmulation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("BCn Emulation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBcnEmulation) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBcnEmulation,
                        onDismissRequest = { expandedBcnEmulation = false }
                    ) {
                        bcnEmulationList.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    bcnEmulation = mode
                                    expandedBcnEmulation = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedBcnType,
                    onExpandedChange = { expandedBcnType = !expandedBcnType },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = bcnEmulationType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("BCn Emulation Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBcnType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBcnType,
                        onDismissRequest = { expandedBcnType = false }
                    ) {
                        bcnEmulationTypeList.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    bcnEmulationType = type
                                    expandedBcnType = false
                                }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Toggles & Fixes", style = MaterialTheme.typography.titleSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { syncEveryFrame = !syncEveryFrame }
                ) {
                    Checkbox(checked = syncEveryFrame, onCheckedChange = { syncEveryFrame = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Every Frame")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { disableKhrPresentWait = !disableKhrPresentWait }
                ) {
                    Checkbox(checked = disableKhrPresentWait, onCheckedChange = { disableKhrPresentWait = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable KHR_present_wait")
                }

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { etc1 = !etc1 }
                ) {
                    Checkbox(checked = etc1, onCheckedChange = { etc1 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ETC1 (RGB)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { etc2 = !etc2 }
                ) {
                    Checkbox(checked = etc2, onCheckedChange = { etc2 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ETC2 (RGB/RGBA)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { astc = !astc }
                ) {
                    Checkbox(checked = astc, onCheckedChange = { astc = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ASTC Support")
                }

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
                            5 -> "12x12 (Max Compression)"
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
                parsedConfig["vulkanVersion"] = vulkanVersion
                parsedConfig["graphicsDriverVersion"] = graphicsDriverVersion
                parsedConfig["showIncompatibleDrivers"] = showIncompatibleDrivers.toString()
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

                parsedConfig["enabledExtensions"] = extensionStates.filter { it.value }.keys.joinToString(",")

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

    if (showExtensionsDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionsDialog = false },
            title = { Text("Available Extensions") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(extensionsList) { ext ->
                        val isChecked = extensionStates[ext] ?: true
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { extensionStates[ext] = !isChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked -> extensionStates[ext] = checked }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = ext, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionsDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showExtensionsDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun loadGpuCardsFromAssets(context: Context): List<String> {
    val list = mutableListOf("Device") 
    try {
        val inputStream = context.assets.open("gpu_cards.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val name = obj.optString("name")
        
            if (name.isNotEmpty() && name != "Device") {
                list.add(name)
            }
        }
    } catch (e: Exception) {
        // Fallback if the asset file fails to read
        list.add("NVIDIA RIVA 128")
    }
    return list
}
