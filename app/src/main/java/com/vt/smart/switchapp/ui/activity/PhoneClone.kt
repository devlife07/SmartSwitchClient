package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.databinding.PhoneCloneBinding
import com.vt.smart.switchapp.utils.utilities.MyConstant
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.utils.utilities.drawableStorage
import com.vt.smart.switchapp.utils.utilities.freespace
import com.vt.smart.switchapp.utils.utilities.percentage
import com.vt.smart.switchapp.utils.utilities.usedspace
import java.util.ArrayList

class PhoneClone : AppCompatActivity() {

    val binding: PhoneCloneBinding by lazy {
        PhoneCloneBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        customToolbar()
        ManageAds.loadAdMobInterstitialAds(this)
//        binding.tvUsedSpace.text = intent.getStringExtra("used")
//        binding.tvTotalStorage.text = intent.getStringExtra("total")
//        binding.tvAvailableSpace.text = intent.getStringExtra("available")
         ManageAds.showAdMobBanner(this, this, binding.bannerLayout)
        binding.oldPhoneLayout.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 30) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.parse(String.format("package:%s", packageName))
                        startActivityForResult(intent, 2000)
                        return@setOnClickListener
                    } catch (e: java.lang.Exception) {
                        val obj = Intent()
                        obj.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        startActivityForResult(obj, 2000)
                    }
                }
            }
            if (checkIfPermissionsGranted()) {

                ManageAds.showInterstitialAd( this) {
                    startActivity(Intent(this@PhoneClone, LoadAllData::class.java))
                }
            }
        }
        binding.newPhoneLayout.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 30) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.parse(String.format("package:%s", packageName))
                        startActivityForResult(intent, 2000)
                        return@setOnClickListener
                    } catch (e: java.lang.Exception) {
                        val obj = Intent()
                        obj.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        startActivityForResult(obj, 2000)
                    }
                }
            }
            if (checkIfPermissionsGranted()) {
                startActivity(
                    Intent(this@PhoneClone, ChooseConnection::class.java)
                        .putExtra("user", "receiver")
                )
            }
        }
        binding.percentagefree.text= percentage +"\nUsed"
        binding.usedSpace.text= usedspace
        binding.freeSpace.text= freespace
//        binding.totalTitle.text= storageValue
        Glide.with(this)
            .load(drawableStorage)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    binding.percentagefree.background = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
    private fun customToolbar() {
        binding.customToolbar.title.text="Phone Clone"
        binding.customToolbar.backarrow.setOnClickListener {
            onBackPressed()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("TESTTAG", "CALLED onRequestPermissionsResult")
        Log.e("TESTTAG", "CALLED ${checkIfPermissions()}")
        if (checkIfPermissions()) {
            checkIfDenied()
        }
    }

    private fun checkIfDenied() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) || !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) || !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS
            ) || !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_CONTACTS
            ) || !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            AppUtils.showPermissionDialog(this) {
                if (it)
                    openSettings()
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", this.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun checkIfPermissions(): Boolean {

        val list = ArrayList<String>()
        if(Build.VERSION.SDK_INT>=33){
            for (perm in MyConstant.allPermissionsList33) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("TESTTAG", "CALLED NOT GRANTED $perm")
                    list.add(perm)
                }
            }
        }
        else{
        for (perm in MyConstant.allPermissionsList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("TESTTAG", "PERMISSION DENIED ")
                list.add(perm)
            }
        }
    }
        Log.e("TESTTAG", "PERMISSION DENIED TOTAL ${list.size}")
        return list.isNotEmpty()

    }

    private fun checkIfPermissionsGranted(): Boolean {

        val list = ArrayList<String>()
        if(Build.VERSION.SDK_INT>=33){
            for (perm in MyConstant.allPermissionsList33) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e("TESTTAG", "CALLED NOT GRANTED $perm")
                    list.add(perm)
                }
            }
        }
        else{
        for (perm in MyConstant.allPermissionsList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                list.add(perm)
            }
        }
    }
        if (list.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, list.toTypedArray(),
                MyConstant.PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true

    }
}