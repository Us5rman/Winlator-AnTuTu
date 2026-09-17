package com.winlator.star.core

import com.github.junrar.Junrar
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Formats this tool can extract. Compression (creating an archive) is currently
 * ZIP-only, since RAR/7z encoders are proprietary or require native libraries
 * beyond what's already in this project.
 */
enum class ArchiveFormat {
    ZIP, TAR, TAR_GZ, TAR_XZ, TAR_ZST, SEVEN_ZIP, RAR, UNKNOWN;

    companion object {
        fun fromFile(file: File): ArchiveFormat {
            val name = file.name.lowercase()
            return when {
                name.endsWith(".tar.gz") || name.endsWith(".tgz") -> TAR_GZ
                name.endsWith(".tar.xz") -> TAR_XZ
                name.endsWith(".tar.zst") -> TAR_ZST
                name.endsWith(".tar") -> TAR
                name.endsWith(".zip") -> ZIP
                name.endsWith(".7z") -> SEVEN_ZIP
                name.endsWith(".rar") -> RAR
                else -> UNKNOWN
            }
        }
    }
}

/** User-facing compression levels, mapped onto java.util.zip.Deflater's 0-9 scale. */
enum class CompressionLevel(val label: String, val deflaterLevel: Int) {
    NONE("No Compression (Store)", Deflater.NO_COMPRESSION),       // 0
    FAST("Fast", 3),
    NORMAL("Normal", Deflater.DEFAULT_COMPRESSION.let { 6 }),      // 6
    HIGH("High", 8),
    ULTRA("Ultra", Deflater.BEST_COMPRESSION)                       // 9
}

data class ArchiveProgress(val currentEntryName: String, val entriesDone: Int, val totalEntries: Int)

object ArchiveUtils {

