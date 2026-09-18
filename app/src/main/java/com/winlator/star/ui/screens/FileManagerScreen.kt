package com.winlator.star.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winlator.star.container.Container
import com.winlator.star.container.ContainerManager
import com.winlator.star.core.ArchiveFormat
import com.winlator.star.core.ArchiveResult
import com.winlator.star.core.ArchiveUtils
import com.winlator.star.core.CompressionLevel
import com.winlator.star.core.FileOpNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

// Theme accents tuned to match the rest of the app.
private val WinlatorBlue = Color(0xFF0072CE)     // "Winlator-ish" blue, matches PSBlue elsewhere
private val AnTuTuRed = Color(0xFFE60012)        // Proper AnTuTu-style saturated red (not neon pink)
private val PitchBlack = Color(0xFF0A0A0C)
private val CardBackground = Color(0xFF121214)
private val BorderColor = Color(0xFF222228)

/**
 * Which drive is currently selected. C: is scoped to a specific [container] (each container
 * has its own Windows prefix). F: and Z: are universal — [container] is null for them, since
 * they resolve to the same path regardless of which container you're in.
 */
private data class DriveSelection(val container: Container?, val driveLetter: String) {
    fun rootPath(context: Context): File = when (driveLetter) {
        "F" -> Environment.getExternalStorageDirectory() // universal shared Android storage
        "Z" -> File(context.filesDir, "imagefs") // universal Linux rootfs shared by all containers
        else -> container?.let { File(it.rootDir, ".wine/drive_c") }
            ?: File(context.filesDir, "imagefs")
    }

    fun label(): String = when (driveLetter) {
        "F" -> "Shared Storage — F:"
        "Z" -> "System Root — Z:"
        else -> "${container?.name ?: "Unknown"} — $driveLetter:"
    }
}

/** Pending clipboard action for copy/cut. */
private data class ClipboardEntry(val file: File, val isCut: Boolean)

/**
 * Drives the shared progress overlay/notification for copy, move, extract, and compress.
 * [total] of -1 means the underlying operation can't report a total (indeterminate progress,
 * e.g. TAR/7z streaming formats, or a RAR extract which is all-or-nothing).
 */
private data class FileOpProgress(
    val title: String,   // "Copying", "Moving", "Extracting", "Compressing"
    val itemName: String,
    val done: Int,
    val total: Int,
    val isComplete: Boolean,
    val failed: Boolean = false,
) {
    val isIndeterminate: Boolean get() = total <= 0 && !isComplete
    val fraction: Float get() = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val percent: Int get() = (fraction * 100).toInt()
}

/** File extensions that can be opened in the built-in text editor. */
private val TEXT_EDITABLE_EXTENSIONS = setOf(
    "ini", "xml", "txt", "cfg", "conf", "json", "log", "reg", "properties", "yaml", "yml", "sh", "bat"
)

private fun File.isTextEditable(): Boolean =
    isFile && (extension.lowercase() in TEXT_EDITABLE_EXTENSIONS)

/** Picks a distinct icon per file category, so the list reads like a real file manager. */
private fun iconFor(file: File): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    if (ArchiveUtils.isArchive(file)) return Icons.Default.FolderZip

    return when (file.extension.lowercase()) {
        "exe", "msi", "bat", "sh" -> Icons.Default.Terminal
        "ini", "cfg", "conf", "reg", "properties", "yaml", "yml" -> Icons.Default.Settings
        "xml", "json", "log" -> Icons.Default.Code
        "txt", "md" -> Icons.Default.Description
        "png", "jpg", "jpeg", "bmp", "webp", "gif", "ico" -> Icons.Default.Image
        "mp3", "wav", "ogg", "flac", "m4a" -> Icons.Default.Audiotrack
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Default.MovieFilter
        "pdf" -> Icons.Default.PictureAsPdf
        "dll", "sys", "so" -> Icons.Default.Widgets
        "lnk", "desktop" -> Icons.Default.Link
        else -> Icons.Default.InsertDriveFile
    }
}

