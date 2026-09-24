package com.vt.smart.switchapp.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.ReceivedFilesActivity
import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.connectivity.TransferManager
import com.vt.smart.switchapp.databinding.ActivityReceiverBinding
import kotlinx.coroutines.launch

/**
 * Receiving screen. Reading from the socket and writing to disk is done by
 * [TransferManager] on a process level worker thread (no foreground service).
 */
class ActivityReceiver : AppCompatActivity() {
    private lateinit var binding: ActivityReceiverBinding
    private var resultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        resultShown = savedInstanceState?.getBoolean(KEY_RESULT_SHOWN, false) ?: false

        binding.ripple.startRippleAnimation()
        binding.tvPercentage.text = "0%"

        binding.btnDone.setOnClickListener {
            TransferManager.resetIfFinished()
            startActivity(Intent(this@ActivityReceiver, ReceivedFilesActivity::class.java))
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
            if (MySocketHandler.getSocket() == null) {
                AppUtils.presentToast(this, "Not connected to sender")
                finish()
                return
            }
            if (TransferManager.startReceiving(this)) {
                AppUtils.presentToast(this, "Keep this screen open until the transfer finishes")
            }
        }
        observeTransfer()
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
                if (state.isSender || resultShown) return
                resultShown = true
                binding.ripple.stopRippleAnimation()
                binding.tvPercentage.text = "100%"
                binding.btnDone.visibility = View.VISIBLE
                if (state.failedFiles == 0) {
                    AppUtils.presentToast(this, "All files received successfully")
                } else {
                    AppUtils.presentToast(
                        this,
                        "Received ${state.filesDone} of ${state.totalFiles} files"
                    )
                }
            }

            is TransferManager.State.Failed -> {
                if (state.isSender || resultShown) return
                resultShown = true
                binding.ripple.stopRippleAnimation()
                binding.btnDone.visibility = View.VISIBLE
                val msg = if (state.filesDone > 0) {
                    "Transfer stopped: ${state.message} (${state.filesDone} files saved)"
                } else {
                    "Transfer failed: ${state.message}"
                }
                AppUtils.presentToast(this, msg)
            }

            TransferManager.State.Idle -> Unit
        }
    }

    private fun confirmCancel() {
        MaterialAlertDialogBuilder(this)
            .setMessage("Stop receiving files?")
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
