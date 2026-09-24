package com.vt.smart.switchapp.ui.models

import java.io.Serializable

class TransferData(
    var name: String,
    var path: String,
    val type: String,
    var fileLength: Long = 0
) : Serializable {
}