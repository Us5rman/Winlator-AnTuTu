package com.winlator.star.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ExeIconExtractor {
    private const val TAG = "ExeIconExtractor"

    @JvmStatic
    fun extract(exeFile: File): Bitmap? {
        if (!exeFile.exists() || !exeFile.canRead()) return null
        return try {
            RandomAccessFile(exeFile, "r").use { raf ->
                val peOffset = readPeOffset(raf) ?: return null
                val iconGroupRva = findResourceRva(raf, peOffset, 14) ?: return null // RT_GROUP_ICON = 14
                val iconRva = findBestIconRva(raf, peOffset, iconGroupRva) ?: return null
                val fileOffset = rvaToFileOffset(raf, peOffset, iconRva) ?: return null
                
                raf.seek(fileOffset)
                val header = ByteArray(8)
                raf.readFully(header)
                
                val isPng = header[0] == 0x89.toByte() && header[1] == 'P'.toByte() &&
                            header[2] == 'N'.toByte() && header[3] == 'G'.toByte()
                
                raf.seek(fileOffset)
                if (isPng) {
                    val size = readResourceSize(raf, peOffset, iconRva) ?: 65536
                    val pngBytes = ByteArray(size)
                    raf.readFully(pngBytes)
                    BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
                } else {
                    // DIB / BMP icon header
                    val dibHeaderSize = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val width = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val height = width // ICO heights are doubled for XOR/AND masks
                    
                    val imageSize = readResourceSize(raf, peOffset, iconRva) ?: return null
                    val iconData = ByteArray(imageSize)
                    raf.seek(fileOffset)
                    raf.readFully(iconData)

                    val bmpHeader = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN)
                    bmpHeader.put('B'.toByte()).put('M'.toByte())
                    bmpHeader.putInt(14 + imageSize)
                    bmpHeader.putShort(0).putShort(0)
                    bmpHeader.putInt(14 + dibHeaderSize)

                    val fullBmp = bmpHeader.array() + iconData
                    BitmapFactory.decodeByteArray(fullBmp, 0, fullBmp.size)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract PE icon from ${exeFile.name}", e)
            null
        }
    }

    private fun readPeOffset(raf: RandomAccessFile): Long? {
        if (raf.length() < 0x40) return null
        raf.seek(0x3C)
        val buf = ByteArray(4)
        raf.readFully(buf)
        val offset = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
        raf.seek(offset)
        raf.readFully(buf)
        return if (buf[0] == 'P'.toByte() && buf[1] == 'E'.toByte()) offset else null
    }

    private fun findResourceRva(raf: RandomAccessFile, peOffset: Long, resType: Int): Long? {
        raf.seek(peOffset + 20)
        val optHeaderSizeBuf = ByteArray(2)
        raf.readFully(optHeaderSizeBuf)
        val optHeaderSize = ByteBuffer.wrap(optHeaderSizeBuf).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

        val magicBuf = ByteArray(2)
        raf.seek(peOffset + 24)
        raf.readFully(magicBuf)
        val is64Bit = ByteBuffer.wrap(magicBuf).order(ByteOrder.LITTLE_ENDIAN).short == 0x20B.toShort()

        val rvaOffset = peOffset + 24 + if (is64Bit) 112 else 96
        raf.seek(rvaOffset + 2 * 8) // Resource Table entry
        val buf = ByteArray(8)
        raf.readFully(buf)
        val resRva = ByteBuffer.wrap(buf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
        return if (resRva != 0L) resRva else null
    }

    private fun findBestIconRva(raf: RandomAccessFile, peOffset: Long, groupRva: Long): Long? {
        val groupOffset = rvaToFileOffset(raf, peOffset, groupRva) ?: return null
        raf.seek(groupOffset + 12)
        val countBuf = ByteArray(2)
        raf.readFully(countBuf)
        val count = ByteBuffer.wrap(countBuf).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        if (count == 0) return null

        raf.seek(groupOffset + 14)
        val entry = ByteArray(14)
        raf.readFully(entry)
        val iconId = ByteBuffer.wrap(entry, 12, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

        return findIconRvaById(raf, peOffset, groupRva, iconId)
    }

    private fun findIconRvaById(raf: RandomAccessFile, peOffset: Long, resRva: Long, iconId: Int): Long? {
        val rootOffset = rvaToFileOffset(raf, peOffset, resRva) ?: return null
        raf.seek(rootOffset + 12)
        val buf = ByteArray(4)
        raf.readFully(buf)
        val namedEntries = ByteBuffer.wrap(buf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        val idEntries = ByteBuffer.wrap(buf, 2, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        val totalEntries = namedEntries + idEntries

        for (i in 0 until totalEntries) {
            raf.seek(rootOffset + 16 + i * 8)
            val entryBuf = ByteArray(8)
            raf.readFully(entryBuf)
            val type = ByteBuffer.wrap(entryBuf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val offsetToData = ByteBuffer.wrap(entryBuf, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int

            if (type == 3) { // RT_ICON = 3
                val subDirOffset = rootOffset + (offsetToData and 0x7FFFFFFF)
                return parseIconSubDir(raf, peOffset, rootOffset, subDirOffset, iconId)
            }
        }
        return null
    }

    private fun parseIconSubDir(
        raf: RandomAccessFile,
        peOffset: Long,
        rootOffset: Long,
        subDirOffset: Long,
        targetId: Int
    ): Long? {
        raf.seek(subDirOffset + 12)
        val buf = ByteArray(4)
        raf.readFully(buf)
        val count = (ByteBuffer.wrap(buf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF) +
                    (ByteBuffer.wrap(buf, 2, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF)

        for (i in 0 until count) {
            raf.seek(subDirOffset + 16 + i * 8)
            val entryBuf = ByteArray(8)
            raf.readFully(entryBuf)
            val id = ByteBuffer.wrap(entryBuf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val dataOffset = ByteBuffer.wrap(entryBuf, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int

            if (id == targetId) {
                val leafOffset = rootOffset + (dataOffset and 0x7FFFFFFF) + 16
                raf.seek(leafOffset)
                val leafBuf = ByteArray(8)
                raf.readFully(leafBuf)
                val dataEntryOffset = rootOffset + (ByteBuffer.wrap(leafBuf, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int and 0x7FFFFFFF)
                
                raf.seek(dataEntryOffset)
                val finalBuf = ByteArray(4)
                raf.readFully(finalBuf)
                return ByteBuffer.wrap(finalBuf).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            }
        }
        return null
    }

    private fun readResourceSize(raf: RandomAccessFile, peOffset: Long, rva: Long): Int? {
        val fileOffset = rvaToFileOffset(raf, peOffset, rva) ?: return null
        return try {
            raf.seek(fileOffset + 4)
            val buf = ByteArray(4)
            raf.readFully(buf)
            ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
        } catch (_: Exception) {
            null
        }
    }

    private fun rvaToFileOffset(raf: RandomAccessFile, peOffset: Long, rva: Long): Long? {
        raf.seek(peOffset + 6)
        val countBuf = ByteArray(2)
        raf.readFully(countBuf)
        val numSections = ByteBuffer.wrap(countBuf).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

        raf.seek(peOffset + 20)
        val optSizeBuf = ByteArray(2)
        raf.readFully(optSizeBuf)
        val optHeaderSize = ByteBuffer.wrap(optSizeBuf).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

        val sectionHeaderStart = peOffset + 24 + optHeaderSize

        for (i in 0 until numSections) {
            raf.seek(sectionHeaderStart + i * 40 + 8)
            val secBuf = ByteArray(12)
            raf.readFully(secBuf)
            val virtualSize = ByteBuffer.wrap(secBuf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            val virtualAddr = ByteBuffer.wrap(secBuf, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            val rawDataOffset = ByteBuffer.wrap(secBuf, 8, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()

            if (rva >= virtualAddr && rva < virtualAddr + virtualSize) {
                return rawDataOffset + (rva - virtualAddr)
            }
        }
        return null
    }
}
