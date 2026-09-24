package com.vt.smart.switchapp.connectivity

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaScannerConnection
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.vt.smart.switchapp.ui.models.TransferData
import com.vt.smart.switchapp.utils.utilities.MyConstant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Sends / receives files over the socket prepared by MyServer / MyClient.
 *
 * Runs on its own worker thread that belongs to the process (not to an
 * Activity), so the transfer survives activity re-creation. No foreground
 * service is used: a partial WakeLock + WifiLock keep CPU and Wi-Fi awake
 * while the transfer runs, and the transfer screens keep the display on.
 */
object TransferManager {

    private const val TAG = "TransferManager"
    private const val WAKE_TAG = "SmartSwitch:Transfer"
    private const val MAX_LOCK_MS = 3 * 60 * 60 * 1000L // safety cap: 3 hours
    private const val UI_UPDATE_INTERVAL_MS = 150L

    sealed class State {
        object Idle : State()

        data class Running(
            val isSender: Boolean,
            val fileIndex: Int,
            val totalFiles: Int,
            val currentFileName: String,
            val bytesDone: Long,
            val totalBytes: Long
        ) : State() {
            val percent: Int
                get() = if (totalBytes <= 0L) 0
                else ((bytesDone * 100) / totalBytes).toInt().coerceIn(0, 100)
        }

        data class Completed(
            val isSender: Boolean,
            val filesDone: Int,
            val totalFiles: Int,
            val failedFiles: Int
        ) : State()

        data class Failed(
            val isSender: Boolean,
            val message: String,
            val filesDone: Int,
            val totalFiles: Int
        ) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var cancelled = false

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    val isRunning: Boolean
        get() = worker?.isAlive == true

    /** Reset a finished (Completed / Failed) state so a new transfer can start. */
    @Synchronized
    fun resetIfFinished() {
        if (!isRunning) {
            _state.value = State.Idle
        }
    }

    @Synchronized
    fun startSending(context: Context, files: List<TransferData>): Boolean {
        if (isRunning) return false
        val appContext = context.applicationContext
        cancelled = false
        _state.value = State.Running(true, 0, files.size, "", 0L, 0L)
        val thread = Thread({ runSender(appContext, files) }, "SmartSwitch-Sender")
        worker = thread
        thread.start()
        return true
    }

    @Synchronized
    fun startReceiving(context: Context): Boolean {
        if (isRunning) return false
        val appContext = context.applicationContext
        cancelled = false
        _state.value = State.Running(false, 0, 0, "", 0L, 0L)
        val thread = Thread({ runReceiver(appContext) }, "SmartSwitch-Receiver")
        worker = thread
        thread.start()
        return true
    }

    /** Cancel a running transfer. Closing the socket unblocks any pending read / write. */
    fun cancel() {
        cancelled = true
        MySocketHandler.clearSocket()
    }

    // ------------------------------------------------------------------ sender

