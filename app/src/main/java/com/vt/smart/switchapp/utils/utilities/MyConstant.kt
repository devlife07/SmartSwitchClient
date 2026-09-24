package com.vt.smart.switchapp.utils.utilities

import android.Manifest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.vt.smart.switchapp.ui.models.TransferData

object MyConstant {
    const val prefName = "EasySwitchProPref"
    const val PERMISSION_REQUEST_CODE = 2812
    const val PORT = 8080
    const val TRANSFER_SO_TIMEOUT_MS = 30_000
    const val CONNECT_TIMEOUT_MS = 5_000
    const val CLIENT_CONNECT_RETRIES = 5
    const val DHCP_RETRY_DELAY_MS = 1_500L
    const val DHCP_MAX_RETRIES = 10
    var reservationOfHotspot: WifiManager.LocalOnlyHotspotReservation? = null

    var mListToSend: ArrayList<TransferData> = ArrayList()
    var allPermissionsList = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    var allPermissionsList33 = arrayOf(
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )
    var isWifiP2pConnected: Boolean = false
    var savedIpAddress: String = ""

}