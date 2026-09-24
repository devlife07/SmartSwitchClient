package com.vt.smart.switchapp.connectivity

import android.util.Log
import com.vt.smart.switchapp.ui.interfaces.ConnectionInterface
import com.vt.smart.switchapp.utils.utilities.MyConstant
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class MyClient internal constructor(
    address: InetAddress,
    connectionInterface: ConnectionInterface
) : Thread() {
    var hostAddress: String = address.hostAddress ?: address.hostName
    var mConnectionInterface = connectionInterface

    override fun run() {
        var lastError: Exception? = null
        val attempts = MyConstant.CLIENT_CONNECT_RETRIES
        for (attempt in 1..attempts) {
            try {
                val socket = Socket()
                socket.connect(
                    InetSocketAddress(hostAddress, MyConstant.PORT),
                    MyConstant.CONNECT_TIMEOUT_MS
                )
                MySocketHandler.setSocket(socket)
                mConnectionInterface.onConnectionSuccessful()
                return
            } catch (e: Exception) {
                lastError = e
                Log.e("abc", "Client attempt $attempt failed: ${e.message}")
                if (attempt < attempts) {
                    try {
                        Thread.sleep(1_000L * attempt)
                    } catch (_: InterruptedException) {
                        mConnectionInterface.onConnectionFailed("Connection cancelled")
                        return
                    }
                }
            }
        }
        mConnectionInterface.onConnectionFailed(lastError?.message ?: "Connection failed")
    }
}
