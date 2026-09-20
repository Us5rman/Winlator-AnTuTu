package com.winlator.star.contentdialog

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.winlator.star.ui.screens.cyclingBlue
import com.winlator.star.ui.screens.cyclingRed
import org.json.JSONArray
import java.io.File

/**
 * Checks if the underlying device uses an Adreno GPU by inspecting device properties.
 */
private fun isAdrenoGpu(): Boolean {
    val hardware = Build.HARDWARE.lowercase()
    val board = Build.BOARD.lowercase()
    val fingerprint = Build.FINGERPRINT.lowercase()
    return hardware.contains("qcom") || hardware.contains("adreno") ||
            board.contains("qcom") || fingerprint.contains("qcom")
}

/**
 * Dynamically loads installed custom drivers from app storage (e.g., PanVK or installed .so/zip files).
 */
private fun getInstalledCustomDrivers(context: Context): List<String> {
    val installedList = mutableListOf<String>()
    
    // Check internal app files directory where custom drivers/adrenotools are unpacked
    val driversDir = File(context.filesDir, "imagefs/usr/lib")
    if (driversDir.exists()) {
        driversDir.listFiles()?.forEach { file ->
            if (file.name.contains("vulkan", ignoreCase = true) || file.name.contains("panvk", ignoreCase = true)) {
                installedList.add(file.nameWithoutExtension)
            }
        }
    }

    // Check shared custom drivers folder
    val customDriversDir = File(context.filesDir, "custom_drivers")
    if (customDriversDir.exists()) {
        customDriversDir.listFiles()?.forEach { file ->
            installedList.add(file.name)
        }
    }

    return installedList.distinct()
}
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
    var expandedVulkanDropdown by remember { mutableStateOf(false) }
    val vulkanVersionList = listOf("1.0", "1.1", "1.2", "1.3", "1.4")

    var showIncompatibleDrivers by remember { mutableStateOf(parsedConfig["showIncompatibleDrivers"]?.toBoolean() ?: false) }

    // Dynamic driver list generation
    val isAdreno = remember { isAdrenoGpu() }
    val installedCustomDrivers = remember { getInstalledCustomDrivers(context) }

    val driverVersionList = remember(showIncompatibleDrivers, isAdreno, installedCustomDrivers) {
        val list = mutableListOf("System")

        // Add v819 and turnip-sdk36 for Adreno devices OR when "Show incompatible drivers" is checked
        if (isAdreno || showIncompatibleDrivers) {
            list.add("v819")
            list.add("turnip-sdk36")
        }

        // Dynamically add all installed custom driver files found on the system
        list.addAll(installedCustomDrivers)
        list.distinct()
    }

    var graphicsDriverVersion by remember { 
        mutableStateOf(
            parsedConfig["graphicsDriverVersion"]?.takeIf { driverVersionList.contains(it) } ?: "System"
        ) 
    }
    var expandedDriverVersionDropdown by remember { mutableStateOf(false) }

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
    // ASTC block size is temporarily locked to 4x4 ("Coming soon" for the other sizes).

    // ETC1 / ETC2 / ASTC map to mutually exclusive BCN_TRANSCODE_TO_* env vars — only one
    // can be active at a time, so picking one clears the other two.
    fun selectBcnTranscodeTarget(target: String) {
        etc1 = target == "etc1"
        etc2 = target == "etc2"
        astc = target == "astc"
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
                ExposedDropdownMenuBox(
                    expanded = expandedVulkanDropdown,
                    onExpandedChange = { expandedVulkanDropdown = !expandedVulkanDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vulkanVersion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vulkan Version") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVulkanDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVulkanDropdown,
                        onDismissRequest = { expandedVulkanDropdown = false }
                    ) {
                        vulkanVersionList.forEach { version ->
                            DropdownMenuItem(
                                text = { Text(version) },
                                onClick = {
                                    vulkanVersion = version
                                    expandedVulkanDropdown = false
                                }
                            )
                        }
                    }
                }

                // Graphics Driver Version Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDriverVersionDropdown,
                    onExpandedChange = { expandedDriverVersionDropdown = !expandedDriverVersionDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = graphicsDriverVersion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Graphics Driver Version") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDriverVersionDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDriverVersionDropdown,
                        onDismissRequest = { expandedDriverVersionDropdown = false }
                    ) {
                        driverVersionList.forEach { driver ->
                            DropdownMenuItem(
                                text = { Text(driver) },
                                onClick = {
                                    graphicsDriverVersion = driver
                                    expandedDriverVersionDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showIncompatibleDrivers = !showIncompatibleDrivers }
                ) {
                    Checkbox(
                        checked = showIncompatibleDrivers,
                        onCheckedChange = { showIncompatibleDrivers = it },
                        colors = CheckboxDefaults.colors(checkedColor = cyclingBlue())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Show incompatible drivers")
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
                    Checkbox(
                        checked = syncEveryFrame,
                        onCheckedChange = { syncEveryFrame = it },
                        colors = CheckboxDefaults.colors(checkedColor = cyclingBlue())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Every Frame")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { disableKhrPresentWait = !disableKhrPresentWait }
                ) {
                    Checkbox(
                        checked = disableKhrPresentWait,
                        onCheckedChange = { disableKhrPresentWait = it },
                        colors = CheckboxDefaults.colors(checkedColor = cyclingBlue())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable KHR_present_wait")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { oneUiHyperOsFix = !oneUiHyperOsFix }
                ) {
                    Checkbox(
                        checked = oneUiHyperOsFix,
                        onCheckedChange = { oneUiHyperOsFix = it },
                        colors = CheckboxDefaults.colors(checkedColor = cyclingBlue())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OneUI / HyperOS Fix")
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Texture Compression Settings", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Only one BCn transcode target can be active at a time.",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectBcnTranscodeTarget("etc1") }
                ) {
                    RadioButton(
                        selected = etc1,
                        onClick = { selectBcnTranscodeTarget("etc1") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = cyclingBlue(),
                            unselectedColor = cyclingBlue().copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ETC1 (RGB)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectBcnTranscodeTarget("etc2") }
                ) {
                    RadioButton(
                        selected = etc2,
                        onClick = { selectBcnTranscodeTarget("etc2") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = cyclingBlue(),
                            unselectedColor = cyclingBlue().copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ETC2 (RGB/RGBA)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectBcnTranscodeTarget("astc") }
                ) {
                    RadioButton(
                        selected = astc,
                        onClick = { selectBcnTranscodeTarget("astc") },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = cyclingRed(),
                            unselectedColor = cyclingRed().copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ASTC Support")
                }

                if (astc) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ASTC Block Size: 4x4 (Max Quality)", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = cyclingRed().copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "Coming soon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cyclingRed(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        // Locked at 4x4 for now — other block sizes are temporarily disabled.
                        Slider(
                            value = 1f,
                            onValueChange = {},
                            valueRange = 1f..5f,
                            steps = 3,
                            enabled = false,
                            colors = SliderDefaults.colors(
                                disabledThumbColor = cyclingBlue(),
                                disabledActiveTrackColor = cyclingBlue(),
                                disabledInactiveTrackColor = cyclingBlue().copy(alpha = 0.3f)
                            )
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
                // Locked to 4x4 while other block sizes are "Coming soon" — see the slider above.
                parsedConfig["astcBlockSize"] = "4x4"

                parsedConfig["enabledExtensions"] = extensionStates.filter { it.value }.keys.joinToString(",")

                val finalConfigString = GraphicsDriverConfigDialog.toGraphicsDriverConfig(parsedConfig)
                onConfirm(finalConfigString)
            }) {
                Text("OK", color = cyclingBlue())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cyclingRed())
            }
        }
    )
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
        list.add("NVIDIA RIVA 128")
    }
    return list
}
