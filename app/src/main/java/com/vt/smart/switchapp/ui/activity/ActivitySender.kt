package com.vt.smart.switchapp.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.ads.ManageAdsUnit
import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.connectivity.TransferManager
import com.vt.smart.switchapp.databinding.ActivitySenderBinding
import com.vt.smart.switchapp.ui.models.TransferData
import com.vt.smart.switchapp.utils.utilities.MyConstant
import kotlinx.coroutines.launch

/**
 * Sending screen. The actual transfer runs in [TransferManager] (process level
 * worker thread, no foreground service); this screen only starts it and shows
 * progress. The screen is kept on while sending so the system does not put
 * the app to sleep in the middle of a transfer.
 */
class ActivitySender : AppCompatActivity() {
    private val TAG = "ActivitySender"
    private lateinit var binding: ActivitySenderBinding
    private var resultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySenderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        resultShown = savedInstanceState?.getBoolean(KEY_RESULT_SHOWN, false) ?: false

        try {
            ManageAdsUnit.getInstance().showAdMobBanner(this, this, binding.bannerLayout)
        } catch (e: Exception) {
            Log.e(TAG, "Banner: ${e.message}")
        }
        binding.ripple.startRippleAnimation()
        binding.tvPercentage.text = "0%"

        binding.btnDone.setOnClickListener {
            TransferManager.resetIfFinished()
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (TransferManager.state.value is TransferManager.State.Running) {
                    confirmCancel()
                } else {
                    TransferManager.resetIfFinished()
                    finish()
                }
            }
        })

        if (savedInstanceState == null && !TransferManager.isRunning) {
            TransferManager.resetIfFinished()
            startTransfer()
        }
        observeTransfer()
    }

    private fun startTransfer() {
        val files = loadFilesToSend()
        if (files.isEmpty()) {
            AppUtils.presentToast(this, "No files to send")
            MySocketHandler.clearSocket()
            finish()
            return
        }
        if (MySocketHandler.getSocket() == null) {
            AppUtils.presentToast(this, "Not connected to receiver")
            finish()
            return
        }
        if (TransferManager.startSending(this, files)) {
            AppUtils.presentToast(this, "Keep this screen open until the transfer finishes")
        }
    }

    private fun loadFilesToSend(): List<TransferData> {
        return try {
            val json = getSharedPreferences(MyConstant.prefName, MODE_PRIVATE)
                .getString("dataList", null)
            if (json.isNullOrEmpty()) return emptyList()
            val list = Gson().fromJson(json, Array<TransferData>::class.java) ?: return emptyList()
            list.filter { !it.path.isNullOrBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot read selected files", e)
            emptyList()
        }
    }

    private fun observeTransfer() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TransferManager.state.collect { render(it) }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun render(state: TransferManager.State) {
        when (state) {
            is TransferManager.State.Running -> {
                binding.tvPercentage.text = "${state.percent}%"
            }

            is TransferManager.State.Completed -> {
                if (!state.isSender || resultShown) return
                resultShown = true
                binding.ripple.stopRippleAnimation()
                binding.tvPercentage.text = "100%"
                binding.btnDone.visibility = View.VISIBLE
                if (state.failedFiles == 0) {
                    AppUtils.presentToast(this, "All files sent successfully")
                } else {
                    AppUtils.presentToast(
                        this,
                        "Sent ${state.filesDone} of ${state.totalFiles} files"
                    )
                }
            }

            is TransferManager.State.Failed -> {
                if (!state.isSender || resultShown) return
                resultShown = true
                binding.ripple.stopRippleAnimation()
                binding.btnDone.visibility = View.VISIBLE
                AppUtils.presentToast(this, "Transfer failed: ${state.message}")
            }

            TransferManager.State.Idle -> Unit
        }
    }

    private fun confirmCancel() {
        MaterialAlertDialogBuilder(this)
            .setMessage("Stop sending files?")
            .setPositiveButton("Stop") { _, _ ->
                TransferManager.cancel()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_RESULT_SHOWN, resultShown)
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    companion object {
        private const val KEY_RESULT_SHOWN = "result_shown"
    }
}
