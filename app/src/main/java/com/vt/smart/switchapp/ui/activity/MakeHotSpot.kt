package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.connectivity.MyServer
import com.vt.smart.switchapp.databinding.ActivityMakeHotSpotBinding
import com.vt.smart.switchapp.ui.interfaces.ConnectionInterface
import com.vt.smart.switchapp.ui.models.HotSpot
import com.vt.smart.switchapp.utils.utilities.MyConstant.reservationOfHotspot
import com.vt.smart.switchapp.utils.utilities.Qrgenerator
import com.google.gson.Gson
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.lang.reflect.Method

class MakeHotSpot : AppCompatActivity(), ConnectionInterface {
    private val TAG = javaClass.simpleName
    private lateinit var binding: ActivityMakeHotSpotBinding
    private lateinit var wifiManager: WifiManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeHotSpotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ManageAds.showAdMobBanner(this,this,binding.bannerLayout)
        wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (reservationOfHotspot != null) {
            reservationOfHotspot!!.close()
        }

        turnOnHotspotForO()

    }

    private fun turnOnHotspot() {
        var method: Method = wifiManager.javaClass.getMethod("getWifiApConfiguration")
        val netConfig = method.invoke(wifiManager) as WifiConfiguration
        method = wifiManager.javaClass.getMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            Boolean::class.javaPrimitiveType
        )
        val result = method.invoke(wifiManager, netConfig, true) as Boolean
        generateQRCode(netConfig.SSID, netConfig.preSharedKey)
    }

        @RequiresApi(Build.VERSION_CODES.O)
    private fun turnOnHotspotForO() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                reservationOfHotspot = reservation
                generateQRCode(
                    reservation.wifiConfiguration!!.SSID,
                    reservation.wifiConfiguration!!.preSharedKey
                )
            }

            override fun onStopped() {
                super.onStopped()
                Log.d(TAG, "Local Hotspot Stopped")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Log.d(TAG, "Local Hotspot failed to start")
            }
        }, Handler())

    }

    private fun generateQRCode(ssid: String, password: String) {
        Log.d(TAG, "ssid: $ssid, password: $password")
        val serializeString = Gson().toJson(HotSpot(ssid, password))
        val bitmap: Bitmap = Qrgenerator
            .newInstance(this)
            ?.setContent(serializeString)
            ?.setErrorCorrectionLevel(ErrorCorrectionLevel.Q)
            ?.setMargin(2)
            ?.qRCOde!!

        binding.ivCode.setImageBitmap(bitmap)

        MyServer.startListening(this)
    }

    // Called from the MyServer worker thread -> hop to the main thread
    override fun onConnectionSuccessful() {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            startActivity(Intent(this, ActivitySender::class.java))
            finish()
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            Toast.makeText(this, "Connection failed: $reason", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        // Stop waiting for a receiver only if nobody connected yet. The hotspot
        // reservation itself is kept until the transfer is done.
        if (com.vt.smart.switchapp.connectivity.MySocketHandler.getSocket() == null) {
            MyServer.stopListening()
        }
        super.onDestroy()
    }
}