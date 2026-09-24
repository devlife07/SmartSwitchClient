package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.application.Application
import com.vt.smart.switchapp.databinding.ActivitySplashBinding
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.utils.utilities.MyConstant
import com.vt.smart.switchapp.utils.utilities.MyConstant.PERMISSION_REQUEST_CODE
import com.vt.smart.switchapp.utils.utilities.MyConstant.allPermissionsList
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import com.vt.smart.switchapp.ads.YandexADs
import com.vt.smart.switchapp.utils.utilities.BillingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale


class Splash : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val isFirstTime = "is_first_impression"
//    private lateinit var consentInformation: ConsentInformation
//    private var consentForm: ConsentForm? = null
//    private val premiumViewModel: PremiumViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

    try {
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_color)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    initializeAdMob()
    /*  try {
          binding.appVersion.text = "v.${BuildConfig.VERSION_NAME}"
      } catch (e: Exception) {
          e.printStackTrace()
      }*/
//        updateUI()
    try {
        privacyPolicy()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    binding.btnStart.setOnClickListener {
            if (checkPermission()) {
                startMainActivity()
            } else {
                changeStatusBar()
                binding.tvPrivacyPolicy.visibility = View.INVISIBLE
                binding.consentForm.visibility = View.VISIBLE
                binding.grantForm.btnContinue.setOnClickListener {
                    checkPermissionsifNotGranted()
                }
            }
        }

    }
    suspend fun getUserCountry(): String? = withContext(Dispatchers.IO) {
        try {
            (URL("https://ipapi.co/json/").openConnection() as HttpURLConnection).run {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"

                val response = inputStream.bufferedReader().use { it.readText() }
                JSONObject(response).optString("country", null)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isUserRussianOrUkrainian(): Boolean {
        // Try API first
        val country = getUserCountry()
        if (country == "RU" || country == "UA") return true

        // Fallback to device language
        val lang = Locale.getDefault().language
        return lang == "ru" || lang == "uk"
    }

    private fun changeStatusBar() {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        } catch (e: Exception) {
            //
        }

    }

    fun checkPermissionsifNotGranted() {
        val permissions =
            if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_CONTACTS
                )
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_CONTACTS
                )
            }
        Permissions.check(
            this /*context*/,
            permissions,
            null /*rationale*/,
            null /*options*/,
            object : PermissionHandler() {
                override fun onGranted() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            if (checkPermission()) {

                                //showInterstitial()
                            }

                        } else { //request for the permission
                            if (Environment.isExternalStorageManager()) {
                                //  showInterstitial()
                            } else { //request for the permission
                                try {
                                    val intent =
                                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    intent.addCategory("android.intent.category.DEFAULT")
                                    intent.data = Uri.parse(
                                        String.format(
                                            "package:%s",
                                            applicationContext.packageName
                                        )
                                    )
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent()
                                    intent.action =
                                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                    startActivity(intent)
                                }
                            }
                        }
                    } else {
                        // showInterstitial()
                    }
                    startMainActivity()
                }

                override fun onDenied(
                    context: Context?,
                    deniedPermissions: ArrayList<String>?
                ) {
                    Toast.makeText(
                        this@Splash,
                        "Permissions required!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun privacyPolicy() {
        val text = getString(R.string.pp)
        val spannableString = SpannableString(text)
        // Making "Terms and Conditions" clickable (if required)
   /*     val termsStart = text.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length*/
     /*   val termsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.GRAY // Set text color to yellow
                ds.isUnderlineText = false // Keep underline
            }
        }*/
      /*  spannableString.setSpan(
            termsClickableSpan,
            termsStart,
            termsEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )*/
      /*  spannableString.setSpan(
            UnderlineSpan(),
            termsStart,
            termsEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )*/

        // Making "Privacy Policy" clickable
        val privacyStart = text.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        val privacyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                AppUtils.privacy(this@Splash)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.YELLOW // Set text color to yellow
                ds.isUnderlineText = true // Keep underline
            }
        }
        spannableString.setSpan(
            privacyClickableSpan,
            privacyStart,
            privacyEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            UnderlineSpan(),
            privacyStart,
            privacyEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvPrivacyPolicy.text = spannableString
//        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod
    }

    private fun checkPermission(): Boolean {

        val storagePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val storagePermission2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val courseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        val contactPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        )

        val videosPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        val audiosPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        val imagesPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        if (Build.VERSION.SDK_INT >= 33) {
            return videosPermission == PackageManager.PERMISSION_GRANTED &&
                    audiosPermission == PackageManager.PERMISSION_GRANTED &&
                    imagesPermission == PackageManager.PERMISSION_GRANTED &&
                    courseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                    fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                    contactPermission == PackageManager.PERMISSION_GRANTED &&
                    cameraPermission == PackageManager.PERMISSION_GRANTED
        } else
            return storagePermission == PackageManager.PERMISSION_GRANTED &&
                    storagePermission2 == PackageManager.PERMISSION_GRANTED &&
                    courseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                    fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                    contactPermission == PackageManager.PERMISSION_GRANTED &&
                    cameraPermission == PackageManager.PERMISSION_GRANTED
    }
    private var uiUpdated = false
    private fun safeUpdateUI() {
        if (!uiUpdated) {
            uiUpdated = true
            updateUI()
        }
    }
    private fun updateUI() {
        binding.progress.visibility = View.GONE
        binding.btnStart.visibility = View.VISIBLE
        //  checkForUpdate()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 112) {
            if (resultCode != RESULT_OK) {
                Log.e("MY_APP", "Update flow failed! Result code: $resultCode")
                // If the update is cancelled or fails,
                // you can request to start the update again.
            }
        }
    }
    private val splashTimeout = 6000L

    private fun initializeAdMob() {

        // 1️⃣ Hard UI fallback (never depend on ads)
        Handler(Looper.getMainLooper()).postDelayed({
            safeUpdateUI()
        }, splashTimeout)

        lifecycleScope.launch {
            try {
                val isRUorUA = isUserRussianOrUkrainian()
//                val isRUorUA = true
                YandexADs.setRussianUser(this@Splash, isRUorUA)
                if (isRUorUA) {
                    YandexADs.initializeYandex(this@Splash) {
                        YandexADs.loadInterstitialAd(this@Splash)
                        YandexADs.loadAppOpenAd(this@Splash)
                        startApp()
                    }
                    return@launch
                }
                else{
                    try {
                        ManageAds.initializeAds(this@Splash)
                        (application as? Application)?.preloadOpenAd()
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                YandexADs.setRussianUser(this@Splash, false)
                try {
                    ManageAds.initializeAds(this@Splash)
                    (application as? Application)?.preloadOpenAd()
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            startApp()
        }
    }



    private fun startApp() {

        CoroutineScope(Dispatchers.Main).launch {

            // Small splash delay (optional)
            delay(1500)

            try {
                // Load interstitial safely
                ManageAds.loadAdMobInterstitialAds(this@Splash)

                val application = application as? Application

                application?.showAdIfAvailable(
                    this@Splash,
                    object : Application.OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            safeUpdateUI()
                        }
                    }
                ) ?: safeUpdateUI()

            } catch (t: Throwable) {
                t.printStackTrace()
                safeUpdateUI()
            }
        }
    }
    private fun isUserSubscribed(): Boolean = BillingManager.isSubscribed.value

    private fun startMainActivity() {

        if (isUserSubscribed()){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }else{
            startActivity(Intent(this, SubscriptionActivity::class.java).putExtra("isFromSplash",true))
            finish()
        }

    }
    private fun checkIfPermissionsGranted(): Boolean {

        val list = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            for (perm in MyConstant.allPermissionsList33) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
//                    Log.e("TESTTAG", "CALLED NOT GRANTED $perm")
                    list.add(perm)
                }
            }
        } else {
            for (perm in allPermissionsList) {
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
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true

    }


    override fun onRequestPermissionsResult(RC: Int, per: Array<String>, PResult: IntArray) {
        super.onRequestPermissionsResult(RC, per, PResult)

        if (RC == PERMISSION_REQUEST_CODE) {
            val permissionResults = HashMap<String, Int>()
            var deniedCode = 0

            for (i in PResult.indices) {
                if (PResult[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults[per[i]] = PResult[i]
                    deniedCode++
                }
            }
            if (deniedCode == 0) {
                Log.d("TESTAG", "allgranted")
//                ManageAdsUnit.loadAdMobInterstitialAds(this@ActivitySplash)
            } else {
                for ((perName, permResult) in permissionResults) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, perName)) {
                        showDialog(
                            "", "This App needs Permissions.", "Yes, Grant Permissions",
                            { dialogInterface, i ->
                                dialogInterface.dismiss()
                                checkIfPermissionsGranted()
                            },
                            "No, Exit App", { dialogInterface, i ->
                                dialogInterface.dismiss()
                                finish()
                            }, false
                        )
                    } else {
                        showDialog(
                            "",
                            "You have denied some permissions. Allow all Permissions at" + "[AppSettings] > [Permissions]",
                            "Go to AppSettings",
                            { dialogInterface, i ->
                                dialogInterface.dismiss()
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                //    finish();
                            },
                            "No Exit App",
                            { dialogInterface, _ ->
                                dialogInterface.dismiss()
                                finish()
                            }, false
                        )
                        break
                    }
                }
            }
        }

    }


    private fun showDialog(
        title: String,
        msg: String,
        positiveLabel: String,
        positiveOnClick: DialogInterface.OnClickListener,
        negativeLabel: String,
        negativeOnClick: DialogInterface.OnClickListener,
        isCancelable: Boolean
    ): AlertDialog {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setCancelable(isCancelable)
        builder.setMessage(msg)
        builder.setPositiveButton(positiveLabel, positiveOnClick)
        builder.setNegativeButton(negativeLabel, negativeOnClick)

        val alertDialog = builder.create()
        alertDialog.show()
        return alertDialog
    }

}