package com.vt.smart.switchapp.ui.activity

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ads.ManageAds
import com.vt.smart.switchapp.databinding.ActivityMainBinding
import com.vt.smart.switchapp.databinding.ExitDialogBinding
//import com.easy.clone.databinding.NativeAdLayoutBinding
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.AppUtils.loadingBar
import com.vt.smart.switchapp.utils.utilities.MyConstant
import com.vt.smart.switchapp.utils.utilities.drawableStorage
import com.vt.smart.switchapp.utils.utilities.freespace
import com.vt.smart.switchapp.utils.utilities.percentage
import com.vt.smart.switchapp.utils.utilities.storageValue
import com.vt.smart.switchapp.utils.utilities.usedspace
//import com.google.android.gms.ads.AdListener
//import com.google.android.gms.ads.AdLoader
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.LoadAdError
//import com.google.android.gms.ads.VideoController
//import com.google.android.gms.ads.VideoOptions
//import com.google.android.gms.ads.nativead.NativeAd
//import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.vt.smart.switchapp.BuildConfig
import com.vt.smart.switchapp.ads.AdManager
import com.vt.smart.switchapp.ads.ManageAds.mInterstitialAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var exitBinding: ExitDialogBinding
//    private var currentNativeAd: NativeAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        exitBinding = ExitDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        customToolbar()
        try {
            AppUtils.progressDG(this)
            loadingBar?.show()
            loadAdMobInterstitialAds(this)
            ManageAds.loadNativeAD(binding.myTemplate,this@MainActivity)
//            ManageAds.showAdMobBanner(this, this, binding.bannerLayout)
        } catch (t: Throwable) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
        }
     /*   if (GlobalValues.isProVersion.value == false) {
            val adRequest = AdRequest.Builder().build()
            binding.bannerAd.loadAd(adRequest)
            binding.bannerAd.visibility = View.VISIBLE
        }*/

        val headerView: View = binding.navigationView
        val handler = Handler()
        handler.postDelayed({
            getStorage()
        }, 1000)
        val rateus = headerView.findViewById<TextView>(R.id.rate_us)
//        val backarrow = headerView.findViewById<ImageView>(R.id.backarrow)
        val homeApp = headerView.findViewById<TextView>(R.id.home_app)
        val privacypolicy = headerView.findViewById<TextView>(R.id.privacy_policy)
        val removead = headerView.findViewById<TextView>(R.id.remove_ads)
        val more_app = headerView.findViewById<TextView>(R.id.more_apps)
        rateus.setOnClickListener {
            AppUtils.rateUs(this)
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    /*    removead.setOnClickListener {
//            Toast.makeText(this, "Remove Ads", Toast.LENGTH_SHORT).show()
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            startActivity(Intent(this, PremiumActivity::class.java))
        }*/
        more_app.setOnClickListener {
//            Toast.makeText(this, "More Apps", Toast.LENGTH_SHORT).show()
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
        homeApp.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    /*    backarrow.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }*/
        privacypolicy.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            AppUtils.privacy(this)
        }
        binding.phoneClone.setOnClickListener {
            ManageAds.showInterstitialAd( this) {
                val intent = Intent(this@MainActivity, PhoneClone::class.java)
                startActivity(intent)
            }
        }
        binding.receivelayout.setOnClickListener {
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
                    Intent(
                        this@MainActivity,
                        ChooseConnection::class.java
                    ).putExtra("user", "receiver")
                )

            }
        }
        binding.sendlayout.setOnClickListener {
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
                    startActivity(
                        Intent(
                            this@MainActivity,
                            SingleSelection::class.java
                        )
                    )
                }
            }
        }
