package com.winlator.star.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.winlator.star.R
import com.winlator.star.SettingsFragment
import com.winlator.star.XServerDisplayActivity
import com.winlator.star.XrActivity
import com.winlator.star.box64.Box64Preset
import com.winlator.star.box64.Box64PresetManager
import com.winlator.star.container.Container
import com.winlator.star.container.Shortcut
import com.winlator.star.contentdialog.GraphicsDriverConfigDialog
import com.winlator.star.contents.ContentProfile
import com.winlator.star.contents.ContentsManager
import com.winlator.star.core.FileUtils
import com.winlator.star.core.KeyValueSet
import com.winlator.star.core.StringUtils
import com.winlator.star.core.WineInfo
import com.winlator.star.fexcore.FEXCorePreset
import com.winlator.star.fexcore.FEXCorePresetManager
import com.winlator.star.inputcontrols.ControlsProfile
import com.winlator.star.inputcontrols.InputControlsManager
import com.winlator.star.midi.MidiManager
import com.winlator.star.ui.LocalTopBarActions
import com.winlator.star.ui.theme.Divider as DividerColor
import com.winlator.star.ui.theme.OnSurface
import com.winlator.star.ui.theme.OnSurfaceVariant
import com.winlator.star.ui.theme.Surface as SurfaceColor
import com.winlator.star.widget.CPUListView
import com.winlator.star.widget.EnvVarsView
import com.winlator.star.winhandler.WinHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Field

