package com.vt.smart.switchapp.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.databinding.ActivitySenderBinding
import com.vt.smart.switchapp.ui.models.TransferData
import com.vt.smart.switchapp.utils.utilities.MyConstant
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.ObjectOutputStream
import java.net.Socket

class Sender : AppCompatActivity() {

    private lateinit var binding: ActivitySenderBinding
    private val TAG = "Sender"
    private val fileList = ArrayList<TransferData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySenderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ManageAds.showAdMobBanner(this, this, binding.bannerLayout)

        binding.ripple.startRippleAnimation()
        binding.tvPercentage.text = "0%"

        loadFilesAndSend()

        binding.btnDone.setOnClickListener { finish() }
    }

    private fun loadFilesAndSend() {
        try {
            val pref = getSharedPreferences(MyConstant.prefName, MODE_PRIVATE)
            val json = pref.getString("dataList", null)

            if (json.isNullOrEmpty()) {
                toast("No files to send")
                finish()
                return
            }

            val list = Gson().fromJson(json, Array<TransferData>::class.java)
            if (list.isNullOrEmpty()) {
                toast("No files to send")
                finish()
                return
            }
            fileList.addAll(list.filter { it.path.isNotBlank() && File(it.path).exists() })

            if (fileList.isEmpty()) {
                toast("No files found to send")
                finish()
                return
            }

//            TransferForegroundService.start(applicationContext)
            TransferThread().start()
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing files", e)
            toast("Something went wrong")
            finish()
        }
    }

    inner class TransferThread : Thread() {
        override fun run() {
            var socket: Socket? = null
            var oos: ObjectOutputStream? = null
            var sentFiles = 0

            try {
                socket = MySocketHandler.getSocket() ?: throw Exception("Socket is null")
                socket.soTimeout = MyConstant.TRANSFER_SO_TIMEOUT_MS
                oos = ObjectOutputStream(BufferedOutputStream(socket.getOutputStream()))

                val totalBytes = fileList.sumOf { File(it.path).length() }
                if (totalBytes <= 0L) {
                    throw Exception("Nothing to send")
                }

                oos.writeInt(fileList.size)
                oos.writeLong(totalBytes)
                oos.flush()

                var sentBytes = 0L
                var lastPercent = -1

                for (item in fileList) {
                    val file = File(item.path)
                    val length = if (file.exists()) file.length() else 0L

                    oos.writeObject(item)
                    oos.writeLong(length)
                    if (length <= 0L) {
                        oos.flush()
                        oos.reset()
                        sentFiles++
                        continue
                    }

                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            oos.write(buffer, 0, bytesRead)
                            sentBytes += bytesRead

                            val percent = ((sentBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                runOnUiThread {
                                    if (!isFinishing && !isDestroyed) {
                                        binding.tvPercentage.text = "$percent%"
                                    }
                                }
                            }
                        }
                    }

                    oos.flush()
                    oos.reset()
                    sentFiles++
                }

                val allSent = sentFiles == fileList.size
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (allSent) {
                        binding.tvPercentage.text = "100%"
                        toast("All files sent successfully")
                    } else {
                        toast("Sent $sentFiles of ${fileList.size} files")
                    }
                    binding.btnDone.visibility = View.VISIBLE
                    binding.ripple.stopRippleAnimation()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transfer failed", e)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    binding.ripple.stopRippleAnimation()
                    toast("Transfer failed: ${e.message}")
                    finish()
                }
            } finally {
                try {
                    oos?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Closing error", e)
                }
                MySocketHandler.clearSocket()
//                TransferForegroundService.stop(applicationContext)
            }
        }
    }

    override fun onDestroy() {
//        TransferForegroundService.stop(applicationContext)
        super.onDestroy()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
