package com.winlator.star.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.R
import com.winlator.star.ui.screens.cyclingBlue
import com.winlator.star.ui.screens.cyclingRed
import com.winlator.star.ui.theme.Divider as DividerColor

/**
 * Which items get a custom-drawn icon (store logos, plus Containers/Contents, which used
 * to share the same folder glyph and were easy to confuse) vs. a stock Material icon.
 */
private enum class DrawerIconKind { GOG, EPIC, AMAZON, STEAM, CONTAINERS, CONTENTS, STOCK }

private fun iconKindFor(screen: Screen): DrawerIconKind = when (screen) {
    Screen.Gog        -> DrawerIconKind.GOG
    Screen.Epic       -> DrawerIconKind.EPIC
    Screen.Amazon     -> DrawerIconKind.AMAZON
    Screen.Steam      -> DrawerIconKind.STEAM
    Screen.Containers -> DrawerIconKind.CONTAINERS
    Screen.Contents   -> DrawerIconKind.CONTENTS
    else              -> DrawerIconKind.STOCK
}

private fun stockIconFor(screen: Screen): ImageVector = when (screen) {
    Screen.Shortcuts     -> Icons.Filled.OpenInNew
    Screen.InputControls -> Icons.Filled.SportsEsports
    Screen.AdrenoTools   -> Icons.Filled.Memory
    Screen.Saves         -> Icons.Filled.Save
    Screen.FileManager   -> Icons.Filled.FolderOpen
    Screen.Settings      -> Icons.Filled.Settings
    Screen.Appearance    -> Icons.Filled.Palette
    else                 -> Icons.Filled.Settings
}

/**
 * Each entry gets its own color family so items sit clearly apart from one another —
 * alternating blue/red rather than every icon reading identically. All cycling, all
 * matching the file manager's signature look, never purple.
 */
@Composable
private fun tintFor(screen: Screen): Color = when (screen) {
    Screen.Gog, Screen.Amazon, Screen.Containers -> cyclingBlue()
    Screen.Epic, Screen.Steam, Screen.Contents   -> cyclingRed()
    else -> cyclingBlue()
}

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    onLaunchStore: (Screen) -> Unit,
    onAbout: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showHelp) {
        HelpSupportDialog(
            onDismiss = { showHelp = false },
            onOpenUrl = { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Winlator",
                    style = MaterialTheme.typography.titleMedium,
                    color = cyclingBlue(),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "AnTuTu",
                    style = MaterialTheme.typography.titleMedium,
                    color = cyclingRed(),
                )
            }
        }

        Divider(color = DividerColor)

        SectionHeader("Emulation")
        DrawerItem(Screen.Shortcuts,     currentRoute, onNavigate)
        DrawerItem(Screen.Containers,    currentRoute, onNavigate)
        DrawerItem(Screen.Settings,      currentRoute, onNavigate)

        Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))

        SectionHeader("Tools")
        DrawerItem(Screen.InputControls, currentRoute, onNavigate)
        DrawerItem(Screen.AdrenoTools,   currentRoute, onNavigate)
        DrawerItem(Screen.Contents,      currentRoute, onNavigate)
        DrawerItem(Screen.FileManager,   currentRoute, onNavigate)
        DrawerItem(Screen.Saves,         currentRoute, onNavigate)
        // Vegas FrameGen (Screen.LsfgSettings) removed — currently broken.

        Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))

        SectionHeader("Game Stores")
        Screen.storeItems.forEach { screen ->
            DrawerStoreItem(screen, onLaunchStore)
        }

        Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))

        SectionHeader("About And Support")
        DrawerIconItem(
            label = "About",
            icon = Icons.Filled.Info,
            onClick = onAbout,
        )
        DrawerIconItem(
            label = "Help and Support",
            icon = Icons.Filled.HelpOutline,
            onClick = { showHelp = true },
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerIcon(screen: Screen, tint: Color, modifier: Modifier = Modifier) {
    when (iconKindFor(screen)) {
        DrawerIconKind.GOG        -> GogIcon(tint, modifier)
        DrawerIconKind.EPIC       -> EpicIcon(tint, modifier)
        DrawerIconKind.AMAZON     -> AmazonIcon(tint, modifier)
        DrawerIconKind.STEAM      -> SteamIcon(tint, modifier)
        DrawerIconKind.CONTAINERS -> ContainersIcon(tint, modifier)
        DrawerIconKind.CONTENTS   -> ContentsIcon(tint, modifier)
        DrawerIconKind.STOCK      -> Icon(imageVector = stockIconFor(screen), contentDescription = null, tint = tint, modifier = modifier)
    }
}

@Composable
private fun DrawerItem(screen: Screen, currentRoute: String, onNavigate: (Screen) -> Unit) {
    val selected = currentRoute == screen.route
    val tint = tintFor(screen)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(screen) }
            .background(if (selected) tint.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        DrawerIcon(screen, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = screen.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) tint else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DrawerStoreItem(screen: Screen, onLaunchStore: (Screen) -> Unit) {
    val tint = tintFor(screen)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchStore(screen) }
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        DrawerIcon(screen, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = screen.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DrawerIconItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------------
// Custom icons. Hand-drawn simplifications for at-a-glance recognition at 22dp — not
// pixel-precise logo reproductions. All drawn with plain Canvas primitives (no
// material-icons-extended dependency needed).
// ---------------------------------------------------------------------------------

/** GOG: a rounded badge with a simple smiling face, echoing GOG's mascot-style mark. */
@Composable
private fun GogIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.28f),
            style = Stroke(width = w * 0.11f)
        )
        val eyeR = w * 0.07f
        drawCircle(tint, radius = eyeR, center = Offset(w * 0.34f, h * 0.42f))
        drawCircle(tint, radius = eyeR, center = Offset(w * 0.66f, h * 0.42f))
        drawArc(
            color = tint,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
            topLeft = Offset(w * 0.22f, h * 0.38f),
            size = Size(w * 0.56f, h * 0.42f)
        )
    }
}

