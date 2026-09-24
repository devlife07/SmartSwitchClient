package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.vt.smart.switchapp.connectivity.MyClient
import com.vt.smart.switchapp.databinding.ActivityJoinHotSpotBinding
import com.vt.smart.switchapp.ui.interfaces.ConnectionInterface
import com.vt.smart.switchapp.ui.models.HotSpot
import com.vt.smart.switchapp.utils.utilities.MyConstant
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import org.json.JSONObject
import java.net.InetAddress


class JoinHotSpot : AppCompatActivity(), ConnectionInterface {
    private lateinit var binding: ActivityJoinHotSpotBinding
    private lateinit var wifiManager: WifiManager
    private var progressDialog: AlertDialog? = null
    private lateinit var codeScanner: CodeScanner
    private val CAMERA_REQ = 123
    private val mainHandler = Handler(Looper.getMainLooper())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var dhcpRetries = 0
    private var clientStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinHotSpotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        setupScanner()

        // Permission Check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQ)
        }
    }

    private fun setupScanner() {
        codeScanner = CodeScanner(this, binding.scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                handleScannedData(it.text)
            }
        }

        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Log.e("Scanner", "Camera error: ${it.message}")
                Toast.makeText(this, "Camera error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleScannedData(qrData: String) {
        dhcpRetries = 0
        clientStarted = false
        showProgressDialog("Connecting to Hotspot...")
        try {
            val jsonObject = JSONObject(qrData)
            val model = Gson().fromJson(jsonObject.toString(), HotSpot::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectToWifiForAndroidQ(model)
            } else {
                connectToWifiLegacy(model)
            }
        } catch (e: Exception) {
            dismissProgressDialog()
            Log.e("QR_ERROR", "Parsing error", e)
            Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show()
            codeScanner.startPreview() // Restart scanner if invalid
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectToWifiForAndroidQ(model: HotSpot) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(model.name)
            .setWpa2Passphrase(model.password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Critical: Bind the app to this specific Wi-Fi network
                connectivityManager.bindProcessToNetwork(network)

                // Wait slightly for DHCP to assign IP
                mainHandler.postDelayed({
                    dismissProgressDialog()
                    initiateClient()
                }, 2000)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                // NetworkCallback methods run on a binder thread, not the main thread
                runOnUiThread {
                    dismissProgressDialog()
                    Toast.makeText(this@JoinHotSpot, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        networkCallback = callback
        connectivityManager.requestNetwork(request, callback)
    }

    private fun connectToWifiLegacy(model: HotSpot) {
        val wifiConfig = WifiConfiguration().apply {
            SSID = "\"${model.name}\""
            preSharedKey = "\"${model.password}\""
        }
        val netId = wifiManager.addNetwork(wifiConfig)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()

        Handler(Looper.getMainLooper()).postDelayed({
            dismissProgressDialog()
            initiateClient()
        }, 3000)
    }

    private fun initiateClient() {
        val dhcp = wifiManager.dhcpInfo.serverAddress
        if (dhcp == 0) {
            if (dhcpRetries >= MyConstant.DHCP_MAX_RETRIES) {
                dismissProgressDialog()
                Toast.makeText(this, "Could not get hotspot IP. Scan again.", Toast.LENGTH_SHORT).show()
                if (::codeScanner.isInitialized) {
                    codeScanner.startPreview()
                }
                return
            }
            dhcpRetries++
            Log.d("Client", "IP not ready, retrying $dhcpRetries/${MyConstant.DHCP_MAX_RETRIES}")
            Handler(Looper.getMainLooper()).postDelayed({ initiateClient() }, MyConstant.DHCP_RETRY_DELAY_MS)
            return
        }

        if (clientStarted) return
        clientStarted = true
        val address = Formatter.formatIpAddress(dhcp)
        try {
            Log.e("MyClientServer", "Connecting to IP: $address")
            val groupOwnerAddress = InetAddress.getByName(address)
            MyClient(groupOwnerAddress, this@JoinHotSpot).start()
        } catch (e: Exception) {
            clientStarted = false
            Log.e("MyClientServer", "Socket error: ${e.message}")
            onConnectionFailed(e.message ?: "Invalid hotspot IP")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQ && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            codeScanner.startPreview()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionSuccessful() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            startActivity(Intent(this, ActivityReceiver::class.java))
            finish()
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            clientStarted = false
            if (isFinishing || isDestroyed) return@runOnUiThread
            dismissProgressDialog()
            Toast.makeText(this, "Connection failed: $reason", Toast.LENGTH_SHORT).show()
            if (::codeScanner.isInitialized) {
                try {
                    codeScanner.startPreview()
                } catch (e: Exception) {
                    Log.e("Scanner", "Preview start failed", e)
                }
            }
        }
    }

    private fun showProgressDialog(message: String) {
        dismissProgressDialog()
        progressDialog = MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        // Delayed callbacks (Handler.postDelayed, NetworkCallback) can fire
        // after the activity's window is already torn down; dismissing then
        // throws IllegalArgumentException from WindowManagerGlobal.
        if (isFinishing || isDestroyed) {
            progressDialog = null
            return
        }
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
        progressDialog = null
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        networkCallback?.let {
            try {
                val connectivityManager =
                    getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: IllegalArgumentException) {
                // already unregistered
            }
        }
        networkCallback = null
        progressDialog?.dismiss()
        progressDialog = null
        super.onDestroy()
    }
}