/** Tint per category — keeps the red/blue accent scheme but still visually distinguishes types. */
private fun iconTintFor(file: File): Color = when {
    file.isDirectory -> WinlatorBlue
    ArchiveUtils.isArchive(file) -> Color(0xFFFFA726) // amber, archives stand out from both accents
    file.extension.lowercase() in setOf("exe", "msi", "bat", "sh") -> Color(0xFF66BB6A) // green, executables
    file.isTextEditable() -> WinlatorBlue
    else -> AnTuTuRed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val containerManager = remember { ContainerManager(context) }
    val containers = remember { containerManager.getContainers() }

    var showContainerPicker by remember { mutableStateOf(false) }
    var pickerDriveLetter by remember { mutableStateOf("C") }

    var selection by remember {
        mutableStateOf(
            if (containers.isNotEmpty()) DriveSelection(containers[0], "C") else null
        )
    }

    var currentDirectory by remember(selection) {
        mutableStateOf(selection?.rootPath(context))
    }

    var clipboard by remember { mutableStateOf<ClipboardEntry?>(null) }

    // Dialog state
    var contextMenuFile by remember { mutableStateOf<File?>(null) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var showTextEditorFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<File?>(null) }
    var showCreateShortcutFor by remember { mutableStateOf<File?>(null) }
    var showCompressDialogFor by remember { mutableStateOf<File?>(null) }

    // Unified progress overlay state, shared by copy/cut/extract/compress
    var fileOpProgress by remember { mutableStateOf<FileOpProgress?>(null) }
    var fileOpJob by remember { mutableStateOf<Job?>(null) }
    val fileOpCancelled = remember { AtomicBoolean(false) }

    val fileList = remember(currentDirectory) {
        currentDirectory?.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }

    fun refresh() {
        // Reassign to force recomposition of fileList via currentDirectory key
        currentDirectory = currentDirectory?.let { File(it.absolutePath) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(16.dp)
    ) {
        // Drive Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("C", "F", "Z").forEach { letter ->
                val isSelected = selection?.driveLetter == letter
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) WinlatorBlue else PitchBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) WinlatorBlue else BorderColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            if (letter == "C") {
                                if (containers.isEmpty()) {
                                    Toast.makeText(context, "No containers found.", Toast.LENGTH_SHORT).show()
                                } else {
                                    pickerDriveLetter = letter
                                    showContainerPicker = true
                                }
                            } else {
                                // F: and Z: are universal — same path regardless of container,
                                // so select them directly instead of prompting for a container.
                                selection = DriveSelection(container = null, driveLetter = letter)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Drive $letter:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Active selection subtitle (which container is backing the current drive)
        selection?.let {
            Text(
                text = it.label(),
                fontSize = 11.sp,
                color = Color(0xFF888899),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Action Bar: Go up / Root, plus Create File / Create Folder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AnTuTuRed),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val root = selection?.rootPath(context)
                        val parent = currentDirectory?.parentFile
                        if (parent != null && currentDirectory != root) {
                            currentDirectory = parent
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (currentDirectory == selection?.rootPath(context)) "Root Directory" else "Go Up",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .clickable { showCreateFolderDialog = true }
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = WinlatorBlue)
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .clickable { showCreateFileDialog = true }
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = WinlatorBlue)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Current Path Display
        Text(
            text = currentDirectory?.absolutePath ?: "No container selected",
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141418), RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // Paste bar, shown only when something is queued
        clipboard?.let { entry ->
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WinlatorBlue.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WinlatorBlue, RoundedCornerShape(12.dp))
                    .clickable {
                        val destDir = currentDirectory ?: return@clickable
                        val dest = File(destDir, entry.file.name)
                        clipboard = null
                        fileOpCancelled.set(false)
                        val opTitle = if (entry.isCut) "Moving" else "Copying"
                        fileOpProgress = FileOpProgress(opTitle, entry.file.name, 0, -1, isComplete = false)
                        fileOpJob = coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                if (entry.isCut) {
                                    moveRecursively(entry.file, dest, fileOpCancelled) { done, total, name ->
                                        fileOpProgress = FileOpProgress(opTitle, name, done, total, isComplete = false)
                                        FileOpNotifier.update(context, opTitle, name, if (total > 0) (done * 100 / total) else 0, total <= 0)
                                    }
                                } else {
                                    copyRecursively(entry.file, dest, fileOpCancelled) { done, total, name ->
                                        fileOpProgress = FileOpProgress(opTitle, name, done, total, isComplete = false)
                                        FileOpNotifier.update(context, opTitle, name, if (total > 0) (done * 100 / total) else 0, total <= 0)
                                    }
                                }
                            }
                            FileOpNotifier.clear(context)
                            when (result) {
                                ArchiveResult.SUCCESS -> {
                                    fileOpProgress = fileOpProgress?.copy(isComplete = true)
                                    refresh()
                                }
                                ArchiveResult.CANCELLED -> {
                                    fileOpProgress = null
                                }
                                ArchiveResult.FAILED -> {
                                    fileOpProgress = null
                                    Toast.makeText(context, "Paste failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (entry.isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = WinlatorBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Paste \"${entry.file.name}\" here",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { clipboard = null }) {
                        Text("Cancel", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // File/Directory List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fileList) { file ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFF1E1E24), RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = {
                                if (file.isDirectory) {
                                    currentDirectory = file
                                } else if (file.isTextEditable()) {
                                    showTextEditorFile = file
                                }
                            },
                            onLongClick = { contextMenuFile = file }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconFor(file),
                            contentDescription = null,
                            tint = iconTintFor(file),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (file.isDirectory) "Directory" else "${file.length() / 1024} KB",
                                fontSize = 11.sp,
                                color = Color(0xFF777788)
                            )
                        }
                        if (clipboard?.file == file) {
                            Icon(
                                imageVector = if (clipboard?.isCut == true) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = WinlatorBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Container / drive picker dialog ----
    if (showContainerPicker) {
        Dialog(onDismissRequest = { showContainerPicker = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose a container — Drive $pickerDriveLetter:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        containers.forEach { container ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selection = DriveSelection(container, pickerDriveLetter)
                                        showContainerPicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = WinlatorBlue)
                                Spacer(Modifier.width(12.dp))
                                Text(container.name, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    TextButton(
                        onClick = { showContainerPicker = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = Color(0xFFAAAAAA))
                    }
                }
            }
        }
    }

    // ---- Long-press context menu ----
    contextMenuFile?.let { file ->
        Dialog(onDismissRequest = { contextMenuFile = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = file.name,
                        color = Color(0xFF888899),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    ContextMenuAction(Icons.Default.ContentCopy, "Copy") {
                        clipboard = ClipboardEntry(file, isCut = false)
                        contextMenuFile = null
                    }
                    ContextMenuAction(Icons.Default.ContentCut, "Cut") {
                        clipboard = ClipboardEntry(file, isCut = true)
                        contextMenuFile = null
                    }
                    if (ArchiveUtils.isArchive(file)) {
                        ContextMenuAction(Icons.Default.UnfoldMore, "Extract Here") {
                            contextMenuFile = null
                            val destDir = File(file.parentFile, file.nameWithoutExtension)
                            fileOpCancelled.set(false)
                            fileOpProgress = FileOpProgress("Extracting", file.name, 0, -1, isComplete = false)
                            fileOpJob = coroutineScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    ArchiveUtils.extract(file, destDir) { archiveProgress ->
                                        fileOpProgress = FileOpProgress(
                                            "Extracting", archiveProgress.currentEntryName,
                                            archiveProgress.entriesDone, archiveProgress.totalEntries,
                                            isComplete = false
                                        )
                                        val pct = if (archiveProgress.totalEntries > 0)
                                            (archiveProgress.entriesDone * 100 / archiveProgress.totalEntries) else 0
                                        FileOpNotifier.update(context, "Extracting", archiveProgress.currentEntryName, pct, archiveProgress.totalEntries <= 0)
                                        !fileOpCancelled.get()
                                    }
                                }
                                FileOpNotifier.clear(context)
                                when (result) {
                                    ArchiveResult.SUCCESS -> {
                                        fileOpProgress = fileOpProgress?.copy(isComplete = true)
                                        refresh()
                                    }
                                    ArchiveResult.CANCELLED -> {
                                        fileOpProgress = null
                                    }
                                    ArchiveResult.FAILED -> {
                                        fileOpProgress = null
                                        val hint = if (ArchiveFormat.fromFile(file) == ArchiveFormat.RAR)
                                            "Extract failed. Note: RAR5 archives are not supported, only RAR4 and older."
                                        else "Extract failed."
                                        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    } else {
                        ContextMenuAction(Icons.Default.FolderZip, "Compress") {
                            showCompressDialogFor = file
                            contextMenuFile = null
                        }
                    }
                    ContextMenuAction(Icons.Default.Link, "Create Shortcut") {
                        showCreateShortcutFor = file
                        contextMenuFile = null
                    }
                    ContextMenuAction(Icons.Default.Edit, "Edit") {
                        showRenameDialog = file
                        contextMenuFile = null
                    }
                    ContextMenuAction(Icons.Default.Delete, "Delete", tint = AnTuTuRed) {
                        showDeleteConfirm = file
                        contextMenuFile = null
                    }
                }
            }
        }
    }

    // ---- Edit menu: Rename vs Edit-as-text (only for supported extensions) ----
    showRenameDialog?.let { file ->
        var newName by remember(file) { mutableStateOf(file.name) }
        Dialog(onDismissRequest = { showRenameDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("File name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Rename button — always available
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WinlatorBlue),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val dest = File(file.parentFile, newName)
                                    if (file.renameTo(dest)) {
                                        showRenameDialog = null
                                        refresh()
                                    } else {
                                        Toast.makeText(context, "Rename failed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Rename", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Edit-as-text button — only for supported text formats
                        if (file.isTextEditable()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AnTuTuRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showRenameDialog = null
                                        showTextEditorFile = file
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Edit Text", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showRenameDialog = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel", color = AnTuTuRed)
                    }
                }
            }
        }
    }

    // ---- Text editor dialog ----
    showTextEditorFile?.let { file ->
        var content by remember(file) {
            mutableStateOf(runCatching { file.readText() }.getOrDefault(""))
        }
        Dialog(
            onDismissRequest = { showTextEditorFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTextEditorFile = null }) {
                            Text("Cancel", color = WinlatorBlue)
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = AnTuTuRed),
                            modifier = Modifier.clickable {
                                val ok = runCatching { file.writeText(content) }.isSuccess
                                if (ok) {
                                    refresh()
                                    Toast.makeText(context, "Saved.", Toast.LENGTH_SHORT).show()
                                    showTextEditorFile = null
                                } else {
                                    Toast.makeText(context, "Failed to save.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Delete confirmation ----
    showDeleteConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete \"${file.name}\"?") },
            text = { Text(if (file.isDirectory) "This will delete the folder and everything inside it." else "This file will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = deleteRecursively(file)
                    showDeleteConfirm = null
                    if (ok) refresh() else Toast.makeText(context, "Delete failed.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Delete", color = AnTuTuRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = WinlatorBlue)
                }
            }
        )
    }

    // ---- Compress dialog: pick a name and a compression level (Ultra -> None) ----
    showCompressDialogFor?.let { file ->
        var archiveName by remember(file) { mutableStateOf("${file.nameWithoutExtension}.zip") }
        var selectedLevel by remember { mutableStateOf(CompressionLevel.NORMAL) }

        Dialog(onDismissRequest = { showCompressDialogFor = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = archiveName,
                        onValueChange = { archiveName = it },
                        label = { Text("Archive name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Compression Level", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompressionLevel.entries.forEach { level ->
                            val isSelected = selectedLevel == level
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) WinlatorBlue.copy(alpha = 0.18f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) WinlatorBlue else BorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedLevel = level }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = level.label,
                                    color = if (isSelected) WinlatorBlue else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Lv ${level.deflaterLevel}",
                                    color = Color(0xFF777788),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCompressDialogFor = null }) {
                            Text("Cancel", color = AnTuTuRed)
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = WinlatorBlue),
                            modifier = Modifier.clickable {
                                val dir = currentDirectory
                                if (dir != null && archiveName.isNotBlank()) {
                                    val destination = File(dir, archiveName)
                                    showCompressDialogFor = null
                                    fileOpCancelled.set(false)
                                    fileOpProgress = FileOpProgress("Compressing", archiveName, 0, -1, isComplete = false)
                                    fileOpJob = coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            ArchiveUtils.compressToZip(listOf(file), destination, selectedLevel) { archiveProgress ->
                                                fileOpProgress = FileOpProgress(
                                                    "Compressing", archiveProgress.currentEntryName,
                                                    archiveProgress.entriesDone, archiveProgress.totalEntries,
                                                    isComplete = false
                                                )
                                                val pct = if (archiveProgress.totalEntries > 0)
                                                    (archiveProgress.entriesDone * 100 / archiveProgress.totalEntries) else 0
                                                FileOpNotifier.update(context, "Compressing", archiveProgress.currentEntryName, pct, archiveProgress.totalEntries <= 0)
                                                !fileOpCancelled.get()
                                            }
                                        }
                                        FileOpNotifier.clear(context)
                                        when (result) {
                                            ArchiveResult.SUCCESS -> {
                                                fileOpProgress = fileOpProgress?.copy(isComplete = true)
                                                refresh()
                                            }
                                            ArchiveResult.CANCELLED -> {
                                                fileOpProgress = null
                                            }
                                            ArchiveResult.FAILED -> {
                                                fileOpProgress = null
                                                Toast.makeText(context, "Compression failed.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Compress", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Copy / move / extract / compress progress overlay ----
    fileOpProgress?.let { op ->
        val infiniteTransition = rememberInfiniteTransition(label = "fileOpShimmer")
        val shimmerPos by infiniteTransition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "fileOpShimmerPos",
        )

        Dialog(
            onDismissRequest = { /* only the Cancel/OK buttons can dismiss this */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.White.copy(alpha = 0.35f),
                        spotColor = Color.White.copy(alpha = 0.5f),
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (op.isComplete) "${op.title} Complete" else "${op.title}…",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = op.itemName,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))

                    if (op.isIndeterminate) {
                        LinearProgressIndicator(
                            color = WinlatorBlue,
                            trackColor = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        GlowingProgressBar(
                            progress = if (op.isComplete) 1f else op.fraction,
                            shimmerPos = shimmerPos,
                            isComplete = op.isComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (op.isIndeterminate) "Working…" else "${op.percent}%",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    if (op.isComplete) {
                        GradientButton(
                            text = "OK",
                            onClick = {
                                fileOpProgress = null
                                fileOpJob = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextButton(
                            onClick = {
                                fileOpCancelled.set(true)
                                fileOpJob?.cancel()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel", color = AnTuTuRed)
                        }
                    }
                }
            }
        }
    }

    // ---- Create shortcut dialog (simple .lnk-style pointer file within the same directory) ----
    showCreateShortcutFor?.let { file ->
        Dialog(onDismissRequest = { showCreateShortcutFor = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create Shortcut", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A shortcut to \"${file.name}\" will be created in this folder.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateShortcutFor = null }) {
                            Text("Cancel", color = AnTuTuRed)
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = WinlatorBlue),
                            modifier = Modifier.clickable {
                                val shortcutFile = File(file.parentFile, "${file.nameWithoutExtension} - Shortcut.lnk")
                                val ok = runCatching {
                                    shortcutFile.writeText(file.absolutePath)
                                }.isSuccess
                                showCreateShortcutFor = null
                                if (ok) refresh() else Toast.makeText(context, "Failed to create shortcut.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Create", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Create new folder dialog ----
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("New Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateFolderDialog = false }) {
                            Text("Cancel", color = AnTuTuRed)
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = WinlatorBlue),
                            modifier = Modifier.clickable {
                                val dir = currentDirectory
                                if (dir != null && folderName.isNotBlank()) {
                                    val ok = File(dir, folderName).mkdirs()
                                    showCreateFolderDialog = false
                                    if (ok) refresh() else Toast.makeText(context, "Failed to create folder.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Create", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Create new file dialog ----
    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCreateFileDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("New File", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File name (e.g. config.ini)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateFileDialog = false }) {
                            Text("Cancel", color = AnTuTuRed)
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = WinlatorBlue),
                            modifier = Modifier.clickable {
                                val dir = currentDirectory
                                if (dir != null && fileName.isNotBlank()) {
                                    val ok = runCatching { File(dir, fileName).createNewFile() }.getOrDefault(false)
                                    showCreateFileDialog = false
                                    if (ok) refresh() else Toast.makeText(context, "Failed to create file.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Create", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = WinlatorBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

/**
 * Copies [src] into [dst] recursively, reporting progress via [onProgress] as
 * (filesDone, totalFiles, currentFileName) after each file, and checking [cancelled]
 * between files so a user-requested cancel actually stops the operation rather than
 * just hiding the dialog while work continues in the background.
 */
private fun copyRecursively(
    src: File,
    dst: File,
    cancelled: AtomicBoolean,
    onProgress: (done: Int, total: Int, name: String) -> Unit
): ArchiveResult {
    val allFiles = mutableListOf<File>()
    collectAllFiles(src, allFiles)
    val total = allFiles.size

    return try {
        for (index in allFiles.indices) {
            if (cancelled.get()) return ArchiveResult.CANCELLED
            val file = allFiles[index]
            val relative = file.relativeTo(src)
            val target = if (relative.path.isEmpty()) dst else File(dst, relative.path)
            onProgress(index, total, file.name)

            if (file.isDirectory) {
                if (!target.exists() && !target.mkdirs()) return ArchiveResult.FAILED
            } else {
                target.parentFile?.let { if (!it.exists()) it.mkdirs() }
                file.copyTo(target, overwrite = true)
            }
        }
        ArchiveResult.SUCCESS
    } catch (e: Exception) {
        ArchiveResult.FAILED
    }
}

/**
 * Moves [src] to [dst]. Tries a fast atomic [File.renameTo] first (works when both paths
 * are on the same filesystem/volume); if that fails — e.g. moving between F: (real Android
 * storage) and a container's internal storage, which are different volumes — falls back to
 * copying then deleting the original, with the same progress/cancellation support as a copy.
 */
private fun moveRecursively(
    src: File,
    dst: File,
    cancelled: AtomicBoolean,
    onProgress: (done: Int, total: Int, name: String) -> Unit
): ArchiveResult {
    onProgress(0, -1, src.name)
    if (src.renameTo(dst)) return ArchiveResult.SUCCESS

    val copyResult = copyRecursively(src, dst, cancelled, onProgress)
    if (copyResult == ArchiveResult.SUCCESS) {
        if (!deleteRecursively(src)) {
            // Copy succeeded but couldn't clean up the source — not a failure the user
            // needs to retry, but worth knowing about if this ever gets logged.
        }
    }
    return copyResult
}

private fun collectAllFiles(file: File, out: MutableList<File>) {
    out.add(file)
    if (file.isDirectory) {
        file.listFiles()?.forEach { collectAllFiles(it, out) }
    }
}

private fun deleteRecursively(file: File): Boolean {
    return try {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    } catch (e: Exception) {
        false
    }
}
