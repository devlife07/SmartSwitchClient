package com.vt.smart.switchapp.ui.interfaces

interface ConnectionInterface {

    fun onConnectionSuccessful()
    fun onConnectionFailed(reason: String) {}
}