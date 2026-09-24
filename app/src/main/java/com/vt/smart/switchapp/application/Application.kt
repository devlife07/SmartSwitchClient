package com.vt.smart.switchapp.application

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.vt.smart.switchapp.BuildConfig
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ads.YandexADs
import com.vt.smart.switchapp.ui.activity.Splash
import com.vt.smart.switchapp.utils.utilities.BillingManager
import java.util.Date

class Application : MultiDexApplication(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null
    private var skipNextForeground = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        isolateWebViewForThisProcess(base)
    }

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = AppOpenAdManager()
        if (!isMainProcess) {
            Log.d("Application", "Skip app init in secondary process")
            return
        }
        try {
            registerActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        } catch (t: Throwable) {
            Log.e("Application", "Lifecycle setup failed", t)
        }
        try {
            BillingManager.init(this)
        } catch (t: Throwable) {
            Log.e("Application", "Billing init failed", t)
        }

        try {
            YandexADs.restoreRussianFlag(this)
            YandexADs.onAppOpenShown = { skipNextForeground = true }
        } catch (t: Throwable) {
            Log.e("Application", "Yandex flag restore failed", t)
        }
    }

    fun preloadOpenAd() {
        if (!isMainProcess) return
        try {
            if (YandexADs.isRussian) {
                YandexADs.loadAppOpenAd(this)
            } else {
                appOpenAdManager.loadAd(this)
            }
        } catch (t: Throwable) {
            Log.e("Application", "preloadOpenAd failed", t)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        if (!isMainProcess) return
        mainHandler.post {
            if (skipNextForeground) {
                skipNextForeground = false
                Log.d("AppOpen", "Skip resume right after open ad")
                return@post
            }
            val activity = currentActivity ?: return@post
            if (activity is Splash || activity.isFinishing) return@post
            if (BillingManager.isSubscribed.value) return@post
            if (appOpenAdManager.isShowingAd || YandexADs.isShowingAppOpenAd) return@post

            Log.d("AppOpen", "Resume show, russian=${YandexADs.isRussian}")
            try {
                if (YandexADs.isRussian) {
                    YandexADs.showAppOpenAdIfAvailable(activity, waitForLoad = true) {}
                } else {
                    appOpenAdManager.showAdIfAvailable(activity, waitForLoad = true)
                }
            } catch (t: Throwable) {
                Log.e("AppOpen", "Resume App Open show threw", t)
            }
        }
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        val isShowingAd = if (YandexADs.isRussian) {
            YandexADs.isShowingAppOpenAd
        } else {
            appOpenAdManager.isShowingAd
        }
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    // Splash activity isey call karegi
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        if (!isMainProcess) {
            onShowAdCompleteListener.onShowAdComplete()
            return
        }
        Log.d("AppOpen", "Splash show, russian=${YandexADs.isRussian}")
        try {
            if (YandexADs.isRussian) {
                YandexADs.showAppOpenAdIfAvailable(activity, waitForLoad = true) {
                    onShowAdCompleteListener.onShowAdComplete()
                }
            } else {
                appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener, waitForLoad = true)
            }
        } catch (t: Throwable) {
            Log.e("AppOpen", "Splash App Open show threw", t)
            onShowAdCompleteListener.onShowAdComplete()
        }
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false
        private var loadTime: Long = 0
        private val loadFinishedListeners = mutableListOf<() -> Unit>()

        /** Ad load karne ka function with callback support */
        fun loadAd(context: Context, onAdLoadFinished: (() -> Unit)? = null) {
            if (YandexADs.isRussian) {
                onAdLoadFinished?.invoke()
                return
            }
            if (isAdAvailable()) {
                onAdLoadFinished?.invoke()
                return
            }
            onAdLoadFinished?.let { loadFinishedListeners.add(it) }
            if (isLoadingAd) {
                Log.d("AppOpen", "AdMob App Open already loading — wait for it")
                return
            }

            isLoadingAd = true
            try {
                val request = AdRequest.Builder().build()
                val adUnitId = if (BuildConfig.DEBUG) {
                    context.getString(R.string.app_open_debug)
                } else {
                    context.getString(R.string.app_open)
                }
                Log.d("AppOpen", "Loading AdMob App Open $adUnitId")
                AppOpenAd.load(
                    context,
                    adUnitId,
                    request,
                    object : AppOpenAd.AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: AppOpenAd) {
                            appOpenAd = ad
                            isLoadingAd = false
                            loadTime = Date().time
                            Log.d("AppOpen", "AdMob App Open loaded")
                            notifyLoadFinished()
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isLoadingAd = false
                            Log.e("AppOpen", "AdMob App Open failed: ${loadAdError.message}")
                            notifyLoadFinished()
                        }
                    }
                )
            } catch (t: Throwable) {
                isLoadingAd = false
                Log.e("AppOpen", "AdMob App Open load threw", t)
                notifyLoadFinished()
            }
        }

        private fun notifyLoadFinished() {
            val listeners = loadFinishedListeners.toList()
            loadFinishedListeners.clear()
            listeners.forEach { it.invoke() }
        }

        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        fun showAdIfAvailable(activity: Activity, waitForLoad: Boolean = true) {
            showAdIfAvailable(activity, object : OnShowAdCompleteListener {
                override fun onShowAdComplete() {}
            }, waitForLoad)
        }

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener,
            waitForLoad: Boolean = true
        ) {
            try {
                if (isShowingAd) {
                    onShowAdCompleteListener.onShowAdComplete()
                    return
                }

                if (isAdAvailable()) {
                    startAdShowProcess(activity, onShowAdCompleteListener)
                    return
                }

                loadAd(activity) {
                    try {
                        if (waitForLoad && isAdAvailable()) {
                            startAdShowProcess(activity, onShowAdCompleteListener)
                        } else {
                            onShowAdCompleteListener.onShowAdComplete()
                        }
                    } catch (t: Throwable) {
                        Log.e("AppOpen", "AdMob App Open show-after-load threw", t)
                        onShowAdCompleteListener.onShowAdComplete()
                    }
                }
            } catch (t: Throwable) {
                Log.e("AppOpen", "AdMob App Open show threw", t)
                onShowAdCompleteListener.onShowAdComplete()
            }
        }

        private fun startAdShowProcess(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
            if (isShowingAd || activity.isFinishing || appOpenAd == null) {
                onShowAdCompleteListener.onShowAdComplete()
                return
            }
            try {
                isShowingAd = true
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        loadAd(activity)
                        onShowAdCompleteListener.onShowAdComplete()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        isShowingAd = false
                        Log.e("AppOpen", "AdMob App Open failed to show: ${adError.message}")
                        onShowAdCompleteListener.onShowAdComplete()
                    }

                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                        skipNextForeground = true
                        Log.d("AppOpen", "AdMob App Open shown")
                    }
                }
                Log.d("AppOpen", "Showing AdMob App Open")
                appOpenAd?.show(activity)
            } catch (t: Throwable) {
                appOpenAd = null
                isShowingAd = false
                Log.e("AppOpen", "AdMob App Open show threw", t)
                onShowAdCompleteListener.onShowAdComplete()
            }
        }
    }

    private fun isolateWebViewForThisProcess(context: Context) {
        try {
            val processName = resolveProcessName(context)
            isMainProcess = processName.isBlank() || processName == context.packageName
            if (!isMainProcess) {
                val suffix = processName.substringAfter(':', "secondary")
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "secondary" }
                WebView.setDataDirectorySuffix(suffix)
                Log.d("Application", "WebView suffix=$suffix process=$processName")
            }
        } catch (t: Throwable) {
            Log.e("Application", "WebView data dir isolate failed", t)
        }
    }

    private fun resolveProcessName(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getProcessName().orEmpty()
            } else {
                val pid = android.os.Process.myPid()
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                manager?.runningAppProcesses
                    ?.firstOrNull { it.pid == pid }
                    ?.processName
                    .orEmpty()
            }
        } catch (_: Throwable) {
            ""
        }
    }

    companion object {
        @JvmStatic
        @Volatile
        var isMainProcess: Boolean = true
            private set
    }
}