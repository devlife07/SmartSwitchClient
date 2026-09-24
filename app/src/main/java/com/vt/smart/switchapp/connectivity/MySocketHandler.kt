package com.vt.smart.switchapp.connectivity

import java.net.Socket

class MySocketHandler {

    companion object {
        private var socket: Socket? = null

        @Synchronized
        fun getSocket(): Socket? {
            return socket
        }

        @Synchronized
        fun setSocket(socket: Socket) {
            Companion.socket = socket
        }

        @Synchronized
        fun clearSocket() {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
            socket = null
        }
    }
}