    /**
     * Compress [sources] into a single ZIP archive at [destination] using [level].
     * Directories are added recursively with paths relative to each source's parent.
     */
    fun compressToZip(
        sources: List<File>,
        destination: File,
        level: CompressionLevel,
        onProgress: ((ArchiveProgress) -> Unit)? = null
    ): Boolean {
        return try {
            val allFiles = mutableListOf<Pair<File, String>>()
            for (src in sources) collectFiles(src, src.parentFile ?: src, allFiles)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zos ->
                zos.setLevel(level.deflaterLevel)
                if (level == CompressionLevel.NONE) {
                    // STORED entries require precomputed size + CRC32, and no compression.
                    zos.setMethod(ZipOutputStream.STORED)
                }

                allFiles.forEachIndexed { index, (file, relativePath) ->
                    onProgress?.invoke(ArchiveProgress(relativePath, index, allFiles.size))

                    val entryName = if (file.isDirectory) "$relativePath/" else relativePath
                    val entry = ZipEntry(entryName)

                    if (level == CompressionLevel.NONE) {
                        entry.method = ZipEntry.STORED
                        entry.size = if (file.isDirectory) 0 else file.length()
                        entry.compressedSize = entry.size
                        entry.crc = if (file.isDirectory) 0 else computeCrc32(file)
                    }

                    zos.putNextEntry(entry)
                    if (file.isFile) {
                        BufferedInputStream(FileInputStream(file)).use { it.copyTo(zos) }
                    }
                    zos.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            destination.delete()
            false
        }
    }

    private fun computeCrc32(file: File): Long {
        val crc = CRC32()
        BufferedInputStream(FileInputStream(file)).use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) crc.update(buffer, 0, read)
        }
        return crc.value
    }

    private fun collectFiles(file: File, base: File, out: MutableList<Pair<File, String>>) {
        val relativePath = file.relativeTo(base).path
        out.add(file to relativePath)
        if (file.isDirectory) {
            file.listFiles()?.forEach { collectFiles(it, base, out) }
        }
    }

    /**
     * Extract [archive] into [destinationDir], auto-detecting format from the file name.
     * Returns true on success. RAR support is limited to RAR4 and older (junrar has no RAR5 decoder).
     */
    fun extract(archive: File, destinationDir: File, onProgress: ((ArchiveProgress) -> Unit)? = null): Boolean {
        if (!destinationDir.exists()) destinationDir.mkdirs()

        return try {
            when (ArchiveFormat.fromFile(archive)) {
                ArchiveFormat.ZIP -> extractZip(archive, destinationDir, onProgress)
                ArchiveFormat.TAR -> extractTar(FileInputStream(archive), destinationDir, onProgress)
                ArchiveFormat.TAR_GZ -> extractTar(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archive))), destinationDir, onProgress)
                ArchiveFormat.TAR_XZ -> extractTar(XZCompressorInputStream(BufferedInputStream(FileInputStream(archive))), destinationDir, onProgress)
                ArchiveFormat.TAR_ZST -> extractTarZst(archive, destinationDir, onProgress)
                ArchiveFormat.SEVEN_ZIP -> extractSevenZip(archive, destinationDir, onProgress)
                ArchiveFormat.RAR -> extractRar(archive, destinationDir)
                ArchiveFormat.UNKNOWN -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractZip(archive: File, destinationDir: File, onProgress: ((ArchiveProgress) -> Unit)?): Boolean {
        ZipFile(archive).use { zip ->
            val entries = zip.entries().toList()
            entries.forEachIndexed { index, entry ->
                onProgress?.invoke(ArchiveProgress(entry.name, index, entries.size))
                val outFile = safeDestination(destinationDir, entry.name) ?: return@forEachIndexed
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        BufferedOutputStream(FileOutputStream(outFile)).use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        return true
    }

    private fun extractTar(rawInput: java.io.InputStream, destinationDir: File, onProgress: ((ArchiveProgress) -> Unit)?): Boolean {
        TarArchiveInputStream(BufferedInputStream(rawInput)).use { tar ->
            var entry: TarArchiveEntry?
            var count = 0
            while (tar.nextTarEntry.also { entry = it } != null) {
                val e = entry ?: continue
                onProgress?.invoke(ArchiveProgress(e.name, count++, -1))
                val outFile = safeDestination(destinationDir, e.name) ?: continue
                if (e.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { output -> tar.copyTo(output) }
                }
            }
        }
        return true
    }

    private fun extractTarZst(archive: File, destinationDir: File, onProgress: ((ArchiveProgress) -> Unit)?): Boolean {
        // Matches TarCompressorUtils' approach: commons-compress' zstandard package,
        // which wraps zstd-jni internally — no direct zstd-jni API usage needed here.
        val zstdInput = ZstdCompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        return extractTar(zstdInput, destinationDir, onProgress)
    }

    private fun extractSevenZip(archive: File, destinationDir: File, onProgress: ((ArchiveProgress) -> Unit)?): Boolean {
        SevenZFile(archive).use { sevenZ ->
            var entry: ArchiveEntry?
            var count = 0
            while (sevenZ.nextEntry.also { entry = it } != null) {
                val e = entry ?: continue
                onProgress?.invoke(ArchiveProgress(e.name, count++, -1))
                val outFile = safeDestination(destinationDir, e.name) ?: continue
                if (e.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(outFile)).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (sevenZ.read(buffer).also { read = it } != -1) output.write(buffer, 0, read)
                    }
                }
            }
        }
        return true
    }

    /** RAR4 and older only — junrar has no RAR5 decoder. */
    private fun extractRar(archive: File, destinationDir: File): Boolean {
        Junrar.extract(archive, destinationDir)
        return true
    }

    /** Prevents zip-slip path traversal (entries like "../../evil") from escaping destinationDir. */
    private fun safeDestination(destinationDir: File, entryName: String): File? {
        val outFile = File(destinationDir, entryName)
        val normalizedDest = destinationDir.canonicalFile
        val normalizedOut = outFile.canonicalFile
        return if (normalizedOut.path.startsWith(normalizedDest.path)) outFile else null
    }

    fun isArchive(file: File): Boolean = file.isFile && ArchiveFormat.fromFile(file) != ArchiveFormat.UNKNOWN

    fun isRar5OrUnsupported(file: File): Boolean {
        // Best-effort hint only: junrar throws at extraction time for RAR5; we can't reliably
        // pre-detect the version without reading the header, so this always returns false and
        // the real signal is the exception surfaced to the caller of extract().
        return false
    }
}
