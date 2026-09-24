package com.vt.smart.switchapp.ads;

import static com.google.android.gms.ads.AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize;
import static com.vt.smart.switchapp.AppUtils.adCounter;
import static com.vt.smart.switchapp.ads.AdManager.AdMobInterId;
import static com.vt.smart.switchapp.ads.Adunit_IDs.AdMobInterIdTest;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.vt.smart.switchapp.AppUtils;
import com.vt.smart.switchapp.BuildConfig;
import com.vt.smart.switchapp.utils.utilities.GlobalValues;

import org.jetbrains.annotations.NotNull;

public class ManageAdsUnit {
    //  private String TAG = getClass().getCanonicalName();
    private String TAG = "TESTAG";
    private static ManageAdsUnit mInstance;

    public static volatile InterstitialAd mInterstitialAd;

    public static ManageAdsUnit getInstance() {
        if (mInstance == null) {
            mInstance = new ManageAdsUnit();
        }
        return mInstance;
    }

    public void initializeAds(Context context) {
        if (!com.vt.smart.switchapp.application.Application.isMainProcess()) {
            return;
        }
        try {
            if (AppUtils.INSTANCE.verifyAppInstallation(context) && !Boolean.TRUE.equals(GlobalValues.INSTANCE.isProVersion().getValue())) {
                MobileAds.initialize(context);
            }
        } catch (Throwable t) {
            Log.e("ADMOBS", "MobileAds.initialize failed", t);
        }
    }

    public void showAdMobBanner(Context context, Activity activity, LinearLayout bannerLayout) {
        if(!YandexADs.INSTANCE.isRussian()) {
            try {
                if (/*AppUtils.INSTANCE.verifyAppInstallation(context) &&*/ GlobalValues.INSTANCE.isProVersion().getValue() != true) {
                    bannerLayout.setGravity(Gravity.CENTER);
                    bannerLayout.setVisibility(View.VISIBLE);
                    AdView adView = new AdView(activity);
                    adView.setAdSize(getSizeOfBanner(activity));
                    if (BuildConfig.DEBUG) {
                        adView.setAdUnitId(Adunit_IDs.AdMobBannerIdTest);
                    } else {
                        adView.setAdUnitId(Adunit_IDs.AdMobBannerId);
                    }

                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);
                    bannerLayout.addView(adView);

                    adView.setAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            Log.e(TAG, "onAdClosed: ");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull @NotNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            Log.e(TAG, "onAdFailedToLoad: " + loadAdError);
                        }

                        @Override
                        public void onAdOpened() {
                            super.onAdOpened();
                            Log.e(TAG, "onAdOpened: ");
                        }

                        @Override
                        public void onAdLoaded() {
                            super.onAdLoaded();
                            Log.e(TAG, "onAdLoaded: ");
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            Log.e(TAG, "onAdClicked: ");
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            Log.e(TAG, "onAdImpression: ");
                        }
                    });
                }
            } catch (Throwable t) {
                Log.e(TAG, "Banner request threw", t);
                try {
                    bannerLayout.setVisibility(View.GONE);
                } catch (Exception ignored) {
                    Log.e(TAG, "Banner request threw", ignored);
                    bannerLayout.setVisibility(View.GONE);
                }
            }
        }
        else{
            try {
                YandexADs.INSTANCE.showBannerAd(context,bannerLayout);
            } catch (Throwable t) {
                Log.e(TAG, "Yandex banner threw", t);
            }
        }
    }
    public void loadAdMobInterstitialAds(Context mContext,String adUnitId) {
        if(!YandexADs.INSTANCE.isRussian()){
            try {
                if (GlobalValues.INSTANCE.isProVersion().getValue()!=true && mInterstitialAd==null) {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    String adID = BuildConfig.DEBUG ? adUnitId : adUnitId;
                    InterstitialAd.load(mContext, adID, adRequest, new InterstitialAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            Log.e(TAG, "add loaded");
                            super.onAdLoaded(interstitialAd);
                            mInterstitialAd = interstitialAd;
    //                        Toast.makeText(mContext,"Inter Ad Loaded",Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            mInterstitialAd = null;
    //                        Toast.makeText(mContext,"Inter Ad Failed",Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onAdFailedToLoad: Inter" + loadAdError);
                        }
                    });
                }
                else{
    //            Toast.makeText(mContext,"ByPass Inter request",Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                mInterstitialAd = null;
                Log.e(TAG, "Interstitial load threw", t);
            }
        }
        else{
            try {
                YandexADs.INSTANCE.loadInterstitialAd(mContext);
            } catch (Throwable t) {
                Log.e(TAG, "Yandex interstitial load threw", t);
            }
        }
    }
    public interface Listener {
        public void intersitialAdClosedCallback();
    }
    private AdSize getSizeOfBanner(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        return getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }
}