/*        binding.history.setOnClickListener {
            ManageAds.showInterstitialAd(true, this) {
                if (Build.VERSION.SDK_INT >= 30) {
                    if (!Environment.isExternalStorageManager()) {
                        try {
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.addCategory("android.intent.category.DEFAULT")
                            intent.data = Uri.parse(String.format("package:%s", packageName))
                            startActivityForResult(intent, 2000)
                        } catch (e: java.lang.Exception) {
                            val obj = Intent()
                            obj.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            startActivityForResult(obj, 2000)
                        }
                    }
                }
                if (checkIfPermissionsGranted()) {
                    startActivity(Intent(this@MainActivity, ReceivedFilesActivity::class.java))
                }
            }
        }*/

        binding.video.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, SingleSelection::class.java)
                intent.putExtra("targetFragment", 1) // Pass the required fragment index (e.g., 2 for the third fragment)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.images.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, SingleSelection::class.java)
                intent.putExtra("targetFragment", 0) // Pass the required fragment index (e.g., 2 for the third fragment)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.contacts.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, SingleSelection::class.java)
                intent.putExtra("targetFragment", 5) // Pass the required fragment index (e.g., 2 for the third fragment)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.audio.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, SingleSelection::class.java)
                intent.putExtra("targetFragment", 3) // Pass the required fragment index (e.g., 2 for the third fragment)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.files.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, SingleSelection::class.java)
                intent.putExtra("targetFragment", 4) // Pass the required fragment index (e.g., 2 for the third fragment)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.apps.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, SingleSelection::class.java)
                intent.putExtra("targetFragment", 2) // Pass the required fragment index (e.g., 2 for the third fragment)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        askReview()
        checkForAppUpdate()
     /*   try {
            if(GlobalValues.isProVersion.value==false) {
                refreshAd(exitBinding.nativeAd)
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }*/
    }

    private fun customToolbar() {
        binding.customToolbar.title.text = "Easy Clone"
        binding.customToolbar.backarrow.visibility = View.GONE
        binding.customToolbar.drawerMenu.visibility = View.VISIBLE
//        binding.customToolbar.premium.visibility = View.VISIBLE
     /*   binding.customToolbar.premium.setOnClickListener {
            Toast.makeText(this, "Premium", Toast.LENGTH_SHORT).show()
        }*/
        binding.customToolbar.drawerMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else
                binding.drawerLayout.openDrawer(GravityCompat.START)
        }
     /*   binding.customToolbar.premium.setOnClickListener {
            try {
                //adremoval
//                if (GlobalValues.isProVersion.value == false) {
                    startActivity(Intent(this, PremiumActivity::class.java))
//                } else {
//                    Toast.makeText(this, "You already purchased", Toast.LENGTH_SHORT).show()
//                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    fun loadAdMobInterstitialAds(context: Activity) {
        Log.d("ADMOBS", "🔹 loadAdMobInterstitialAds() called")
        if (mInterstitialAd != null) return
        try {
            val adRequest = AdRequest.Builder().build()
            val adId = if (BuildConfig.DEBUG) {
                AdManager.AdMobInterIdTest
            } else {
                AdManager.AdMobInterIdTest
            }

            InterstitialAd.load(context, adId, adRequest, object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("ADMOBS", "✅ Interstitial Ad Loaded Successfully")
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("ADMOBS", "❌ Failed to Load Interstitial Ad: ${loadAdError.message}")
                    mInterstitialAd = null
                }
            })
        } catch (t: Throwable) {
            mInterstitialAd = null
            Log.e("ADMOBS", "Interstitial load threw", t)
        }
    }
    private fun getStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stat = StatFs(Environment.getExternalStorageDirectory().path)
                val bytesAvailable = stat.blockSizeLong * stat.blockCountLong
                val bytesFree = stat.blockSizeLong * stat.availableBlocksLong
                val bytesUsed = bytesAvailable - bytesFree

                val units = arrayOf("B", "KB", "MB", "GB", "TB")

                val total = formatSize(bytesAvailable, units)
                val available = formatSize(bytesFree, units)
                val usedSpace = formatSize(bytesUsed, units)
                freespace = available
                usedspace = usedSpace
                val usedPercentage =
                    (bytesUsed.toDouble() / bytesAvailable.toDouble() * 100).toInt()
                val freePercentage = 100 - usedPercentage

                withContext(Dispatchers.Main) {
                    // Update UI based on storage usage
                    updateStorageUI(usedPercentage, usedSpace, total)

                    // Dismiss loading bar after 500ms
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            loadingBar?.dismiss()
                        } catch (e: Exception) {
                            // Handle exception
                        }
                    }, 500)
                }
            } catch (e: Exception) {
                Log.e("StorageError", "Error calculating storage: ${e.message}")
            }
        }
    }

    private fun formatSize(size: Long, units: Array<String>): String {
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun updateStorageUI(freePercentage: Int, usedSpace: String, total: String) {
        val drawableResId = when {
            freePercentage > 80 -> {R.drawable.storage_capacity_full}
            freePercentage in 61..80 -> {R.drawable.storage_capacity_low}
            freePercentage in 36..60 -> {R.drawable.storage_capacity}
            else -> {R.drawable.storage_capacity}
        }
        drawableStorage =drawableResId
        Glide.with(this)
            .load(drawableResId)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    binding.percentagefree.background = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        binding.percentagefree.text = "$freePercentage%\nUsed"
        binding.percentagefree.visibility = View.VISIBLE
        binding.totalTitle.text = usedSpace+" of "+total+" Used"
        storageValue =usedSpace+" of "+total+" Used"
        percentage = "$freePercentage %"
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isFinishing) {
            showExitDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitDialog() {
        val exitDialog = Dialog(this)
        exitDialog.setContentView(R.layout.exit_dialog)
        exitDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        exitDialog.setCanceledOnTouchOutside(false)
        exitDialog.setCancelable(true)
        exitDialog.show()
        val btnYes = exitDialog.findViewById<Button>(R.id.yes)
        val btnRateUs = exitDialog.findViewById<Button>(R.id.rate)
        val btnNo = exitDialog.findViewById<Button>(R.id.no)
        val nativeAdView = exitDialog.findViewById<FrameLayout>(R.id.nativeAd)
       /* if(GlobalValues.isProVersion.value==false) {
            refreshAd(nativeAdView)
        }*/
        btnYes.setOnClickListener {
            exitDialog.dismiss()
            finishAffinity()
        }

        btnNo.setOnClickListener { exitDialog.dismiss() }
        btnRateUs.setOnClickListener {
            exitDialog.dismiss()

            val appPackageName = packageName

            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appPackageName")
                    )
                )
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                )
            }
        }
    }

    private fun askReview() {

        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                setInAppReview(reviewInfo, reviewManager)
            }
        }
    }

    private fun setInAppReview(reviewInfo: ReviewInfo, reviewManager: ReviewManager) {
        val flow = reviewManager.launchReviewFlow(this, reviewInfo);
        flow.addOnCompleteListener {

        }
    }

    private fun checkForAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)

        val appUpdateInfoTask = appUpdateManager!!.appUpdateInfo

        installStateUpdatedListener =
            InstallStateUpdatedListener { installState ->
                if (installState.installStatus() == InstallStatus.DOWNLOADED)
                    popupSnackbarForCompleteUpdateAndUnregister()
            }

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    appUpdateManager!!.registerListener(installStateUpdatedListener!!)
                    startAppUpdateFlexible(appUpdateInfo)
                }
            }
        }
    }

    private fun startAppUpdateFlexible(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager!!.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.FLEXIBLE,
                this,
                1011
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
            unregisterInstallStateUpdListener()
        }
    }

    private fun popupSnackbarForCompleteUpdateAndUnregister() {
        val parentLayout: View = findViewById(android.R.id.content)
        val snackBar = Snackbar.make(
            parentLayout,
            getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        )
        snackBar.setAction(
            R.string.restart
        ) { appUpdateManager!!.completeUpdate() }
        snackBar.setActionTextColor(ContextCompat.getColor(this, R.color.purple_700))
        snackBar.show()
        unregisterInstallStateUpdListener()
    }

    private fun checkNewAppVersionState() {
        appUpdateManager
            ?.appUpdateInfo
            ?.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdateAndUnregister()
                }
            }
    }

    private fun unregisterInstallStateUpdListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null) appUpdateManager!!.unregisterListener(
            installStateUpdatedListener!!
        )
    }

    private var appUpdateManager: AppUpdateManager? = null
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null


    override fun onDestroy() {
        unregisterInstallStateUpdListener()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        ManageAds.loadAdMobInterstitialAds(this)
        checkNewAppVersionState()
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
        if (Build.VERSION.SDK_INT >= 33) {
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
        } else {
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
        if (Build.VERSION.SDK_INT >= 33) {
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
        } else {
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

/*    private fun populateNativeAdView(nativeAd: NativeAd, unifiedAdBinding: NativeAdLayoutBinding) {
        val nativeAdView = unifiedAdBinding.root

        // Set the media view.
        nativeAdView.mediaView = unifiedAdBinding.adMedia

        // Set other ad assets.
        nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = unifiedAdBinding.adBody
        nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
        nativeAdView.iconView = unifiedAdBinding.adAppIcon
        nativeAdView.priceView = unifiedAdBinding.adPrice
        nativeAdView.starRatingView = unifiedAdBinding.adStars
        nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        unifiedAdBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.mediaContent = it }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            unifiedAdBinding.adBody.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adBody.visibility = View.VISIBLE
            unifiedAdBinding.adBody.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            unifiedAdBinding.adCallToAction.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adCallToAction.visibility = View.VISIBLE
            unifiedAdBinding.adCallToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            unifiedAdBinding.adAppIcon.visibility = View.GONE
        } else {
            unifiedAdBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            unifiedAdBinding.adAppIcon.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            unifiedAdBinding.adPrice.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adPrice.visibility = View.VISIBLE
            unifiedAdBinding.adPrice.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            unifiedAdBinding.adStore.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStore.visibility = View.VISIBLE
            unifiedAdBinding.adStore.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            unifiedAdBinding.adStars.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStars.rating = nativeAd.starRating!!.toFloat()
            unifiedAdBinding.adStars.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            unifiedAdBinding.adAdvertiser.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adAdvertiser.text = nativeAd.advertiser
            unifiedAdBinding.adAdvertiser.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        nativeAdView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc != null && mediaContent.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
//                        mainActivityBinding.refreshButton.isEnabled = true
//                        mainActivityBinding.videostatusText.text = "Video status: Video playback has ended."
                        super.onVideoEnd()
                    }
                }
        } else {
//            mainActivityBinding.videostatusText.text = "Video status: Ad does not contain a video asset."
//            mainActivityBinding.refreshButton.isEnabled = true
        }
    }

    *//**
     * Creates a request for a new native ad based on the boolean parameters and calls the
     * corresponding "populate" method when one is successfully returned.
     *//*
    private fun refreshAd(nativeLayout: FrameLayout) {
        // If currentNativeAd is already loaded, populate the nativeLayout without reloading
        if (currentNativeAd != null) {
            val unifiedAdBinding = NativeAdLayoutBinding.inflate(layoutInflater)
            populateNativeAdView(currentNativeAd!!, unifiedAdBinding)
            nativeLayout.removeAllViews()
            nativeLayout.addView(unifiedAdBinding.root)
            return
        }

        val nativeID = if (BuildConfig.DEBUG) {
            resources.getString(R.string.nativeAd)
        } else {
            resources.getString(R.string.nativeAd)
        }

        val builder = AdLoader.Builder(this, nativeID)

        builder.forNativeAd { nativeAd ->
            var activityDestroyed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                activityDestroyed = isDestroyed
            }
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }

            // Destroy the old ad and assign the new one
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd

            // Inflate and populate the native ad view
            val unifiedAdBinding = NativeAdLayoutBinding.inflate(layoutInflater)
            populateNativeAdView(nativeAd, unifiedAdBinding)
            nativeLayout.removeAllViews()
            nativeLayout.addView(unifiedAdBinding.root)
        }

        // Set up ad options
        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        builder.withNativeAdOptions(adOptions)

        // Set the AdListener to handle failures
        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error = """
                domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
            """
              *//*  Toast.makeText(
                    this@MainActivity,
                    "Failed to load native ad with error $error",
                    Toast.LENGTH_SHORT
                ).show()*//*
            }
        }).build()

        // Load the ad
        adLoader.loadAd(AdRequest.Builder().build())
    }*/


}
