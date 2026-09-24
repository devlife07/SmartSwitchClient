package com.vt.smart.switchapp.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.vt.smart.switchapp.BuildConfig
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.utils.utilities.BillingManager
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import com.yandex.mobile.ads.nativeads.template.NativeBannerView
import java.util.Date

object YandexADs {
    private const val PREFS_NAME = "yandex_ads_prefs"
    private const val KEY_IS_RUSSIAN = "is_russian"

    var isRussian: Boolean = false
    private var isSdkInitialized = false
    var interstitialAd: InterstitialAd? = null
    var yandexNativeAd: NativeAd? = null
    var nativeId:String="R-M-18452785-4"
    var interstitialAdLoader: InterstitialAdLoader? = null

    fun interstitialAdUnitId(context: Context): String =
        if (BuildConfig.DEBUG) context.getString(R.string.interstitialYandexTest)
        else context.getString(R.string.interstitialYandex)

    fun bannerAdUnitId(context: Context): String =
        if (BuildConfig.DEBUG) context.getString(R.string.bannerYandexTest)
        else context.getString(R.string.bannerYandex)

    fun appOpenAdUnitId(context: Context): String =
        if (BuildConfig.DEBUG) context.getString(R.string.appOpenYandexTest)
        else context.getString(R.string.appOpenYandex)

    fun nativeAdUnitId(context: Context): String =
        if (BuildConfig.DEBUG) context.getString(R.string.nativeYandexTest)
        else context.getString(R.string.nativeYandex)

    private var appOpenAd: AppOpenAd? = null
    private var appOpenAdLoader: AppOpenAdLoader? = null
    private var isLoadingAppOpenAd = false
    var isShowingAppOpenAd = false
    private var appOpenLoadTime: Long = 0
    var onAppOpenShown: (() -> Unit)? = null
    private val appOpenLoadFinishedListeners = mutableListOf<() -> Unit>()

    private fun isUserSubscribed(): Boolean = BillingManager.isSubscribed.value

