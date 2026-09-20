package com.winlator.star.core

import android.content.Context
import java.io.File

/**
 * Installs libbcn_layer.so — bundled as assets/graphics_driver/leegao_bcn.tzst — into the
 * shared Linux rootfs at Z:/usr/lib, overwriting any existing copy.
 *
 * This intentionally does NOT extract the whole archive. leegao_bcn.tzst's own internal
 * layout is "leegao_bcn/usr/lib/libbcn_layer.so" (plus whatever else lives alongside it),
 * but we only want that one file, redirected straight to Z:. TarCompressorUtils.extract's
 * OnExtractFileListener hook lets us do that: it's called once per archive entry (files AND
 * directories) with the path it *would* extract to, and we can return a different File to
 * redirect that entry, or null to skip it entirely. Returning null for every entry except
 * the .so means nothing else in the archive ever touches disk.
 *
 * NOTE: OnExtractFileListener is assumed to be a top-level interface in this same package
 * (com.winlator.star.core), matching how it's used unqualified inside TarCompressorUtils.java
 * itself. If it turns out to actually be a nested type (TarCompressorUtils.OnExtractFileListener),
 * this is a one-line fix to the reference below.
 *
 * This does blocking file I/O (decompressing a .tzst) — call it from a background thread
 * (e.g. Dispatchers.IO), not directly on the main/UI thread.
 */
object BcnLayerInstaller {
    private const val ASSET_PATH = "graphics_driver/leegao_bcn.tzst"
    private const val TARGET_FILE_NAME = "libbcn_layer.so"

    /** Z: drive root — matches how FileManagerScreen.kt resolves Z: for its own file browser. */
    private fun imagefsRoot(context: Context): File = File(context.filesDir, "imagefs")

    /**
     * Extracts just libbcn_layer.so from the bundled archive into Z:/usr/lib, overwriting
     * whatever's already there (FileOutputStream truncates an existing file by default, so
     * no separate delete step is needed). Returns true if the file exists at the target
     * path afterward.
     */
    fun installToDriveZ(context: Context): Boolean {
        val targetDir = File(imagefsRoot(context), "usr/lib")
        if (!targetDir.exists() && !targetDir.mkdirs()) return false
        val targetFile = File(targetDir, TARGET_FILE_NAME)

        // Never actually read from or written to — purely a naming anchor so
        // TarCompressorUtils has *some* destination to build candidate File paths
        // against for the listener to inspect. Since every entry either redirects to
        // targetFile or is skipped (null), nothing is ever created under this path.
        val scratchAnchor = File(context.cacheDir, "bcn_layer_scratch")

        val listener = OnExtractFileListener { file, _ ->
            if (file.name == TARGET_FILE_NAME) targetFile else null
        }

        val extractOk = TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            ASSET_PATH,
            scratchAnchor,
            listener
        )

        return extractOk && targetFile.isFile
    }
}
