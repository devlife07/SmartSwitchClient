package com.vt.smart.switchapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vt.smart.switchapp.connectivity.MySocketHandler
import com.vt.smart.switchapp.ui.models.TransferFile
import com.vt.smart.switchapp.utils.utilities.MyViewState
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class FileSenderViewModel(val context: Application) :
    AndroidViewModel(context) {

    private val _viewState = MutableSharedFlow<MyViewState>()

    val viewState: SharedFlow<MyViewState> = _viewState

    private var job: Job? = null

    fun send(transferModel: TransferFile) {
        if (job != null) {
            return
        }
        job = viewModelScope.launch {
            withContext(context = Dispatchers.IO) {
                _viewState.emit(value = MyViewState.Idle)

                var socket: Socket? = MySocketHandler.getSocket()
                val outputStream = socket?.getOutputStream()
                val objectOutputStream = ObjectOutputStream(outputStream)
                val bufferd = BufferedOutputStream(outputStream)
                objectOutputStream.writeInt(transferModel.list.size)
                val gson = Gson()
                val json: String = gson.toJson(transferModel)
                objectOutputStream.writeObject(json)
                val cr = context.contentResolver
                var i = 0
                try {
                    for (dataModel in transferModel.list) {
                        if(dataModel.type=="Apps"){
                            try {
                                val bytes = ByteArray(File(dataModel.path).length().toInt())
                                val bis = BufferedInputStream(FileInputStream(File(dataModel.path)))
                                bis.read(bytes, 0, bytes.size)
                                objectOutputStream.writeObject(dataModel)
                                objectOutputStream.writeObject(bytes)
                            } catch (ex: OutOfMemoryError) {

                            }

                        }else{
                            val buf = ByteArray(1024)
                            var inputStream: InputStream?
                            var len = 0
                            var write = 0
                            try {
                                val fileUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        File(dataModel.path)
                                    )
                                } else {
                                    Uri.fromFile(File(dataModel.path))
                                }
                                inputStream =
                                    cr.openInputStream(fileUri)
                                while (inputStream!!.read(buf, 0, buf.size).also { len = it } != -1) {
                                    write += len
                                    bufferd.write(buf, 0, len)
                                }
                                inputStream.close()
                            } catch (e: Exception) {
                            }
                        }


                        _viewState.emit(value = MyViewState.Progress((((i + 1).toFloat() / transferModel.list.size.toFloat()) * 100).toInt()))

                    }
                    _viewState.emit(value = MyViewState.Success)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    _viewState.emit(value = MyViewState.Failed(throwable = e))
                } finally {

                    outputStream?.close()
                    objectOutputStream?.close()
                    socket?.close()
                }
            }
        }
        job?.invokeOnCompletion {
            job = null
        }
    }
}