    private fun runSender(context: Context, requested: List<TransferData>) {
        acquireLocks(context)
        var sentFiles = 0
        var failedFiles = 0
        val files = requested.filter { !it.path.isNullOrBlank() }
        try {
            val socket = MySocketHandler.getSocket() ?: throw IOException("Not connected")
            prepareSocket(socket)

            val lengths = files.map { item ->
                val f = File(item.path)
                if (f.isFile && f.canRead()) f.length() else TransferProtocol.SKIPPED
            }
            val totalBytes = lengths.filter { it > 0 }.sum()

            val out = DataOutputStream(
                BufferedOutputStream(socket.getOutputStream(), TransferProtocol.BUFFER_SIZE)
            )
            out.writeInt(TransferProtocol.MAGIC)
            out.writeInt(TransferProtocol.VERSION)
            out.writeInt(files.size)
            out.writeLong(totalBytes)
            out.flush()

            val buffer = ByteArray(TransferProtocol.BUFFER_SIZE)
            var bytesDone = 0L
            var lastUi = 0L

            files.forEachIndexed { index, item ->
                checkCancelled()
                val declared = lengths[index]
                val displayName = item.name.orEmpty()
                out.writeUTF(displayName)
                out.writeUTF(item.type.orEmpty())

                var input: InputStream? = null
                if (declared >= 0) {
                    input = try {
                        BufferedInputStream(FileInputStream(File(item.path)), TransferProtocol.BUFFER_SIZE)
                    } catch (e: Exception) {
                        Log.e(TAG, "Cannot open ${item.path}: ${e.message}")
                        null
                    }
                }

                if (input == null) {
                    // could not read the file - tell the receiver to skip it
                    out.writeLong(TransferProtocol.SKIPPED)
                    if (declared > 0) bytesDone += declared // keep progress consistent
                    failedFiles++
                    publishRunning(true, index, files.size, displayName, bytesDone, totalBytes)
                    return@forEachIndexed
                }

                out.writeLong(declared)
                var remaining = declared
                var readError = false
                val stream: InputStream = input
                try {
                    while (remaining > 0) {
                        checkCancelled()
                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                        val read = if (readError) {
                            -1
                        } else {
                            try {
                                stream.read(buffer, 0, toRead)
                            } catch (e: IOException) {
                                -1
                            }
                        }
                        if (read < 0) {
                            // file shrank / became unreadable: pad with zeros to keep the stream in sync
                            if (!readError) {
                                readError = true
                                java.util.Arrays.fill(buffer, 0.toByte())
                            }
                            out.write(buffer, 0, toRead)
                            remaining -= toRead
                            bytesDone += toRead
                            continue
                        }
                        out.write(buffer, 0, read)
                        remaining -= read
                        bytesDone += read

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastUi >= UI_UPDATE_INTERVAL_MS) {
                            lastUi = now
                            publishRunning(true, index, files.size, displayName, bytesDone, totalBytes)
                        }
                    }
                } finally {
                    try {
                        stream.close()
                    } catch (_: IOException) {
                    }
                }
                // tell the receiver whether this file's data is valid
                out.writeByte(if (readError) TransferProtocol.STATUS_BROKEN else TransferProtocol.STATUS_OK)
                if (readError) failedFiles++ else sentFiles++
                publishRunning(true, index, files.size, displayName, bytesDone, totalBytes)
            }
            out.flush()

            // wait for the receiver to confirm everything was written to disk
            socket.soTimeout = TransferProtocol.ACK_TIMEOUT_MS
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            val ack = input.readInt()
            input.readInt() // files saved on the other side (informational)
            if (ack != TransferProtocol.ACK_OK) throw IOException("Receiver did not confirm")

            _state.value = State.Completed(true, sentFiles, files.size, failedFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Send failed", e)
            _state.value = State.Failed(true, errorMessage(e), sentFiles, files.size)
        } finally {
            finishTransfer()
        }
    }

    // ---------------------------------------------------------------- receiver

