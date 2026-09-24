package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.databinding.LoadAllDataBinding
import com.vt.smart.switchapp.ui.models.ContactModel
import com.vt.smart.switchapp.ui.models.TransferData
import com.vt.smart.switchapp.ui.models.FileModel
import com.vt.smart.switchapp.utils.utilities.MyConstant.prefName
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.AppUtils.loadingBar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class LoadAllData : AppCompatActivity() {
    private val TAG = "LoadAllData"
    private lateinit var binding: LoadAllDataBinding

    private var mPicturesList = ArrayList<FileModel>()
    private var mVideosList = ArrayList<FileModel>()
    private var mMusicList = ArrayList<FileModel>()
    private var mOtherFilesList = ArrayList<FileModel>()
    private var mAppsList = ArrayList<FileModel>()
    private var mContactList = ArrayList<ContactModel>()

    private var mImagesSize: Long = 0
    private var mVideosSize: Long = 0
    private var mMusicsSize: Long = 0
    private var mFilesSize: Long = 0
    private var mAppsSize: Long = 0
    private var allDataSize: Long = 0

    private lateinit var sharedPreferences: SharedPreferences
    private val PERMISSION_REQUEST_CODE = 101

    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoadAllDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ManageAds.showAdMobBanner(this, this, binding.bannerLayout)

        sharedPreferences = getSharedPreferences("SmartSwitchPrefs", MODE_PRIVATE)
        binding.title.text = "Select Data"

        setupClickListeners()
        checkPermissionsAndLoad()
    }

    private fun setupClickListeners() {
        binding.backarrow.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnShare.setOnClickListener { prepareDataForTransfer() }

        binding.layoutImages.setOnClickListener { binding.checkboxImages.toggle() }
        binding.layoutVideos.setOnClickListener { binding.checkboxVideos.toggle() }
        binding.layoutMusic.setOnClickListener { binding.checkboxMusic.toggle() }
        binding.layoutApps.setOnClickListener { binding.checkboxApps.toggle() }
        binding.layoutFiles.setOnClickListener { binding.checkboxFiles.toggle() }
        binding.layoutContacts.setOnClickListener { binding.checkboxContacts.toggle() }
    }

    private fun checkPermissionsAndLoad() {
        try {
            val permissions = mutableListOf(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missing.isEmpty()) startLoadingProcess()
            else ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Permission check error", e)
            toast("Error checking permissions")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startLoadingProcess()
    }

    private fun startLoadingProcess() {
        showProgress("Loading data...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jobs = listOf(
                    launch { safeLoadMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mPicturesList) { mImagesSize += it } },
                    launch { safeLoadMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mVideosList) { mVideosSize += it } },
                    launch { safeLoadMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mMusicList) { mMusicsSize += it } },
                    launch { safeLoadFiles() },
                    launch { safeLoadApps() },
                    launch { if (hasPermission(Manifest.permission.READ_CONTACTS)) safeLoadContacts() }
                )
                jobs.joinAll()

                withContext(Dispatchers.Main) {
                    dismissProgress()
                    updateUI()
                    setupCheckboxListeners()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Loading process failed", e)
                withContext(Dispatchers.Main) {
                    dismissProgress()
                    toast("Error loading data")
                }
            }
        }
    }

    // ----------- Safe Media Loader -----------
    private fun safeLoadMedia(uri: Uri, list: ArrayList<FileModel>, sizeUpdate: (Long) -> Unit) {
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(dataIdx) ?: continue
                        val size = cursor.getLong(sizeIdx)
                        list.add(FileModel(cursor.getString(nameIdx) ?: "Unknown", path, "Media"))
                        sizeUpdate(size)
                    } catch (inner: Exception) {
                        Log.e(TAG, "Error reading media item", inner)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Media query failed", e)
        }
    }

    private fun safeLoadFiles() {
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE)
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"

            contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    try {
                        val name = cursor.getString(nameIdx) ?: ""
                        val path = cursor.getString(dataIdx) ?: continue
                        if (name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".txt")) {
                            val size = cursor.getLong(sizeIdx)
                            mOtherFilesList.add(FileModel(name, path, "Document"))
                            mFilesSize += size
                        }
                    } catch (inner: Exception) {
                        Log.e(TAG, "Error reading file item", inner)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Files query failed", e)
        }
    }

    private fun safeLoadApps() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in apps) {
                try {
                    if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                        val file = File(app.sourceDir)
                        if (file.exists()) {
                            mAppsList.add(FileModel(app.loadLabel(pm).toString(), app.sourceDir, "App"))
                            mAppsSize += file.length()
                        }
                    }
                } catch (inner: Exception) {
                    Log.e(TAG, "Error reading app info", inner)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Apps load failed", e)
        }
    }

    private fun safeLoadContacts() {
        try {
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val set = HashSet<String>()

                while (cursor.moveToNext()) {
                    try {
                        val name = cursor.getString(nameIdx) ?: "Unknown"
                        val rawNum = cursor.getString(numIdx) ?: continue
                        val cleanNum = rawNum.replace(Regex("[^0-9]"), "")
                        if (set.add(cleanNum)) {
                            val file = getCsvFile(cleanNum) ?: createCsvFile(cleanNum)
                            file?.let { mContactList.add(ContactModel(name, cleanNum, it.absolutePath)) }
                        }
                    } catch (inner: Exception) {
                        Log.e(TAG, "Error reading contact", inner)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contacts load failed", e)
        }
    }

    // ------------- CSV Helpers -------------
    private fun createCsvFile(number: String): File? {
        return try {
            val dir = File(getExternalFilesDir(null), "Contacts")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "Contact_$number.vcf")
            FileWriter(file).use {
                it.write("BEGIN:VCARD\nVERSION:3.0\nFN:$number\nTEL;TYPE=CELL:$number\nEND:VCARD")
            }
            file
        } catch (e: IOException) {
            Log.e(TAG, "CSV file creation failed", e)
            null
        }
    }

    private fun getCsvFile(number: String): File? {
        val file = File(getExternalFilesDir(null), "Contacts/Contact_$number.vcf")
        return if (file.exists()) file else null
    }

    // ------------- UI Updates -------------
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        binding.tvImagesSize.text = "${mPicturesList.size} | ${formatSize(mImagesSize)}"
        binding.tvVideosSize.text = "${mVideosList.size} | ${formatSize(mVideosSize)}"
        binding.tvMusicSize.text = "${mMusicList.size} | ${formatSize(mMusicsSize)}"
        binding.tvFilesSize.text = "${mOtherFilesList.size} | ${formatSize(mFilesSize)}"
        binding.tvAppsSize.text = "${mAppsList.size} | ${formatSize(mAppsSize)}"
        binding.tvContactsSize.text = "${mContactList.size} Contacts"
        updateTotalDisplay()
    }

    private fun setupCheckboxListeners() {
        val checkListener = CompoundButton.OnCheckedChangeListener { _, _ -> calculateTotalSelected() }
        binding.checkboxImages.setOnCheckedChangeListener(checkListener)
        binding.checkboxVideos.setOnCheckedChangeListener(checkListener)
        binding.checkboxMusic.setOnCheckedChangeListener(checkListener)
        binding.checkboxFiles.setOnCheckedChangeListener(checkListener)
        binding.checkboxApps.setOnCheckedChangeListener(checkListener)
        binding.checkboxContacts.setOnCheckedChangeListener(checkListener)
    }

    private fun calculateTotalSelected() {
        allDataSize = 0
        if (binding.checkboxImages.isChecked) allDataSize += mImagesSize
        if (binding.checkboxVideos.isChecked) allDataSize += mVideosSize
        if (binding.checkboxMusic.isChecked) allDataSize += mMusicsSize
        if (binding.checkboxFiles.isChecked) allDataSize += mFilesSize
        if (binding.checkboxApps.isChecked) allDataSize += mAppsSize
        updateTotalDisplay()
    }

    private fun updateTotalDisplay() {
        binding.tvTotalSize.text = "Selected\n${formatSize(allDataSize)}"
    }

    private fun prepareDataForTransfer() {
        try {
            val finalTransferList = ArrayList<TransferData>()
            if (binding.checkboxImages.isChecked) mPicturesList.forEach { finalTransferList.add(TransferData(it.name, it.path, "Images")) }
            if (binding.checkboxVideos.isChecked) mVideosList.forEach { finalTransferList.add(TransferData(it.name, it.path, "Videos")) }
            if (binding.checkboxApps.isChecked) mAppsList.forEach { finalTransferList.add(TransferData(it.name, it.path, "Apps")) }
            if (binding.checkboxContacts.isChecked) mContactList.forEach { finalTransferList.add(TransferData(it.contactName, it.absolutePath, "Contacts")) }

            if (finalTransferList.isEmpty()) {
                toast("Please select at least one item")
                return
            }

            sharedPreferences.edit { putString("dataList", Gson().toJson(finalTransferList)) }
            startActivity(Intent(this, ChooseConnection::class.java).putExtra("user", "sender"))

        } catch (e: Exception) {
            Log.e(TAG, "Prepare transfer failed", e)
            toast("Error preparing data")
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun formatSize(bytes: Long) = android.text.format.Formatter.formatFileSize(this, bytes)

    private fun toast(msg: String) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    private fun showProgress(msg: String) = runOnUiThread {
        dismissProgress()
        progressDialog = MaterialAlertDialogBuilder(this).setMessage(msg).setCancelable(false).create()
        progressDialog?.show()
    }

    private fun dismissProgress() = runOnUiThread {
        progressDialog?.dismiss()
        progressDialog = null
    }
}