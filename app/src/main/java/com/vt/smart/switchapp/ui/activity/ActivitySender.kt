package com.vt.smart.switchapp.ui.activity

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.google.gson.Gson
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.ads.ManageAdsUnit
import com.vt.smart.switchapp.connectivity.MySocketHandler.Companion.getSocket
import com.vt.smart.switchapp.databinding.ActivitySenderBinding
import com.vt.smart.switchapp.ui.models.Contact
import com.vt.smart.switchapp.ui.viewmodel.FileSenderViewModel
import com.vt.smart.switchapp.utils.utilities.MyConstant
import java.io.*
import kotlin.getValue

class ActivitySender : AppCompatActivity() {
    private val TAG = javaClass.canonicalName
    private lateinit var binding: ActivitySenderBinding
    private val fileSenderViewModel by viewModels<FileSenderViewModel>()

    private var mArrayList: ArrayList<com.vt.smart.switchapp.ui.models.TransferData> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySenderBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        ManageAdsUnit.getInstance().showAdMobInterstitial(this,this)
        ManageAdsUnit.getInstance().showAdMobBanner(this,this,binding.bannerLayout)
        binding.ripple.startRippleAnimation()
        getFilesAndTransfer()

        binding.btnDone.setOnClickListener {
            finish()
        }
    }

    private fun getFilesAndTransfer() {
        val sharedPreferences = getSharedPreferences(MyConstant.prefName, MODE_PRIVATE)
        val gson = Gson()
        val name: String? = sharedPreferences.getString("dataList", null)
        val list = gson.fromJson(
            name,
            Array<com.vt.smart.switchapp.ui.models.TransferData>::class.java
        )
        mArrayList.addAll(list)
        Log.e(TAG, "getFilesAndTransfer: ${list.size}")
        val transfer = TransferData()
        transfer.start()
//        for (index in mArrayList.indices) {
//            mArrayList[index].fileLength = File(mArrayList[index].path).length()
//        }

//        fileSenderViewModel.send(AllFileTransferModel(mArrayList))
//
//        CoroutineScope(Dispatchers.Main).launch {
//            fileSenderViewModel.viewState.collectLatest {
//                when (it) {
//                    is Progress -> {
//                        binding.tvPercentage.text = it.progress.toString()
//                    }
//                    is ViewState.Success -> {
//                        binding.btnDone.visibility = View.VISIBLE
//                        MUtils.presentToast(this@ActivitySender, "All files sent successfully")
//                    }
//                    is ViewState.Failed -> {
//                        binding.btnDone.visibility = View.VISIBLE
//                        MUtils.presentToast(this@ActivitySender, "Some files failed to sent")
//                    }
//                }
//            }
//        }

    }

    inner class TransferData : Thread() {
        @SuppressLint("SetTextI18n")
        override fun run() {
            super.run()
            try {
                val socket = getSocket()
                if (socket != null) {
                    val oos = ObjectOutputStream(socket.getOutputStream())
                    val dos = DataOutputStream(oos)
                    dos.writeInt(mArrayList.size)
                    // dos.writeUTF(MUtils.size(Constants.totalDataSize))
                    for (i in 0 until mArrayList.size) {
                        try {
                            val currentFile = File(mArrayList[i].path)

                            try {
                                val bytes = ByteArray(currentFile.length().toInt())
                                val bis = BufferedInputStream(FileInputStream(currentFile))
                                bis.read(bytes, 0, bytes.size)
                                oos.writeObject(mArrayList[i])
                                oos.writeObject(bytes)
                            } catch (ex: OutOfMemoryError) {

                            }
                            runOnUiThread {

                                val percentage =
                                    ((i + 1).toFloat() / mArrayList.size.toFloat()) * 100
                                binding.tvPercentage.text = "${percentage.toInt()}%"

                            }

                        } catch (ex: Exception) {
                            Log.e(TAG, "Exception : $ex")
                            ex.printStackTrace()
                        } finally {
                            try {
                                oos.flush()
                                oos.reset()
                            } catch (ex: Exception) {
                                Log.e(TAG, "run: $ex")
                            }
                        }
                    }
                    runOnUiThread {
                        // showDialog()
                        //   binding.tvPercentage.text = "${filesList.size}/${filesList.size}"
                        binding.btnDone.visibility = View.VISIBLE
                        AppUtils.presentToast(this@ActivitySender, "All files sent successfully")
                    }
                    try {
                        oos.close()
                        socket.close()
                    } catch (ex: Exception) {
                        Log.e(TAG, "run: $ex")
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, " HERE run: $ex")
                AppUtils.presentToast(this@ActivitySender, "Connection interrupted")
                finish()
            }
        }
    }

    @SuppressLint("Range")
    private fun getAllPhoneContacts(): ArrayList<Contact> {

        val list: ArrayList<Contact> = ArrayList()
        val cr: ContentResolver = contentResolver
        val cur = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        if (cur?.count ?: 0 > 0) {
            var phoneNo: String = ""
            while (cur != null && cur.moveToNext()) {
                val id = cur.getString(
                    cur.getColumnIndex(ContactsContract.Contacts._ID)
                )

                val name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id), null
                    )
                    while (pCur!!.moveToNext()) {
                        phoneNo = pCur.getString(
                            pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                    }
                    list.add(
                        Contact(
                            name, phoneNo
                        )
                    )

                    pCur.close()
                }
            }
        }
        cur?.close()
        return list
    }

}