// Custom theme accents
private val PSBlue = Color(0xFF0072CE)
private val PSRed = Color(0xFFD32F2F)
private val DarkBg = Color(0xFF0D0D0D)
@Composable
fun ShortcutsScreen(vm: ShortcutsViewModel = viewModel()) {
    val shortcuts by vm.shortcuts.collectAsState(initial = emptyList())
    val sortOrder by vm.sortOrder.collectAsState()
    val isGridView by vm.isGridView.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    var confirmRemove by remember { mutableStateOf<Shortcut?>(null) }
    var cloneTarget by remember { mutableStateOf<Shortcut?>(null) }
    var settingsShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var propertiesShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showImportContainerPicker by remember { mutableStateOf(false) }
    var pendingImportContainerIndex by remember { mutableStateOf(-1) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDialogName by remember { mutableStateOf("") }
    var renameDialogContainerIndex by remember { mutableStateOf(-1) }

    // Auto Detect Games feature state
    var showAutoDetectPicker by remember { mutableStateOf(false) }
    var autoDetectContainerIndex by remember { mutableStateOf(-1) }
    var discoveredExes by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedExes by remember { mutableStateOf<Set<File>>(emptySet()) }
    var showExeSelectionDialog by remember { mutableStateOf(false) }

    val autoDetectFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val folderPath = FileUtils.getFilePathFromUri(context, uri)
        if (folderPath != null) {
            val rootDir = File(folderPath)
            val found = mutableListOf<File>()
            rootDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension.equals("exe", ignoreCase = true)) {
                    found.add(file)
                }
            }
            if (found.isNotEmpty()) {
                discoveredExes = found
                selectedExes = found.toSet()
                showExeSelectionDialog = true
            } else {
                Toast.makeText(context, "No .exe files found in selected folder.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Could not access folder.", Toast.LENGTH_SHORT).show()
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (pendingImportContainerIndex >= 0) {
            val result = vm.importShortcut(pendingImportContainerIndex, uri, context)
            when (result) {
                is ImportResult.Success -> {
                    renameDialogContainerIndex = pendingImportContainerIndex
                    renameDialogName = result.shortcutName
                    showRenameDialog = true
                }
                is ImportResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
            pendingImportContainerIndex = -1
        }
    }

    // Set up top bar action icons for sort, grid toggle, and game scanner
    val topBarActionsSetter = LocalTopBarActions.current
    DisposableEffect(isGridView) {
        topBarActionsSetter {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Sort Shortcuts",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier = Modifier.background(DarkBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Name (A-Z)", color = Color.White) },
                    onClick = {
                        vm.setSortOrder(SortOrder.NAME_ASC)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Name (Z-A)", color = Color.White) },
                    onClick = {
                        vm.setSortOrder(SortOrder.NAME_DESC)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Recently Added", color = Color.White) },
                    onClick = {
                        vm.setSortOrder(SortOrder.RECENT)
                        showSortMenu = false
                    }
                )
            }
            IconButton(onClick = { vm.toggleGridView() }) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle Layout",
                    tint = Color.White
                )
            }
        }
        onDispose {
            topBarActionsSetter {}
        }
    }
        Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (shortcuts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shortcuts added yet.\nTap + to import or detect games.",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shortcuts, key = { it.file.absolutePath }) { shortcut ->
                        PS4GridShortcutCard(
                            shortcut = shortcut,
                            onClick = { vm.launchShortcut(shortcut, activity) },
                            onLongClick = { propertiesShortcut = shortcut },
                            onSettingsClick = { settingsShortcut = shortcut },
                            onCloneClick = { cloneTarget = shortcut },
                            onRemoveClick = { confirmRemove = shortcut }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shortcuts, key = { it.file.absolutePath }) { shortcut ->
                        ListShortcutItem(
                            shortcut = shortcut,
                            onClick = { vm.launchShortcut(shortcut, activity) },
                            onLongClick = { propertiesShortcut = shortcut },
                            onSettingsClick = { settingsShortcut = shortcut },
                            onCloneClick = { cloneTarget = shortcut },
                            onRemoveClick = { confirmRemove = shortcut }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showImportContainerPicker = true },
            containerColor = PSBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Shortcut Options"
            )
        }
    }

    // Modal: Container Picker for Adding / Scanning
    if (showImportContainerPicker) {
        val containers = vm.getContainers()
        AlertDialog(
            onDismissRequest = { showImportContainerPicker = false },
            containerColor = DarkBg,
            title = { Text("Add New Shortcut", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select container & action:", color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                    if (containers.isEmpty()) {
                        Text("No containers found. Please create one first.", color = PSRed)
                    } else {
                        containers.forEachIndexed { index, container ->
                            Surface(
                                color = SurfaceColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = container.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                showImportContainerPicker = false
                                                pendingImportContainerIndex = container.id
                                                importFileLauncher.launch("*/*")
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PSBlue),
                                            border = BorderStroke(1.dp, PSBlue),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("File", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                showImportContainerPicker = false
                                                autoDetectContainerIndex = container.id
                                                autoDetectFolderLauncher.launch(null)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PSRed),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Auto Detect", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportContainerPicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
        // Auto Detect Game Executables Selection Dialog
    if (showExeSelectionDialog) {
        val container = vm.getContainerById(autoDetectContainerIndex)
        AlertDialog(
            onDismissRequest = { showExeSelectionDialog = false },
            containerColor = DarkBg,
            title = {
                Text(
                    text = "Detected Executables (${discoveredExes.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Container: ${container?.name ?: "Default"}",
                        color = PSBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selectedExes = discoveredExes.toSet() },
                            colors = ButtonDefaults.textButtonColors(contentColor = PSBlue)
                        ) {
                            Text("Select All", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { selectedExes = emptySet() },
                            colors = ButtonDefaults.textButtonColors(contentColor = PSRed)
                        ) {
                            Text("Deselect All", fontSize = 12.sp)
                        }
                    }
                    Divider(color = DividerColor)
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .fillMaxWidth()
                    ) {
                        items(discoveredExes, key = { it.absolutePath }) { file ->
                            val isChecked = selectedExes.contains(file)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExes = if (isChecked) {
                                            selectedExes - file
                                        } else {
                                            selectedExes + file
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedExes = if (checked == true) {
                                            selectedExes + file
                                        } else {
                                            selectedExes - file
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PSBlue,
                                        uncheckedColor = Color.Gray,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = file.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = file.parent ?: "",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedExes.isNotEmpty()) {
                            val count = vm.createShortcutsForFiles(autoDetectContainerIndex, selectedExes.toList(), context)
                            Toast.makeText(context, "Added $count game shortcuts successfully!", Toast.LENGTH_SHORT).show()
                        }
                        showExeSelectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PSBlue),
                    enabled = selectedExes.isNotEmpty()
                ) {
                    Text("Add Selected (${selectedExes.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExeSelectionDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Modal: Rename Shortcut Dialog post-import
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = DarkBg,
            title = { Text("Set Game Title", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameDialogName,
                    onValueChange = { renameDialogName = it },
                    label = { Text("Shortcut Name", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDialogName.isNotBlank() && renameDialogContainerIndex >= 0) {
                            vm.finalizeImportName(renameDialogContainerIndex, renameDialogName, context)
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PSBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Skip", color = Color.Gray)
                }
            }
        )
    }

    // Modal: Remove Shortcut Confirmation
    confirmRemove?.let { shortcut ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            containerColor = DarkBg,
            title = { Text("Remove Shortcut", color = Color.White) },
            text = { Text("Are you sure you want to remove '${shortcut.name}'?", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        vm.removeShortcut(shortcut, context)
                        confirmRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PSRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Modal: Clone Shortcut Dialog
    cloneTarget?.let { shortcut ->
        var cloneName by remember { mutableStateOf("${shortcut.name} (Copy)") }
        AlertDialog(
            onDismissRequest = { cloneTarget = null },
            containerColor = DarkBg,
            title = { Text("Clone Shortcut", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = cloneName,
                    onValueChange = { cloneName = it },
                    label = { Text("New Shortcut Name", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cloneName.isNotBlank()) {
                            vm.cloneShortcut(shortcut, cloneName, context)
                        }
                        cloneTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PSBlue)
                ) {
                    Text("Clone")
                }
            },
            dismissButton = {
                TextButton(onClick = { cloneTarget = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Modal: Properties Inspector
    propertiesShortcut?.let { shortcut ->
        ShortcutPropertiesDialog(
            shortcut = shortcut,
            onDismiss = { propertiesShortcut = null }
        )
    }

    // Modal: Game-Specific Configuration / Settings Sheet
    settingsShortcut?.let { shortcut ->
        ShortcutSettingsDialog(
            shortcut = shortcut,
            onDismiss = { settingsShortcut = null },
            onSave = { updatedShortcut ->
                vm.updateShortcut(updatedShortcut, context)
                settingsShortcut = null
            }
        )
    }
}
@Composable
private fun PS4GridShortcutCard(
    shortcut: Shortcut,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloneClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val iconBitmap = remember(shortcut.iconPath) {
        if (!shortcut.iconPath.isNullOrEmpty() && File(shortcut.iconPath).exists()) {
            BitmapFactory.decodeFile(shortcut.iconPath)
        } else null
    }

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.67f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = shortcut.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PSBlue.copy(alpha = 0.6f), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortcut.name.take(2).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = shortcut.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Game Settings", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = PSBlue) },
                            onClick = {
                                showMenu = false
                                onSettingsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clone", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White) },
                            onClick = {
                                showMenu = false
                                onCloneClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Home Screen", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddToHomeScreen, contentDescription = null, tint = Color.White) },
                            onClick = {
                                showMenu = false
                                createPinnedShortcut(context, shortcut)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove", color = PSRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PSRed) },
                            onClick = {
                                showMenu = false
                                onRemoveClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListShortcutItem(
    shortcut: Shortcut,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloneClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val iconBitmap = remember(shortcut.iconPath) {
        if (!shortcut.iconPath.isNullOrEmpty() && File(shortcut.iconPath).exists()) {
            BitmapFactory.decodeFile(shortcut.iconPath)
        } else null
    }

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = shortcut.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = shortcut.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PSBlue,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shortcut.path ?: "Executable",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onSettingsClick() }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = PSBlue
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkBg)
                ) {
                    DropdownMenuItem(
                        text = { Text("Clone", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onCloneClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Home Screen", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.AddToHomeScreen, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            createPinnedShortcut(context, shortcut)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove", color = PSRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PSRed) },
                        onClick = {
                            showMenu = false
                            onRemoveClick()
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun ShortcutPropertiesDialog(
    shortcut: Shortcut,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PSBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Shortcut Properties",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                PropertyItem(label = "Name", value = shortcut.name)
                PropertyItem(label = "Container ID", value = shortcut.container.id.toString())
                PropertyItem(label = "Container Name", value = shortcut.container.name)
                PropertyItem(label = "Executable Path", value = shortcut.path ?: "N/A")
                PropertyItem(label = "Exec File Path", value = shortcut.file.absolutePath)
                PropertyItem(label = "Icon Path", value = shortcut.iconPath.ifEmpty { "Default" })
                PropertyItem(label = "WM Class", value = shortcut.wmClass.ifEmpty { "None" })
                PropertyItem(
                    label = "Extra Args",
                    value = shortcut.extraArgs.ifEmpty { "None" }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PSBlue)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PropertyItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            color = PSBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Divider(
            color = DividerColor.copy(alpha = 0.3f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ShortcutSettingsDialog(
    shortcut: Shortcut,
    onDismiss: () -> Unit,
    onSave: (Shortcut) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(shortcut.name) }
    var extraArgs by remember { mutableStateOf(shortcut.extraArgs) }
    var iconPath by remember { mutableStateOf(shortcut.iconPath) }
    var wmClass by remember { mutableStateOf(shortcut.wmClass) }

    var screenResolution by remember { mutableStateOf(shortcut.getExtra("screenResolution", "1280x720")) }
    var graphicsDriver by remember { mutableStateOf(shortcut.getExtra("graphicsDriver", "Turnip")) }
    var dxvkVersion by remember { mutableStateOf(shortcut.getExtra("dxvkVersion", "2.3.1")) }
    var vkd3dVersion by remember { mutableStateOf(shortcut.getExtra("vkd3dVersion", "2.12")) }
    var box64Preset by remember { mutableStateOf(shortcut.getExtra("box64Preset", "Intermediate")) }
    var fexCorePreset by remember { mutableStateOf(shortcut.getExtra("fexCorePreset", "Intermediate")) }

    var controlsProfile by remember { mutableStateOf(shortcut.getExtra("controlsProfile", "Default")) }
    var midiSoundFont by remember { mutableStateOf(shortcut.getExtra("midiSoundFont", "Default")) }

    var cpuAffinity by remember { mutableStateOf(shortcut.getExtra("cpuAffinity", "All")) }
    var envVars by remember { mutableStateOf(shortcut.getExtra("envVars", "")) }

    val tabs = listOf("General", "Graphics", "Input & Sound", "Advanced")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DarkBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game Settings: ${shortcut.name}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = PSBlue
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    color = if (selectedTab == index) PSBlue else Color.Gray
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> GeneralSettingsTab(
                            name = name,
                            onNameChange = { name = it },
                            extraArgs = extraArgs,
                            onExtraArgsChange = { extraArgs = it },
                            wmClass = wmClass,
                            onWmClassChange = { wmClass = it },
                            iconPath = iconPath,
                            onIconPathChange = { iconPath = it }
                        )
                        1 -> GraphicsSettingsTab(
                            screenResolution = screenResolution,
                            onResolutionChange = { screenResolution = it },
                            graphicsDriver = graphicsDriver,
                            onDriverChange = { graphicsDriver = it },
                            dxvkVersion = dxvkVersion,
                            onDxvkChange = { dxvkVersion = it },
                            vkd3dVersion = vkd3dVersion,
                            onVkd3dChange = { vkd3dVersion = it },
                            box64Preset = box64Preset,
                            onBox64Change = { box64Preset = it },
                            fexCorePreset = fexCorePreset,
                            onFexCoreChange = { fexCorePreset = it }
                        )
                        2 -> ControlsAndSoundSettingsTab(
                            controlsProfile = controlsProfile,
                            onControlsProfileChange = { controlsProfile = it },
                            midiSoundFont = midiSoundFont,
                            onMidiSoundFontChange = { midiSoundFont = it }
                        )
                        3 -> AdvancedSettingsTab(
                            cpuAffinity = cpuAffinity,
                            onCpuAffinityChange = { cpuAffinity = it },
                            envVars = envVars,
                            onEnvVarsChange = { envVars = it }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            shortcut.name = name
                            shortcut.extraArgs = extraArgs
                            shortcut.iconPath = iconPath
                            shortcut.wmClass = wmClass
                            shortcut.putExtra("screenResolution", screenResolution)
                            shortcut.putExtra("graphicsDriver", graphicsDriver)
                            shortcut.putExtra("dxvkVersion", dxvkVersion)
                            shortcut.putExtra("vkd3dVersion", vkd3dVersion)
                            shortcut.putExtra("box64Preset", box64Preset)
                            shortcut.putExtra("fexCorePreset", fexCorePreset)
                            shortcut.putExtra("controlsProfile", controlsProfile)
                            shortcut.putExtra("midiSoundFont", midiSoundFont)
                            shortcut.putExtra("cpuAffinity", cpuAffinity)
                            shortcut.putExtra("envVars", envVars)
                            onSave(shortcut)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PSBlue)
                    ) {
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}
@Composable
private fun GeneralSettingsTab(
    name: String,
    onNameChange: (String) -> Unit,
    extraArgs: String,
    onExtraArgsChange: (String) -> Unit,
    wmClass: String,
    onWmClassChange: (String) -> Unit,
    iconPath: String,
    onIconPathChange: (String) -> Unit
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = FileUtils.getFilePathFromUri(context, it)
            if (path != null) {
                onIconPathChange(path)
            } else {
                Toast.makeText(context, "Failed to resolve image path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Display Name", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = extraArgs,
            onValueChange = onExtraArgsChange,
            label = { Text("Command Line Arguments", color = Color.Gray) },
            placeholder = { Text("-nogui -dx11", color = Color.DarkGray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = wmClass,
            onValueChange = onWmClassChange,
            label = { Text("Window Class (WM_CLASS)", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Custom Cover / Icon Image",
                color = PSBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    val iconBitmap = remember(iconPath) {
                        if (iconPath.isNotEmpty() && File(iconPath).exists()) {
                            BitmapFactory.decodeFile(iconPath)
                        } else null
                    }

                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = "Cover Image Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "No Icon",
                            tint = Color.Gray
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PSBlue),
                        border = BorderStroke(1.dp, PSBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Image")
                    }

                    if (iconPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { onIconPathChange("") },
                            colors = ButtonDefaults.textButtonColors(contentColor = PSRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset Cover", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphicsSettingsTab(
    screenResolution: String,
    onResolutionChange: (String) -> Unit,
    graphicsDriver: String,
    onDriverChange: (String) -> Unit,
    dxvkVersion: String,
    onDxvkChange: (String) -> Unit,
    vkd3dVersion: String,
    onVkd3dChange: (String) -> Unit,
    box64Preset: String,
    onBox64Change: (String) -> Unit,
    fexCorePreset: String,
    onFexCoreChange: (String) -> Unit
) {
    var resolutionExpanded by remember { mutableStateOf(false) }
    var driverExpanded by remember { mutableStateOf(false) }
    var dxvkExpanded by remember { mutableStateOf(false) }
    var vkd3dExpanded by remember { mutableStateOf(false) }
    var box64Expanded by remember { mutableStateOf(false) }
    var fexExpanded by remember { mutableStateOf(false) }

    val resolutions = listOf("800x600", "1024x768", "1280x720", "1600x900", "1920x1080", "2560x1440")
    val drivers = listOf("Turnip", "VirGL", "LLVMpipe", "Vulkan-Native")
    val dxvkVersions = listOf("1.10.3", "2.0", "2.1", "2.2", "2.3.1")
    val vkd3dVersions = listOf("2.6", "2.8", "2.10", "2.12")
    val box64Presets = listOf("Safe", "Intermediate", "Performance", "Aggressive")
    val fexPresets = listOf("Safe", "Intermediate", "Performance", "Aggressive")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownSettingSelector(
            label = "Screen Resolution",
            selectedValue = screenResolution,
            options = resolutions,
            expanded = resolutionExpanded,
            onExpandedChange = { resolutionExpanded = it },
            onSelectOption = onResolutionChange
        )

        DropdownSettingSelector(
            label = "Graphics Driver",
            selectedValue = graphicsDriver,
            options = drivers,
            expanded = driverExpanded,
            onExpandedChange = { driverExpanded = it },
            onSelectOption = onDriverChange
        )

        DropdownSettingSelector(
            label = "DXVK Version",
            selectedValue = dxvkVersion,
            options = dxvkVersions,
            expanded = dxvkExpanded,
            onExpandedChange = { dxvkExpanded = it },
            onSelectOption = onDxvkChange
        )

        DropdownSettingSelector(
            label = "VKD3D Version",
            selectedValue = vkd3dVersion,
            options = vkd3dVersions,
            expanded = vkd3dExpanded,
            onExpandedChange = { vkd3dExpanded = it },
            onSelectOption = onVkd3dChange
        )

        DropdownSettingSelector(
            label = "Box64 Preset Profile",
            selectedValue = box64Preset,
            options = box64Presets,
            expanded = box64Expanded,
            onExpandedChange = { box64Expanded = it },
            onSelectOption = onBox64Change
        )

        DropdownSettingSelector(
            label = "FEX-Core Preset Profile",
            selectedValue = fexCorePreset,
            options = fexPresets,
            expanded = fexExpanded,
            onExpandedChange = { fexExpanded = it },
            onSelectOption = onFexCoreChange
        )
    }
}

@Composable
private fun ControlsAndSoundSettingsTab(
    controlsProfile: String,
    onControlsProfileChange: (String) -> Unit,
    midiSoundFont: String,
    onMidiSoundFontChange: (String) -> Unit
) {
    var controlsExpanded by remember { mutableStateOf(false) }
    var midiExpanded by remember { mutableStateOf(false) }

    val controlsProfiles = listOf("Default", "RTS / Strategy", "FPS / Action", "Gamepad Emulation", "Custom Touch Layout")
    val soundFonts = listOf("Default", "GeneralUser GS", "FluidR3_GM", "Roland SC-55", "Custom SoundFont")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DropdownSettingSelector(
            label = "Input Controls Profile",
            selectedValue = controlsProfile,
            options = controlsProfiles,
            expanded = controlsExpanded,
            onExpandedChange = { controlsExpanded = it },
            onSelectOption = onControlsProfileChange
        )

        DropdownSettingSelector(
            label = "MIDI SoundFont Engine",
            selectedValue = midiSoundFont,
            options = soundFonts,
            expanded = midiExpanded,
            onExpandedChange = { midiExpanded = it },
            onSelectOption = onMidiSoundFontChange
        )
    }
}

@Composable
private fun AdvancedSettingsTab(
    cpuAffinity: String,
    onCpuAffinityChange: (String) -> Unit,
    envVars: String,
    onEnvVarsChange: (String) -> Unit
) {
    val cpuCores = listOf("All", "Performance Cores Only", "Efficiency Cores Only", "Core 0-3", "Core 4-7")
    var cpuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DropdownSettingSelector(
            label = "CPU Core Affinity",
            selectedValue = cpuAffinity,
            options = cpuCores,
            expanded = cpuExpanded,
            onExpandedChange = { cpuExpanded = it },
            onSelectOption = onCpuAffinityChange
        )

        OutlinedTextField(
            value = envVars,
            onValueChange = onEnvVarsChange,
            label = { Text("Environment Variables", color = Color.Gray) },
            placeholder = { Text("DXVK_HUD=1 MESA_EXTENSION_MAX_YEAR=2024", color = Color.DarkGray) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 5
        )
    }
}
@Composable
private fun DropdownSettingSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectOption: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = PSBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceColor)
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(DarkBg)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onSelectOption(option)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

private fun createPinnedShortcut(context: Context, shortcut: Shortcut) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val intent = Intent(context, XServerDisplayActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("shortcut_path", shortcut.file.absolutePath)
                putExtra("container_id", shortcut.container.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val iconBitmap = if (!shortcut.iconPath.isNullOrEmpty() && File(shortcut.iconPath).exists()) {
                BitmapFactory.decodeFile(shortcut.iconPath)
            } else null

            val icon = if (iconBitmap != null) {
                Icon.createWithBitmap(iconBitmap)
            } else {
                Icon.createWithResource(context, R.drawable.ic_shortcut_default)
            }

            val pinShortcutInfo = ShortcutInfo.Builder(context, shortcut.file.name)
                .setShortLabel(shortcut.name)
                .setLongLabel(shortcut.name)
                .setIcon(icon)
                .setIntent(intent)
                .build()

            shortcutManager.requestPinShortcut(pinShortcutInfo, null)
        } else {
            Toast.makeText(context, "Pinning shortcuts is not supported by your launcher.", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Pinned shortcuts require Android 8.0 or higher.", Toast.LENGTH_SHORT).show()
    }
}

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    RECENT
}

sealed class ImportResult {
    data class Success(val shortcutName: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class ShortcutsViewModel : androidx.lifecycle.ViewModel() {
    private val _sortOrder = kotlinx.coroutines.flow.MutableStateFlow(SortOrder.NAME_ASC)
    val sortOrder: kotlinx.coroutines.flow.StateFlow<SortOrder> = _sortOrder

    private val _isGridView = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isGridView: kotlinx.coroutines.flow.StateFlow<Boolean> = _isGridView

    private val _shortcuts = kotlinx.coroutines.flow.MutableStateFlow<List<Shortcut>>(emptyList())
    val shortcuts: kotlinx.coroutines.flow.StateFlow<List<Shortcut>> = _shortcuts

    init {
        loadShortcuts()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applySort()
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun launchShortcut(shortcut: Shortcut, activity: Activity) {
        val intent = Intent(activity, XServerDisplayActivity::class.java).apply {
            putExtra("shortcut_path", shortcut.file.absolutePath)
            putExtra("container_id", shortcut.container.id)
        }
        activity.startActivity(intent)
    }

    fun getContainers(): List<Container> {
        val containers = mutableListOf<Container>()
        val profilesDir = File(FileUtils.getProfilesDir())
        if (profilesDir.exists()) {
            profilesDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val id = file.name.toIntOrNull()
                    if (id != null) {
                        val container = Container(id)
                        container.name = file.name
                        containers.add(container)
                    }
                }
            }
        }
        return containers
    }

    fun getContainerById(id: Int): Container? {
        return getContainers().find { it.id == id }
    }

    private fun loadShortcuts() {
        val list = mutableListOf<Shortcut>()
        val shortcutsDir = File(FileUtils.getShortcutsDir())
        if (shortcutsDir.exists()) {
            shortcutsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".desktop")) {
                    val shortcut = Shortcut(file)
                    list.add(shortcut)
                }
            }
        }
        _shortcuts.value = list
        applySort()
    }

    private fun applySort() {
        val currentList = _shortcuts.value.toMutableList()
        when (_sortOrder.value) {
            SortOrder.NAME_ASC -> currentList.sortBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> currentList.sortByDescending { it.name.lowercase() }
            SortOrder.RECENT -> currentList.sortByDescending { it.file.lastModified() }
        }
        _shortcuts.value = currentList
    }

    fun createShortcutsForFiles(containerId: Int, files: List<File>, context: Context): Int {
        var count = 0
        val container = getContainerById(containerId) ?: return 0
        files.forEach { file ->
            val shortcutName = file.nameWithoutExtension
            val desktopFile = File(FileUtils.getShortcutsDir(), "$shortcutName.desktop")
            try {
                FileWriter(desktopFile).use { writer ->
                    writer.write("[Desktop Entry]\n")
                    writer.write("Name=$shortcutName\n")
                    writer.write("Exec=${file.absolutePath}\n")
                    writer.write("Type=Application\n")
                    writer.write("ContainerId=$containerId\n")
                }
                count++
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        loadShortcuts()
        return count
    }

    fun importShortcut(containerIndex: Int, uri: Uri, context: Context): ImportResult {
        val filePath = FileUtils.getFilePathFromUri(context, uri)
            ?: return ImportResult.Error("Failed to resolve file path from Uri.")
        
        val file = File(filePath)
        val defaultName = file.nameWithoutExtension
        val desktopFile = File(FileUtils.getShortcutsDir(), "$defaultName.desktop")
        
        return try {
            FileWriter(desktopFile).use { writer ->
                writer.write("[Desktop Entry]\n")
                writer.write("Name=$defaultName\n")
                writer.write("Exec=${file.absolutePath}\n")
                writer.write("Type=Application\n")
                writer.write("ContainerId=$containerIndex\n")
            }
            loadShortcuts()
            ImportResult.Success(defaultName)
        } catch (e: Exception) {
            ImportResult.Error(e.localizedMessage ?: "Failed to import shortcut.")
        }
    }

    fun finalizeImportName(containerIndex: Int, newName: String, context: Context) {
        val shortcutsDir = File(FileUtils.getShortcutsDir())
        val desktopFile = File(shortcutsDir, "$newName.desktop")
        if (!desktopFile.exists()) {
            loadShortcuts()
        }
    }

    fun updateShortcut(shortcut: Shortcut, context: Context) {
        shortcut.save()
        loadShortcuts()
    }

    fun cloneShortcut(shortcut: Shortcut, newName: String, context: Context) {
        val newDesktopFile = File(FileUtils.getShortcutsDir(), "$newName.desktop")
        try {
            shortcut.file.copyTo(newDesktopFile, overwrite = true)
            val cloned = Shortcut(newDesktopFile)
            cloned.name = newName
            cloned.save()
            loadShortcuts()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to clone shortcut", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeShortcut(shortcut: Shortcut, context: Context) {
        if (shortcut.file.exists()) {
            shortcut.file.delete()
        }
        if (shortcut.iconPath.isNotEmpty()) {
            val iconFile = File(shortcut.iconPath)
            if (iconFile.exists()) iconFile.delete()
        }
        loadShortcuts()
    }
}
