package com.vt.smart.switchapp.ui.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.ui.models.TransferFile
import com.vt.smart.switchapp.ui.models.TransferData
import com.vt.smart.switchapp.utils.utilities.MyViewState
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.*

class FileReceiverViewModel(val context: Application) :
    AndroidViewModel(context) {

    private val _viewState = MutableSharedFlow<MyViewState>()

    val viewState: SharedFlow<MyViewState> = _viewState


    private var job: Job? = null

    fun startListener() {
        if (job != null) {
            return
        }

        job = viewModelScope.launch(context = Dispatchers.IO) {
            _viewState.emit(value = MyViewState.Idle)
            var len: Int? = null

            val inputstream = MySocketHandler.getSocket()?.getInputStream()
            val objectInputStream = ObjectInputStream(inputstream)
            val buffInputStream = BufferedInputStream(inputstream)

            // we get size of item to be received
            val sizeOfItems = objectInputStream.readInt()
            val receivedList = objectInputStream.readObject()
            var file_data = ArrayList<TransferData>()

            if (receivedList != null) {
                val gson = Gson()
                val file =
                    gson.fromJson(receivedList.toString(), TransferFile::class.java)
                file_data = ArrayList(file.list)
            }
            var j = 0

            try {
                for (i in file_data.indices) {

                    val folder =
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "/Smart Switch"
                        )

                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                    val fileName = if (file_data[i].type == "Apps") {
                        file_data[i].name + ".apk"
                    } else if (file_data[i].type == "contacts") {
                        file_data[i].name + ".vcf"
                    } else
                        file_data[i].name
                    val file = File(
                        folder, "/$fileName"
                    )
                    if (file_data[i].type == "Apps") {
                        try {
                            var bytes: ByteArray?
                            bytes = objectInputStream.readObject() as ByteArray

                            val folder =
                                File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "/Smart Switch"
                                )

                            if (!folder.exists()) {
                                folder.mkdirs()
                            }
                            var fos: FileOutputStream? = null

                            fos = FileOutputStream(file)
                            fos.write(bytes)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {

                        var fileSize = file_data[i].fileLength
                        val outputStream: OutputStream = FileOutputStream(file)
                        val buf = ByteArray(1024)


                        while (fileSize > 0 && buffInputStream.read(
                                buf, 0,
                                Math.min(buf.size.toLong(), fileSize).toInt()
                            ).also { len = it } != -1
                        ) {
                            len?.let { outputStream.write(buf, 0, it) }
                            //                        outputStream.flush();
                            fileSize -= len!!.toLong()
                        }
                        outputStream.close()
                    }
                    j += 1
                    _viewState.emit(value = MyViewState.Progress((((j + 1).toFloat() / file_data.size.toFloat()) * 100).toInt()))

                }
                _viewState.emit(value = MyViewState.Progress(100))
                _viewState.emit(value = MyViewState.Success)

            } catch (e: Exception) {
                _viewState.emit(value = MyViewState.Failed(throwable = e))

            }
        }
        job?.invokeOnCompletion {
            job = null
        }
    }

}