    private fun runReceiver(context: Context) {
        acquireLocks(context)
        var savedFiles = 0
        var failedFiles = 0
        var totalFiles = 0
        val savedPaths = ArrayList<String>()
        try {
            val socket = MySocketHandler.getSocket() ?: throw IOException("Not connected")
            prepareSocket(socket)
            val input = DataInputStream(
                BufferedInputStream(socket.getInputStream(), TransferProtocol.BUFFER_SIZE)
            )

            // the sender may take a moment (ads / file checks) before sending the header
            socket.soTimeout = TransferProtocol.ACK_TIMEOUT_MS
            if (input.readInt() != TransferProtocol.MAGIC) throw IOException("Incompatible sender app")
            val version = input.readInt()
            if (version != TransferProtocol.VERSION) throw IOException("Please update the app on both phones")
            totalFiles = input.readInt()
            val totalBytes = input.readLong()
            socket.soTimeout = TransferProtocol.READ_TIMEOUT_MS

            val directory = TransferProtocol.receiveDirectory(context)
            val buffer = ByteArray(TransferProtocol.BUFFER_SIZE)
            var bytesDone = 0L
            var lastUi = 0L
            publishRunning(false, 0, totalFiles, "", 0L, totalBytes)

            for (index in 0 until totalFiles) {
                checkCancelled()
                val name = input.readUTF()
                val type = input.readUTF()
                val length = input.readLong()
                if (length < 0) {
                    failedFiles++
                    publishRunning(false, index, totalFiles, name, bytesDone, totalBytes)
                    continue
                }

                val target = TransferProtocol.uniqueFile(directory, TransferProtocol.buildFileName(name, type))
                var output: FileOutputStream? = try {
                    FileOutputStream(target)
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot create ${target.absolutePath}: ${e.message}")
                    null
                }

                var remaining = length
                var writeError = output == null
                try {
                    val bos = output?.let { BufferedOutputStream(it, TransferProtocol.BUFFER_SIZE) }
                    while (remaining > 0) {
                        checkCancelled()
                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                        val read = input.read(buffer, 0, toRead)
                        if (read < 0) throw EOFException("Connection closed by sender")
                        if (bos != null && !writeError) {
                            try {
                                bos.write(buffer, 0, read)
                            } catch (e: IOException) {
                                // disk full etc. - keep draining the socket so later files still arrive
                                Log.e(TAG, "Write failed for ${target.name}: ${e.message}")
                                writeError = true
                            }
                        }
                        remaining -= read
                        bytesDone += read

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastUi >= UI_UPDATE_INTERVAL_MS) {
                            lastUi = now
                            publishRunning(false, index, totalFiles, name, bytesDone, totalBytes)
                        }
                    }
                    if (bos != null && !writeError) {
                        try {
                            bos.flush()
                        } catch (e: IOException) {
                            writeError = true
                        }
                    }
                    // status byte is always read to stay in sync with the sender
                    if (input.readByte().toInt() != TransferProtocol.STATUS_OK) {
                        writeError = true
                    }
                } finally {
                    try {
                        output?.close()
                    } catch (_: IOException) {
                    }
                    output = null
                    if (remaining > 0) {
                        // interrupted in the middle of this file: don't leave a broken file behind
                        try {
                            target.delete()
                        } catch (_: Exception) {
                        }
                    }
                }

                if (writeError) {
                    failedFiles++
                    try {
                        target.delete()
                    } catch (_: Exception) {
                    }
                } else {
                    savedFiles++
                    savedPaths.add(target.absolutePath)
                }
                publishRunning(false, index, totalFiles, name, bytesDone, totalBytes)
            }

            // confirm to the sender
            val out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            out.writeInt(TransferProtocol.ACK_OK)
            out.writeInt(savedFiles)
            out.flush()
            // give the ACK time to leave before the socket / hotspot is torn down:
            // the sender closes its side right after reading it
            try {
                socket.soTimeout = 5_000
                input.read()
            } catch (_: Exception) {
            }

            _state.value = State.Completed(false, savedFiles, totalFiles, failedFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Receive failed", e)
            _state.value = State.Failed(false, errorMessage(e), savedFiles, totalFiles)
        } finally {
            scanFiles(context, savedPaths)
            finishTransfer()
        }
    }

    // ----------------------------------------------------------------- helpers

    private fun prepareSocket(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.sendBufferSize = TransferProtocol.SOCKET_BUFFER_SIZE
            socket.receiveBufferSize = TransferProtocol.SOCKET_BUFFER_SIZE
            socket.soTimeout = TransferProtocol.READ_TIMEOUT_MS
        } catch (e: Exception) {
            Log.w(TAG, "Socket options: ${e.message}")
        }
    }

    private fun checkCancelled() {
        if (cancelled) throw IOException("Transfer cancelled")
    }

    private fun publishRunning(
        isSender: Boolean,
        index: Int,
        totalFiles: Int,
        name: String,
        bytesDone: Long,
        totalBytes: Long
    ) {
        _state.value = State.Running(isSender, index, totalFiles, name, bytesDone, totalBytes)
    }

    private fun errorMessage(e: Exception): String {
        return when {
            cancelled -> "Transfer cancelled"
            e is SocketTimeoutException -> "Connection timed out"
            e is EOFException -> "Connection closed by other device"
            else -> e.message ?: "Connection interrupted"
        }
    }

    private fun scanFiles(context: Context, paths: List<String>) {
        if (paths.isEmpty()) return
        try {
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
        } catch (e: Exception) {
            Log.w(TAG, "Media scan failed: ${e.message}")
        }
    }

    private fun finishTransfer() {
        MySocketHandler.clearSocket()
        MyServer.stopListening()
        HotspotNetworkHolder.release()
        try {
            // sender side: turn the local-only hotspot off once we are done
            MyConstant.reservationOfHotspot?.close()
        } catch (_: Exception) {
        }
        MyConstant.reservationOfHotspot = null
        releaseLocks()
        synchronized(this) {
            worker = null
        }
    }

    @SuppressLint("WakelockTimeout")
    @Suppress("DEPRECATION")
    @Synchronized
    private fun acquireLocks(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG).apply {
                setReferenceCounted(false)
                acquire(MAX_LOCK_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock: ${e.message}")
        }
        try {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wm.createWifiLock(mode, WAKE_TAG).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "WifiLock: ${e.message}")
        }
    }

    @Synchronized
    private fun releaseLocks() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        try {
            wifiLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        wakeLock = null
        wifiLock = null
    }
}
