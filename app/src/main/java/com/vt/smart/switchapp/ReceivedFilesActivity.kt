package com.vt.smart.switchapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.vt.smart.switchapp.databinding.ActivityReceivedFilesBinding
import com.vt.smart.switchapp.ui.models.HistoryModel
import com.vt.smart.switchapp.ui.adapters.HistoryAdapter
import com.vt.smart.switchapp.AppUtils.formatDateForGroup
import com.vt.smart.switchapp.BuildConfig
import java.io.File

class ReceivedFilesActivity : AppCompatActivity() {

    val list: ArrayList<HistoryModel> = ArrayList()

    val binding: ActivityReceivedFilesBinding by lazy {
        ActivityReceivedFilesBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val adapter = HistoryAdapter(ArrayList()) {
            if (it is HistoryModel) {
                val file = File(it.path)
                try {
                    view(
                        this,
                        FileProvider.getUriForFile(
                            this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            file
                        ),
                        DocumentFile.fromFile(file).type
                    )
                } catch (e: java.lang.Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.file_corrupted),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
        val folder =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/Phone Switch Clone"
            )
        val files = folder.listFiles()
        if (files == null) {
            binding.noItemFound.visibility = View.VISIBLE
        } else
            binding.noItemFound.visibility = View.GONE
        files?.forEach {
            list.add(HistoryModel(it.name, it.absolutePath, it.lastModified()))
        }
        list.sortByDescending { it.dateModified }
        val objectList: ArrayList<Any> = ArrayList()
        val tempList = list.groupBy { it.dateModified.formatDateForGroup(this) }
        tempList.forEach {
            objectList.add(it.key)
            objectList.addAll(it.value)
        }
        adapter.submitList(objectList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.imgBack.setOnClickListener { onBackPressed() }
    }

    fun view(context: Context, uri: Uri?, type: String?) {
        try {
            if (VERSION.SDK_INT >= 24) {
                try {
                    val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                    m.invoke(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.unable_to_open, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, R.string.unable_to_open, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.unable_to_open, Toast.LENGTH_LONG)
                .show()
        }
    }
}