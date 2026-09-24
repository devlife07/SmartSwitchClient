package com.vt.smart.switchapp.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log
import java.net.Inet4Address
import java.net.InetAddress

/**
 * On Android 10+ the receiver joins the sender's hotspot through a
 * WifiNetworkSpecifier request. That Wi-Fi link only stays up while the
 * NetworkCallback is registered - if the activity that requested it
 * unregisters it in onDestroy(), the phone drops the hotspot and the running
 * transfer dies.
 *
 * This holder keeps the request alive at process level (no service needed)
 * until the transfer is finished, then releases it.
 */
object HotspotNetworkHolder {
    private const val TAG = "HotspotNetworkHolder"

    private var connectivityManager: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    var network: Network? = null
        private set

    @Synchronized
    fun hold(context: Context, cm: ConnectivityManager, cb: ConnectivityManager.NetworkCallback) {
        if (callback != null && callback !== cb) {
            releaseInternal()
        }
        connectivityManager = cm
        callback = cb
        Log.d(TAG, "Holding hotspot network request (${context.packageName})")
    }

    @Synchronized
    fun onNetworkAvailable(network: Network) {
        this.network = network
    }

    @Synchronized
    fun isHolding(cb: ConnectivityManager.NetworkCallback?): Boolean {
        return cb != null && callback === cb
    }

    /**
     * Gateway (= hotspot owner / sender) address of the joined Wi-Fi network.
     * Used when WifiManager.dhcpInfo is empty, which happens on newer Android
     * versions for specifier based connections.
     */
    fun gatewayAddress(): InetAddress? {
        val cm = connectivityManager ?: return null
        val net = network ?: return null
        return try {
            val linkProperties = cm.getLinkProperties(net) ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val dhcp = linkProperties.dhcpServerAddress
                if (dhcp is Inet4Address && !dhcp.isAnyLocalAddress) return dhcp
            }
            linkProperties.routes
                .mapNotNull { it.gateway }
                .firstOrNull { it is Inet4Address && !it.isAnyLocalAddress }
        } catch (e: Exception) {
            Log.e(TAG, "gatewayAddress failed: ${e.message}")
            null
        }
    }

    @Synchronized
    fun release() {
        releaseInternal()
    }

    private fun releaseInternal() {
        val cm = connectivityManager
        val cb = callback
        if (cm != null && cb != null) {
            try {
                cm.bindProcessToNetwork(null)
            } catch (_: Exception) {
            }
            try {
                cm.unregisterNetworkCallback(cb)
            } catch (_: Exception) {
                // already unregistered
            }
        }
        connectivityManager = null
        callback = null
        network = null
    }
}
