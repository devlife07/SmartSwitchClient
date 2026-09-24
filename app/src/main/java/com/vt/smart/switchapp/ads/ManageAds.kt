package com.vt.smart.switchapp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.vt.smart.switchapp.AppUtils
import com.vt.smart.switchapp.BuildConfig
import com.vt.smart.switchapp.R
import com.vt.smart.switchapp.ads.YandexADs.loadYandexNativeAd
import com.vt.smart.switchapp.ads.nativeAd.TemplateView
import com.vt.smart.switchapp.utils.utilities.BillingManager

object ManageAds {

    var mInterstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun initializeAds(context: Context) {
        if (!com.vt.smart.switchapp.application.Application.isMainProcess) return
        try {
            MobileAds.initialize(context)
        } catch (t: Throwable) {
            Log.e("ADMOBS", "MobileAds.initialize failed", t)
        }
    }

    // ─── Helper: subscribed hai to ad skip karo ───────────────────────
    private fun isUserSubscribed(): Boolean = BillingManager.isSubscribed.value

    // ─── Interstitial Load ────────────────────────────────────────────
    fun loadAdMobInterstitialAds(context: Context) {
        try {
            if (!YandexADs.isRussian) {
                if (isUserSubscribed()) return
                if (mInterstitialAd != null || isLoading) return

                isLoading = true

                val adId = if (BuildConfig.DEBUG)
                    AdManager.AdMobInterIdTest
                else
                    AdManager.AdMobInterId

                InterstitialAd.load(
                    context,
                    adId,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {

                        override fun onAdLoaded(ad: InterstitialAd) {
                            mInterstitialAd = ad
                            isLoading = false
                            Log.d("ADMOBS", "✅ Interstitial Loaded")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            mInterstitialAd = null
                            isLoading = false
                            Log.e("ADMOBS", "❌ Load failed: ${error.message}")
                        }
                    }
                )
            } else {
                YandexADs.loadInterstitialAd(context)
            }
        } catch (t: Throwable) {
            isLoading = false
            mInterstitialAd = null
            Log.e("ADMOBS", "Interstitial load threw", t)
        }
    }

    fun showInterstitialAd(
        activity: Activity,
        onContinue: () -> Unit
    ) {
        try {
            if (!YandexADs.isRussian) {
                if (isUserSubscribed()) {
                    onContinue()
                    return
                }

                AppUtils.adCounter++

                if (AppUtils.adCounter < 2) {
                    onContinue()
                    return
                }

                AppUtils.adCounter = 0

                if (mInterstitialAd == null) {
                    loadAdMobInterstitialAds(activity.applicationContext)
                    onContinue()
                    return
                }

                mInterstitialAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {

                        override fun onAdDismissedFullScreenContent() {
                            mInterstitialAd = null
                            loadAdMobInterstitialAds(activity.applicationContext)
                            onContinue()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            mInterstitialAd = null
                            loadAdMobInterstitialAds(activity.applicationContext)
                            onContinue()
                        }
                    }

                mInterstitialAd?.show(activity)
            } else {
                YandexADs.showYandexAd(activity, onContinue)
            }
        } catch (t: Throwable) {
            mInterstitialAd = null
            Log.e("ADMOBS", "Interstitial show threw", t)
            try {
                onContinue()
            } catch (ignored: Throwable) {
            }
        }
    }

    // ─── Banner ───────────────────────────────────────────────────────
    fun showAdMobBanner(context: Context, activity: Activity, bannerLayout: LinearLayout) {
        if (isUserSubscribed()) {
            bannerLayout.removeAllViews()
            bannerLayout.visibility = android.view.View.GONE
            return
        }
        if (YandexADs.isRussian) {
            YandexADs.showBannerAd(context, bannerLayout)
            return
        }
        try {
            bannerLayout.removeAllViews()
            bannerLayout.gravity = Gravity.CENTER
            bannerLayout.visibility = android.view.View.VISIBLE

            val adView = AdView(activity)
            adView.adUnitId = if (BuildConfig.DEBUG) {
                activity.getString(R.string.bannerTest)
            } else {
                activity.getString(R.string.banner)
            }
            bannerLayout.addView(adView)

            val loadBanner = {
                val density = activity.resources.displayMetrics.density
                val containerWidth = bannerLayout.width.takeIf { it > 0 }
                    ?: activity.resources.displayMetrics.widthPixels
                val adWidth = (containerWidth / density).toInt().coerceAtLeast(320)
                adView.setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
                )
                adView.loadAd(AdRequest.Builder().build())
            }

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    bannerLayout.visibility = android.view.View.VISIBLE
                    Log.d("ADMOBS", "Banner loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    bannerLayout.visibility = android.view.View.GONE
                    Log.e("ADMOBS", "Banner failed: ${error.message}")
                }
            }

            if (bannerLayout.width > 0) {
                loadBanner()
            } else {
                bannerLayout.post {
                    try {
                        loadBanner()
                    } catch (t: Throwable) {
                        Log.e("ADMOBS", "Banner post-load threw", t)
                        bannerLayout.visibility = android.view.View.GONE
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("ADMOBS", "Banner request threw", t)
            bannerLayout.visibility = android.view.View.GONE
        }
    }

    // ─── Native Ad ────────────────────────────────────────────────────
    fun loadNativeAD(template: TemplateView, activity: Activity) {
        if (!YandexADs.isRussian) {
            // ✅ Subscribed = native ad hide karo
            if (isUserSubscribed()) {
                template.visibility = android.view.View.GONE
                return
            }

            try {
                val adId = if (BuildConfig.DEBUG) {
                    activity.getString(R.string.nativeAdTest)
                } else {
                    activity.getString(R.string.nativeAd)
                }

                val videoOptions = VideoOptions.Builder()
                    .setStartMuted(false)
                    .build()

                val adOptions = NativeAdOptions.Builder()
                    .setVideoOptions(videoOptions)
                    .build()

                val adLoader = AdLoader.Builder(activity, adId)
                    .forNativeAd { nativeAd ->
                        Log.d("TAG", "Native Ad Loaded")
                        if (activity.isDestroyed) {
                            nativeAd.destroy()
                            return@forNativeAd
                        }
                        // Video callbacks
                        nativeAd.mediaContent?.videoController?.videoLifecycleCallbacks =
                            object : VideoLifecycleCallbacks() {
                                override fun onVideoEnd() {
                                    super.onVideoEnd()
                                    Log.d("TAG", "Video Finished")
                                }
                            }
                        template.visibility = android.view.View.VISIBLE
                        template.setNativeAd(nativeAd)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d("TAG", "Native Ad Failed To Load")
                            template.visibility = android.view.View.GONE
                        }
                    })
                    .withNativeAdOptions(adOptions)
                    .build()

                adLoader.loadAd(AdRequest.Builder().build())

            } catch (t: Throwable) {
                Log.e("ADMOBS", "Native request threw", t)
                template.visibility = android.view.View.GONE
            }
        }
        else{
            loadYandexNativeAd(template, activity)
        }
    }

}