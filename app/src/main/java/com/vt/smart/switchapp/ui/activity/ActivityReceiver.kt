package com.vt.smart.switchapp.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.ReceivedFilesActivity
import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.databinding.ActivityReceiverBinding
import com.vt.smart.switchapp.ui.models.TransferData
import kotlinx.coroutines.*
import java.io.*

class ActivityReceiver : AppCompatActivity() {
    private val TAG = javaClass.canonicalName
    private lateinit var binding: ActivityReceiverBinding
    private var uniqueID: Long = 0L
    private var receivedFiles = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        uniqueID = System.currentTimeMillis()

        binding.ripple.startRippleAnimation()
        binding.btnDone.setOnClickListener {
            startActivity(Intent(this@ActivityReceiver, ReceivedFilesActivity::class.java))
            finish()
        }

        // Start receiving data using a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            receiveData()
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun receiveData() {
        val socket = MySocketHandler.getSocket()
        if (socket != null) {
            var ois: ObjectInputStream? = null
            var dis: DataInputStream? = null
            try {
                ois = ObjectInputStream(BufferedInputStream(socket.getInputStream()))
                dis = DataInputStream(ois)
                val filesCount = dis.readInt()

                for (i in 0 until filesCount) {
                    try {
                        receivedFiles += 1
                        val fileToReceive = ois.readObject() as TransferData
                        val buffer = ByteArray(4096)
                        val folder = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "/Smart Switch"
                        )

                        if (!folder.exists()) {
                            folder.mkdirs()
                        }

                        val fileName = when (fileToReceive.type) {
                            "Apps" -> fileToReceive.name + ".apk"
                            "contacts" -> fileToReceive.name + ".vcf"
                            else -> fileToReceive.name
                        }
                        val file = File(folder, "/$fileName")

                        FileOutputStream(file).use { fos ->
                            BufferedOutputStream(fos).use { bos ->
                                var bytesRead: Int
                                while (ois.read(buffer).also { bytesRead = it } != -1) {
                                    bos.write(buffer, 0, bytesRead)
                                }
                                bos.flush()
                            }
                        }

                        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))

                        // Update UI on the main thread
                        withContext(Dispatchers.Main) {
                            val percentage = ((i + 1).toFloat() / filesCount.toFloat()) * 100
                            binding.tvPercentage.text = "${percentage.toInt()}%"
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error receiving file: $e")
                    }
                }

                // Final UI update after all files are received
                withContext(Dispatchers.Main) {
                    binding.tvPercentage.text = "100%"
                    if (receivedFiles == filesCount) {
                        AppUtils.presentToast(this@ActivityReceiver, "All files received successfully")
                    } else {
                        AppUtils.presentToast(this@ActivityReceiver, "Partial files received successfully")
                    }
                    binding.btnDone.visibility = View.VISIBLE
                }

            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    AppUtils.presentToast(this@ActivityReceiver, "Connection interrupted")
                    finish()
                }
            } finally {
                try {
                    dis?.close()
                    ois?.close()
                    socket.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing streams/socket: $e")
                }
            }
        }
    }
}
