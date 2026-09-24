package com.vt.smart.switchapp.connectivity

import android.util.Log
import com.vt.smart.switchapp.ui.interfaces.ConnectionInterface
import com.vt.smart.switchapp.utils.utilities.MyConstant
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class MyServer(private val connectionInterface: ConnectionInterface) : Thread() {
    private val TAG = javaClass.canonicalName
    var socket: Socket? = null
    var serverSocket: ServerSocket? = null
    @Volatile
    private var stoppedByUser = false

    override fun run() {
        try {
            val server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress(MyConstant.PORT))
            serverSocket = server

            socket = server.accept()

            val accepted = socket
            if (accepted == null) {
                throw Exception("Accepted socket is null")
            }
            MySocketHandler.setSocket(accepted)
            try {
                server.close()
            } catch (_: Exception) {
            }
            connectionInterface.onConnectionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Exception server 1: ${e.message}")
            if (!stoppedByUser) {
                connectionInterface.onConnectionFailed(e.message ?: "Server failed")
            }
        } finally {
            clearActive(this)
        }
    }

    fun shutdown() {
        stoppedByUser = true
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
    }

    companion object {
        private val lock = Any()
        @Volatile
        private var active: MyServer? = null

        fun startListening(callback: ConnectionInterface) {
            synchronized(lock) {
                val current = active
                if (current != null && current.isAlive) {
                    Log.d("MyServer", "Server already listening")
                    return
                }
                val server = MyServer(callback)
                active = server
                server.start()
            }
        }

        fun stopListening() {
            synchronized(lock) {
                active?.shutdown()
                active = null
            }
        }

        private fun clearActive(server: MyServer) {
            synchronized(lock) {
                if (active === server) {
                    active = null
                }
            }
        }
    }
}
