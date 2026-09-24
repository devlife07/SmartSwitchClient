package com.vt.smart.switchapp.connectivity

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Wire format used between sender and receiver (plain DataOutputStream, no Java serialization):
 *
 *  HEADER : int MAGIC | int VERSION | int fileCount | long totalBytes
 *  FILE   : UTF name  | UTF type    | long length (-1 = skipped) | <length raw bytes> | byte status (0 = ok)
 *  (repeated fileCount times)
 *  ACK    : receiver -> sender : int ACK_OK | int filesSaved
 *
 * Every file is prefixed with its exact byte length, so the receiver always
 * knows where one file ends and the next one starts (the old ObjectStream
 * based code read "until -1" which merged / lost files).
 */
object TransferProtocol {
    const val MAGIC = 0x53535446 // "SSTF"
    const val VERSION = 2
    const val ACK_OK = 0x4F4B4F4B // "OKOK"
    const val SKIPPED = -1L
    const val STATUS_OK = 0
    const val STATUS_BROKEN = 1

    const val BUFFER_SIZE = 64 * 1024
    const val SOCKET_BUFFER_SIZE = 256 * 1024

    /** Read timeout while data is flowing. A stalled link fails instead of hanging forever. */
    const val READ_TIMEOUT_MS = 30_000

    /** Sender waits this long for the final receiver acknowledgement. */
    const val ACK_TIMEOUT_MS = 60_000

    /** Folder (inside public Downloads) where received files are stored. */
    const val RECEIVED_FOLDER = "Smart Switch"

    private const val TAG = "TransferProtocol"

    /** Public Downloads/Smart Switch, falling back to the app specific folder if not writable. */
    fun receiveDirectory(context: Context): File {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            RECEIVED_FOLDER
        )
        if (isWritableDir(publicDir)) return publicDir

        val privateDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            RECEIVED_FOLDER
        )
        privateDir.mkdirs()
        Log.w(TAG, "Public Downloads not writable, using ${privateDir.absolutePath}")
        return privateDir
    }

    /** All folders a received file may have been saved into (used by the history screen). */
    fun allReceiveDirectories(context: Context): List<File> {
        val list = ArrayList<File>()
        list.add(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                RECEIVED_FOLDER
            )
        )
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
            list.add(File(it, RECEIVED_FOLDER))
        }
        return list
    }

    private fun isWritableDir(dir: File): Boolean {
        return try {
            if (!dir.exists() && !dir.mkdirs()) return false
            val probe = File(dir, ".probe_${System.nanoTime()}")
            val ok = probe.createNewFile()
            probe.delete()
            ok
        } catch (e: Exception) {
            false
        }
    }

    /** Final on-disk name, adding the extension the old app expected for apps / contacts. */
    fun buildFileName(name: String, type: String): String {
        val clean = sanitize(name).ifBlank { "file_${System.currentTimeMillis()}" }
        return when {
            type.equals("Apps", ignoreCase = true) && !clean.endsWith(".apk", true) -> "$clean.apk"
            type.equals("Contacts", ignoreCase = true) && !clean.endsWith(".vcf", true) -> "$clean.vcf"
            else -> clean
        }
    }

    /** "photo.jpg" -> "photo (1).jpg" when a file with the same name already exists. */
    fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($index)$ext")
            index++
        }
        return candidate
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\u0000]"), "_").trim()
    }
}