/** Epic: an angular shield outline with a blocky "E" mark inside. */
@Composable
private fun EpicIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.02f)
            lineTo(w * 0.92f, h * 0.18f)
            lineTo(w * 0.92f, h * 0.55f)
            cubicTo(w * 0.92f, h * 0.8f, w * 0.74f, h * 0.94f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.26f, h * 0.94f, w * 0.08f, h * 0.8f, w * 0.08f, h * 0.55f)
            lineTo(w * 0.08f, h * 0.18f)
            close()
        }
        drawPath(shield, color = tint, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))

        val ePath = Path().apply {
            moveTo(w * 0.34f, h * 0.30f)
            lineTo(w * 0.34f, h * 0.70f)
            moveTo(w * 0.34f, h * 0.30f)
            lineTo(w * 0.64f, h * 0.30f)
            moveTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.58f, h * 0.50f)
            moveTo(w * 0.34f, h * 0.70f)
            lineTo(w * 0.64f, h * 0.70f)
        }
        drawPath(ePath, color = tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
    }
}

/** Amazon: the smile-arrow swoosh underline, Amazon's most recognizable mark. */
@Composable
private fun AmazonIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val smile = Path().apply {
            moveTo(w * 0.10f, h * 0.52f)
            cubicTo(w * 0.30f, h * 0.80f, w * 0.70f, h * 0.80f, w * 0.90f, h * 0.52f)
        }
        drawPath(smile, color = tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
        val arrow = Path().apply {
            moveTo(w * 0.90f, h * 0.52f)
            lineTo(w * 0.78f, h * 0.50f)
            moveTo(w * 0.90f, h * 0.52f)
            lineTo(w * 0.81f, h * 0.66f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Steam: the atom-like large circle with two smaller connected circles. */
@Composable
private fun SteamIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = tint,
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.075f)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.64f),
            end = Offset(w * 0.62f, h * 0.36f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )
        drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.36f, h * 0.64f), style = Stroke(width = w * 0.06f))
        drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.62f, h * 0.36f))
    }
}

/** Containers: three stacked, slightly offset layers — reads as isolated environments,
 *  distinct from a plain folder. */
@Composable
private fun ContainersIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val layerW = w * 0.8f
        val layerH = h * 0.22f
        listOf(0.62f, 0.38f, 0.14f).forEach { topFrac ->
            drawRoundRect(
                color = tint,
                topLeft = Offset((w - layerW) / 2f, h * topFrac),
                size = Size(layerW, layerH),
                cornerRadius = CornerRadius(layerH * 0.25f),
                style = Stroke(width = w * 0.07f)
            )
        }
    }
}

/** Contents: a 2x2 grid of small squares — an asset/content grid, distinct from Containers'
 *  stacked layers and from File Manager's folder glyph. */
@Composable
private fun ContentsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cell = w * 0.38f
        val gap = w * 0.14f
        val startX = (w - (cell * 2 + gap)) / 2f
        val startY = (h - (cell * 2 + gap)) / 2f
        val corner = CornerRadius(cell * 0.22f)
        listOf(
            Offset(startX, startY),
            Offset(startX + cell + gap, startY),
            Offset(startX, startY + cell + gap),
            Offset(startX + cell + gap, startY + cell + gap),
        ).forEach { topLeft ->
            drawRoundRect(
                color = tint,
                topLeft = topLeft,
                size = Size(cell, cell),
                cornerRadius = corner,
                style = Stroke(width = w * 0.065f)
            )
        }
    }
}

@Composable
private fun HelpSupportDialog(onDismiss: () -> Unit, onOpenUrl: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help & Support") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "For bug reports, feature requests, and support, visit the GitHub repository.",
                    color = MaterialTheme.colorScheme.onSurface
                )
                SupportLink(
                    label = "GitHub Repository",
                    url = "https://github.com/The412Banner/star-compose",
                    onOpenUrl = onOpenUrl
                )
                SupportLink(
                    label = "Report an Issue",
                    url = "https://github.com/The412Banner/star-compose/issues",
                    onOpenUrl = onOpenUrl
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun SupportLink(label: String, url: String, onOpenUrl: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(url) }
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.OpenInNew,
            contentDescription = null,
            tint = cyclingBlue(),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = cyclingBlue(),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
    }
}
