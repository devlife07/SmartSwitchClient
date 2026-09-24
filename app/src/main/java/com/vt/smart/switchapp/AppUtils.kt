package com.vt.smart.switchapp

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Time
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.vt.smart.switchapp.BuildConfig
import com.kaopiz.kprogresshud.KProgressHUD
import java.io.File
import java.util.*

object AppUtils {
    @JvmField
    var isConsentGranted= MutableLiveData<Boolean>(false)
    @JvmField
    var adCounter:Int=1
    @JvmField
    var isOpenAdShow= MutableLiveData<Boolean>(false)
    var loadingBar: KProgressHUD?=null
    fun presentToast(context: Context, text: String) {
        try {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
          e.printStackTrace()
        }
    }
    fun progressDG(context: Context){
        try {
            loadingBar =KProgressHUD.create(context)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait")
                .setDetailsLabel("Loading data")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
        }
        catch (e:Exception){
           //
        }

    }
    fun rateUs(activity: Activity) {
        val appPackageName: String = activity.packageName
        try {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (activityNotFoundException: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }
    fun shareApp(activity: Activity) {
        try {
            val appPackageName: String = activity.packageName
            val myUrl = "https://play.google.com/store/apps/details?id=$appPackageName"
            val sendIntent = Intent()
            sendIntent.setAction(Intent.ACTION_SEND)
            sendIntent.putExtra(Intent.EXTRA_TEXT, myUrl)
            sendIntent.setType("text/plain")
            activity.startActivity(
                Intent.createChooser(
                    sendIntent,
                    activity.getString(R.string.share)
                )
            )
        } catch (e: java.lang.Exception) {
        }
    }
    fun privacy(activity: Activity) {
        try {
            val url = "https://sites.google.com/view/smart-mobile-switch-pp/home"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(browserIntent)
        } catch (e: java.lang.Exception) {
            // Catch Exception here
        }
    }
    fun appFile(context: Context, appPackageName: String): File {
        return File(
            context.packageManager.getApplicationInfo(
                appPackageName,
                0
            ).sourceDir
        )
    }

    fun size(size: Long): String {
        var s = ""
        val kb = (size / 1024).toDouble()
        val mb = kb / 1024
        val gb = mb / 1024
        val tb = gb / 1024
        if (size < 1024L) {
            s = "$size Bytes"
        } else if (size >= 1024 && size < 1024L * 1024) {
            s = String.format("%.2f", kb) + " KB"
        } else if (size >= 1024L * 1024 && size < 1024L * 1024 * 1024) {
            s = String.format("%.2f", mb) + " MB"
        } else if (size >= 1024L * 1024 * 1024 && size < 1024L * 1024 * 1024 * 1024) {
            s = String.format("%.2f", gb) + " GB"
        } else if (size >= 1024L * 1024 * 1024 * 1024) {
            s = String.format("%.2f", tb) + " TB"
        }
        return s
    }


    fun isNetworkEnabled(locationManager: LocationManager): Boolean {
        try {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        return false
    }

    fun isGPSEnabled(locationManager: LocationManager): Boolean {

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        return false
    }

    fun verifyAppInstallation(context: Context): Boolean {

        return if (BuildConfig.DEBUG) {
            true
        } else {
            val installersList: List<String> =
                ArrayList(listOf("com.android.vending", "com.google.android.feedback"))
            val appInstaller = context.packageManager.getInstallerPackageName(context.packageName)

            appInstaller != null && installersList.contains(appInstaller)

        }
    }

    fun Long.formatDateForGroup(
        context: Context,
        showYearEvenIfCurrent: Boolean = false
    ): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = this

        return when {
            DateUtils.isToday(this) -> {
                context.getString(R.string.today)
            }

            DateUtils.isToday(this + DateUtils.DAY_IN_MILLIS) -> {
                // ❌ Wrong in original code: should check "yesterday"
                "Tomorrow"
            }

            DateUtils.isToday(this - DateUtils.DAY_IN_MILLIS) -> {
                // ✅ Correct yesterday check
                context.getString(R.string.yesterday)
            }

            else -> {
                // Default date format
                var format = "dd/MM/yyyy"

                // If year is current and not required
                if (!showYearEvenIfCurrent && isThisYear()) {
                    format = "dd/MM"
                }

                DateFormat.format(format, cal).toString()
            }
        }
    }


    fun Long.isThisYear(): Boolean {
        val time = Time()
        time.set(this)
        val thenYear = time.year
        time.set(System.currentTimeMillis())
        return (thenYear == time.year)
    }

    fun showPermissionDialog(activity: Activity, callback: (isGrantClicked: Boolean) -> Unit) {
        val permissionDialog = Dialog(activity)
        permissionDialog.setContentView(R.layout.permission_dialog)
        permissionDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        permissionDialog.setCanceledOnTouchOutside(false)
        permissionDialog.setCancelable(true)
        permissionDialog.show()

        val btnYes = permissionDialog.findViewById<Button>(R.id.yes)
        val btnNo = permissionDialog.findViewById<Button>(R.id.no)
        btnYes.setOnClickListener {
            permissionDialog.dismiss()
            callback(true)
        }

        btnNo.setOnClickListener {
            callback(false)
            permissionDialog.dismiss()
        }

    }
}