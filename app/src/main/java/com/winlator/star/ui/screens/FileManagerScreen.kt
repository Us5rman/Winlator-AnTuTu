package com.winlator.star.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class DriveSpec(val letter: String, val label: String, val path: File)

@Composable
fun FileManagerScreen() {
    val drives = remember {
        listOf(
            DriveSpec("C", "Drive C:", File("/data/data/com.winlator.star/files/imagefs/drive_c")),
            DriveSpec("F", "Drive F:", Environment.getExternalStorageDirectory()),
            DriveSpec("Z", "Drive Z:", File("/"))
        )
    }

    var activeDrive by remember { mutableStateOf(drives[0]) }
    var currentDirectory by remember { mutableStateOf(activeDrive.path) }

    val fileList = remember(currentDirectory) {
        currentDirectory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    val electricBlue = Color(0xFF00E5FF)
    val neonRed = Color(0xFFFF0055)
    val pitchBlack = Color(0xFF0A0A0C)
    val cardBackground = Color(0xFF121214)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pitchBlack)
            .padding(16.dp)
    ) {
        // Drive Selector Row (Matching tab pill aesthetic)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            drives.forEach { drive ->
                val isSelected = activeDrive.letter == drive.letter
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) electricBlue else pitchBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) electricBlue else Color(0xFF2A2A2E),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            activeDrive = drive
                            currentDirectory = drive.path
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = drive.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dual-tone Red Accent Action Bar (Matching "Install content" banner)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = neonRed),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (currentDirectory.parentFile != null && currentDirectory != activeDrive.path) {
                        currentDirectory = currentDirectory.parentFile!!
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
                    text = if (currentDirectory == activeDrive.path) "Root Directory" else "Go Up One Directory",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Current Path Display
        Text(
            text = currentDirectory.absolutePath,
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141418), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF222228), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(12.dp))

        // File/Directory List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fileList) { file ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFF1E1E24), RoundedCornerShape(10.dp))
                        .clickable {
                            if (file.isDirectory) {
                                currentDirectory = file
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (file.isDirectory) electricBlue else neonRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
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
                    }
                }
            }
        }
    }
}