    fun restoreRussianFlag(context: Context) {
        isRussian = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_RUSSIAN, false)
    }

    fun setRussianUser(context: Context, russian: Boolean) {
        isRussian = russian
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_RUSSIAN, russian)
            .apply()
    }

    fun initializeYandex(context: Context, onInitialized: (() -> Unit)? = null) {
        if (!com.vt.smart.switchapp.application.Application.isMainProcess) {
            onInitialized?.invoke()
            return
        }
        if (isSdkInitialized) {
            onInitialized?.invoke()
            return
        }
        try {
            MobileAds.initialize(context.applicationContext) {
                isSdkInitialized = true
                Log.d("YandexAd", "SDK initialized")
                Handler(Looper.getMainLooper()).post {
                    onInitialized?.invoke()
                }
            }
        } catch (t: Throwable) {
            Log.e("YandexAd", "Yandex MobileAds.initialize failed", t)
            onInitialized?.invoke()
        }
    }

    fun loadAppOpenAd(context: Context, onAdLoadFinished: (() -> Unit)? = null) {
        if (isUserSubscribed() || isAppOpenAdAvailable()) {
            onAdLoadFinished?.invoke()
            return
        }
        onAdLoadFinished?.let { appOpenLoadFinishedListeners.add(it) }
        if (isLoadingAppOpenAd) {
            Log.d("YandexAd", "App Open already loading — wait for it")
            return
        }
        isLoadingAppOpenAd = true
        try {
            appOpenAdLoader = AppOpenAdLoader(context.applicationContext).apply {
                setAdLoadListener(object : AppOpenAdLoadListener {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAppOpenAd = false
                        appOpenLoadTime = Date().time
                        Log.d("YandexAd", "App Open loaded successfully")
                        notifyAppOpenLoadFinished()
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        isLoadingAppOpenAd = false
                        Log.e("YandexAd", "App Open failed: ${error.description}")
                        notifyAppOpenLoadFinished()
                    }
                })
            }
            val adRequestConfiguration =
                AdRequestConfiguration.Builder(appOpenAdUnitId(context)).build()
            appOpenAdLoader?.loadAd(adRequestConfiguration)
        } catch (t: Throwable) {
            Log.e("YandexAd", "App Open load threw", t)
            isLoadingAppOpenAd = false
            notifyAppOpenLoadFinished()
        }
    }

    private fun notifyAppOpenLoadFinished() {
        val listeners = appOpenLoadFinishedListeners.toList()
        appOpenLoadFinishedListeners.clear()
        listeners.forEach { it.invoke() }
    }

    fun showAppOpenAdIfAvailable(
        activity: Activity,
        waitForLoad: Boolean = true,
        onComplete: () -> Unit
    ) {
        try {
            if (isUserSubscribed() || isShowingAppOpenAd) {
                if (!isShowingAppOpenAd) onComplete()
                return
            }

            if (isAppOpenAdAvailable()) {
                startAppOpenAdShow(activity, onComplete)
                return
            }

            loadAppOpenAd(activity) {
                try {
                    if (waitForLoad && isAppOpenAdAvailable()) {
                        startAppOpenAdShow(activity, onComplete)
                    } else {
                        onComplete()
                    }
                } catch (t: Throwable) {
                    Log.e("YandexAd", "App Open show-after-load threw", t)
                    onComplete()
                }
            }
        } catch (t: Throwable) {
            Log.e("YandexAd", "App Open show threw", t)
            try {
                onComplete()
            } catch (_: Throwable) {
            }
        }
    }

    private fun wasAppOpenLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - appOpenLoadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    private fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasAppOpenLoadTimeLessThanNHoursAgo(4)
    }

    private fun startAppOpenAdShow(activity: Activity, onComplete: () -> Unit) {
        val ad = appOpenAd
        if (ad == null || activity.isFinishing) {
            onComplete()
            return
        }
        try {
            ad.setAdEventListener(object : AppOpenAdEventListener {
                override fun onAdShown() {
                    isShowingAppOpenAd = true
                    onAppOpenShown?.invoke()
                    Log.d("YandexAd", "App Open shown")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    clearAppOpenAd()
                    isShowingAppOpenAd = false
                    Log.e("YandexAd", "App Open failed to show: ${adError.description}")
                    onComplete()
                }

                override fun onAdDismissed() {
                    clearAppOpenAd()
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                    onComplete()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            ad.show(activity)
        } catch (t: Throwable) {
            clearAppOpenAd()
            isShowingAppOpenAd = false
            Log.e("YandexAd", "App Open show threw", t)
            onComplete()
        }
    }

    private fun clearAppOpenAd() {
        appOpenAd?.setAdEventListener(null)
        appOpenAd = null
    }
    //Banner Yandex
    fun showBannerAd(context: Context, container: LinearLayout) {
        try {
            if (isUserSubscribed()) {
                destroyBannerIn(container)
                container.visibility = View.GONE
                return
            }
            val activity = context as? Activity ?: return
            if (activity.isFinishing || activity.isDestroyed) return

            initializeYandex(activity) {
                if (activity.isFinishing || activity.isDestroyed) return@initializeYandex
                container.post {
                    if (activity.isFinishing || activity.isDestroyed) return@post
                    try {
                        loadYandexBanner(activity, container)
                    } catch (t: Throwable) {
                        Log.e("YandexAd", "Banner setup failed", t)
                        destroyBannerIn(container)
                        container.visibility = View.GONE
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("YandexAd", "Banner request threw", t)
            try {
                container.visibility = View.GONE
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadYandexBanner(activity: Activity, container: LinearLayout) {
        destroyBannerIn(container)
        container.visibility = View.VISIBLE
        val params = container.layoutParams
        if (params != null && params.height > 0) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            container.layoutParams = params
        }

        val density = activity.resources.displayMetrics.density
        val screenWidthDp = (activity.resources.displayMetrics.widthPixels / density).toInt()
            .coerceAtLeast(1)
        val containerWidthDp = if (container.width > 0) {
            (container.width / density).toInt()
        } else {
            screenWidthDp
        }
        val adWidthDp = minOf(containerWidthDp, screenWidthDp).coerceAtLeast(1)
        Log.d("YandexAd", "Loading banner widthDp=$adWidthDp screenDp=$screenWidthDp")

        val bannerAdView = BannerAdView(activity)
        bannerAdView.setAdUnitId(bannerAdUnitId(activity))
        bannerAdView.setAdSize(BannerAdSize.stickySize(activity, adWidthDp))
        bannerAdView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bannerAdView.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {
                container.visibility = View.VISIBLE
                Log.d("YandexAd", "Banner loaded successfully")
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.e("YandexAd", "Banner failed: ${error.description}")
                destroyBannerIn(container)
                container.visibility = View.GONE
            }

            override fun onAdClicked() {}
            override fun onLeftApplication() {}
            override fun onReturnedToApplication() {}
            override fun onImpression(data: ImpressionData?) {}
        })
        container.addView(bannerAdView)
        bannerAdView.loadAd(AdRequest.Builder().build())
    }

    private fun destroyBannerIn(container: LinearLayout) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is BannerAdView) {
                try {
                    child.setBannerAdEventListener(null)
                    child.destroy()
                } catch (t: Throwable) {
                    Log.e("YandexAd", "Banner destroy failed", t)
                }
            }
        }
        container.removeAllViews()
    }
     fun loadInterstitialAd(context: Context) {
        try {
            if (interstitialAd == null && !isUserSubscribed()) {
                interstitialAdLoader = InterstitialAdLoader(context).apply {
                    setAdLoadListener(object : InterstitialAdLoadListener {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                        }
                        override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                            loadInterstitialAdSecond(context)
                        }
                    })
                }
                val adRequestConfiguration =
                    AdRequestConfiguration.Builder(interstitialAdUnitId(context)).build()
                interstitialAdLoader?.loadAd(adRequestConfiguration)
            }
        } catch (t: Throwable) {
            Log.e("YandexAd", "Interstitial load threw", t)
            interstitialAd = null
        }
    }

     fun loadInterstitialAdSecond(context: Context) {
        try {
            if (interstitialAd == null && !isUserSubscribed()) {
                interstitialAdLoader = InterstitialAdLoader(context).apply {
                    setAdLoadListener(object : InterstitialAdLoadListener {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                        }
                        override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                        }
                    })
                }
                val adRequestConfiguration =
                    AdRequestConfiguration.Builder(interstitialAdUnitId(context)).build()
                interstitialAdLoader?.loadAd(adRequestConfiguration)
            }
        } catch (t: Throwable) {
            Log.e("YandexAd", "Interstitial second load threw", t)
            interstitialAd = null
        }
    }

     fun showYandexAd(activity: Activity, onContinue: () -> Unit) {
         try {
             if (interstitialAd != null && !isUserSubscribed()) {
                 interstitialAd?.apply {
                     setAdEventListener(object : InterstitialAdEventListener {
                         override fun onAdShown() {}
                         override fun onAdFailedToShow(adError: AdError) {
                             onContinue()
                             interstitialAd?.setAdEventListener(null)
                             interstitialAd = null
                             loadInterstitialAd(activity)
                         }
                         override fun onAdDismissed() {
                             onContinue()
                             interstitialAd?.setAdEventListener(null)
                             interstitialAd = null
                             loadInterstitialAd(activity)
                         }
                         override fun onAdClicked() {}
                         override fun onAdImpression(impressionData: ImpressionData?) {}
                     })
                     show(activity)
                 }
             } else {
                 onContinue()
             }
         } catch (t: Throwable) {
             Log.e("YandexAd", "Interstitial show threw", t)
             interstitialAd = null
             try {
                 onContinue()
             } catch (_: Throwable) {
             }
         }
    }

     fun showSplashYandexAd(activity: Activity, onContinue: () -> Unit) {
         try {
             if (interstitialAd != null && !isUserSubscribed()) {
                 interstitialAd?.apply {
                     setAdEventListener(object : InterstitialAdEventListener {
                         override fun onAdShown() {}
                         override fun onAdFailedToShow(adError: AdError) {
                             onContinue()
                             interstitialAd?.setAdEventListener(null)
                             interstitialAd = null
                             loadInterstitialAd(activity)
                         }
                         override fun onAdDismissed() {
                             onContinue()
                             interstitialAd?.setAdEventListener(null)
                             interstitialAd = null
                             loadInterstitialAd(activity)
                         }
                         override fun onAdClicked() {}
                         override fun onAdImpression(impressionData: ImpressionData?) {}
                     })
                     show(activity)
                 }
             } else {
                 onContinue()
             }
         } catch (t: Throwable) {
             Log.e("YandexAd", "Splash interstitial show threw", t)
             interstitialAd = null
             try {
                 onContinue()
             } catch (_: Throwable) {
             }
         }
    }

    //Native Ads

    fun loadYandexNativeAd(container: ViewGroup, context: Context) {
        if (isUserSubscribed()) {
            container.visibility = View.GONE
            return
        }
        initializeYandex(context) {
            try {
                if (yandexNativeAd != null && bindYandexNativeAd(container, yandexNativeAd!!)) {
                    return@initializeYandex
                }
                yandexNativeAd = null
                val adLoader = NativeAdLoader(context)
                adLoader.setNativeAdLoadListener(object : NativeAdLoadListener {
                    override fun onAdLoaded(ad: NativeAd) {
                        yandexNativeAd = ad
                        if (!bindYandexNativeAd(container, ad)) {
                            yandexNativeAd = null
                        }
                        Log.d("YandexAd", "Native loaded")
                    }
                    override fun onAdFailedToLoad(error: AdRequestError) {
                        yandexNativeAd = null
                        container.visibility = View.GONE
                        Log.e("YandexAd", "Native failed: ${error.description}")
                    }
                })
                val requestConfig = NativeAdRequestConfiguration.Builder(nativeAdUnitId(context)).build()
                adLoader.loadAd(requestConfig)
            } catch (t: Throwable) {
                Log.e("YandexAd", "Native request threw", t)
                container.visibility = View.GONE
            }
        }
    }

    private fun bindYandexNativeAd(container: ViewGroup, ad: NativeAd): Boolean {
        try {
            val adView = LayoutInflater.from(container.context)
                .inflate(R.layout.yandex_native_small, container, false) as NativeAdView
            val binder = NativeAdViewBinder.Builder(adView)
                .setTitleView(adView.findViewById(R.id.yandex_native_title))
                .setBodyView(adView.findViewById(R.id.yandex_native_body))
                .setIconView(adView.findViewById(R.id.yandex_native_icon))
                .setCallToActionView(adView.findViewById(R.id.yandex_native_cta))
                .setMediaView(adView.findViewById<MediaView>(R.id.yandex_native_media))
                .setSponsoredView(adView.findViewById(R.id.yandex_native_sponsored))
                .setDomainView(adView.findViewById(R.id.yandex_native_domain))
                .setWarningView(adView.findViewById(R.id.yandex_native_warning))
                .build()
            container.removeAllViews()
            container.addView(
                adView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            ad.bindNativeAd(binder)
            container.visibility = View.VISIBLE
            Log.d("YandexAd", "Native bound to compact layout")
            return true
        } catch (t: Throwable) {
            Log.e("YandexAd", "Compact native bind failed, using template", t)
            return bindYandexNativeFallback(container, ad)
        }
    }

    private fun bindYandexNativeFallback(container: ViewGroup, ad: NativeAd): Boolean {
        return try {
            val nativeBannerView = NativeBannerView(container.context)
            nativeBannerView.setAd(ad)
            container.removeAllViews()
            container.addView(
                nativeBannerView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            container.visibility = View.VISIBLE
            Log.d("YandexAd", "Native bound to template fallback")
            true
        } catch (t: Throwable) {
            Log.e("YandexAd", "Native fallback bind failed", t)
            container.visibility = View.GONE
            false
        }
    }

}