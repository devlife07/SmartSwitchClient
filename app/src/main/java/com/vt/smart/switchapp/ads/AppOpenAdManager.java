//package com.easy.clone.ads;
//
//import static androidx.lifecycle.Lifecycle.Event.ON_START;
//
//import android.app.Activity;
//import android.app.Application;
//import android.content.Context;
//import android.os.Bundle;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.lifecycle.LifecycleObserver;
//import androidx.lifecycle.OnLifecycleEvent;
//import androidx.lifecycle.ProcessLifecycleOwner;
//
//import com.easy.clone.utils.utilities.AppUtils;
//import com.easy.clone.utils.utilities.GlobalValues;
//import com.google.android.gms.ads.AdError;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.FullScreenContentCallback;
//import com.google.android.gms.ads.LoadAdError;
//import com.google.android.gms.ads.appopen.AppOpenAd;
//
//import java.util.Date;
//
///**
// * Prefetches App Open Ads.
// */
//public class AppOpenAdManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {
//    private static final String LOG_TAG = "AppOpenAdManager";
//    private static boolean isShowingAd = false;
//    private final Application myApplication;
//    private AppOpenAd appOpenAd = null;
//    private Activity currentActivity;
//    private Boolean isLoadingAd = false;
//    private long loadTime = 0;
//    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
//    private boolean isShowingAd1 = false;
//
//    /**
//     * Constructor
//     */
//    public AppOpenAdManager(Application myApplication) {
//        this.myApplication = myApplication;
//        this.myApplication.registerActivityLifecycleCallbacks(this);
//        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
//    }
//
//    /**
//     * LifecycleObserver methods
//     */
//    @OnLifecycleEvent(ON_START)
//    public void onStart() {
//        if(AppUtils.isConsentGranted.getValue()==true && GlobalValues.INSTANCE.isProVersion().getValue()==false) {
////            showAdIfAvailable();
//        }
//    }
//
//    /**
//     * Request an ad
//     */
//    private void fetchAd(Context context) {
//        // Do not load ad if there is an unused ad or one is already loading.
//        if (isLoadingAd || isAdAvailable() || appOpenAd!=null ) {
//            return;
//        }
//
//        isLoadingAd = true;
//        if(GlobalValues.INSTANCE.isProVersion().getValue()==false){
//            AdRequest request = new AdRequest.Builder().build();
//            String id = "ca-app-pub-29243860033380/512143022";
//     /*   if (BuildConfig.DEBUG)
//            id = context.getString(R.string.app_open);
//        else
//            id = context.getString(R.string.app_open);*/
//            AppOpenAd.load(
//                    context,
//                    id,
//                    request,
//                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
//                    new AppOpenAd.AppOpenAdLoadCallback() {
//                        /**
//                         * Called when an app open ad has loaded.
//                         *
//                         * @param ad the loaded app open ad.
//                         */
//
//                        @Override
//                        public void onAdLoaded(AppOpenAd ad) {
//                            appOpenAd = ad;
//                            isLoadingAd = false;
//                            loadTime = (new Date()).getTime();
//                            Log.d(LOG_TAG, "onAdLoaded.");
//                            if (!isShowingAd1) {
//                                if(AppUtils.isConsentGranted.getValue()==true) {
////                                    showAdIfAvailable1();
//                                }
//                            }
//                        }
//                        @Override
//                        public void onAdFailedToLoad(LoadAdError loadAdError) {
//                            isLoadingAd = false;
//                            Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
//                        }
//                    });
//        }
//
//    }
//
//    private void showAdIfAvailable() {
//        // Only show ad if there is not already an app open ad currently showing
//        // and an ad is available.
//        if (!isShowingAd && isAdAvailable()) {
//            Log.d(LOG_TAG, "Will show ad.");
//
//            FullScreenContentCallback fullScreenContentCallback =
//                    new FullScreenContentCallback() {
//                        @Override
//                        public void onAdDismissedFullScreenContent() {
//                            // Set the reference to null so isAdAvailable() returns false.
//                            AppOpenAdManager.this.appOpenAd = null;
//                            isShowingAd = false;
//                            fetchAd(currentActivity);
//                        }
//
//                        @Override
//                        public void onAdFailedToShowFullScreenContent(AdError adError) {
//                            Log.e("testtag", "onAdFailedToShowFullScreenContent: " + adError.getMessage());
//                        }
//
//                        @Override
//                        public void onAdShowedFullScreenContent() {
//                            isShowingAd = true;
//                        }
//                    };
//
//            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
//            appOpenAd.show(currentActivity);
//
//        } else {
//            Log.d(LOG_TAG, "Can not show ad.");
//            fetchAd(currentActivity);
//        }
//    }
//
//    public void showAdIfAvailable1() {
//        // Only show ad if there is not already an app open ad currently showing
//        // and an ad is available.
//        if (!isShowingAd && isAdAvailable()) {
//            Log.d(LOG_TAG, "Will show ad.");
//
//            FullScreenContentCallback fullScreenContentCallback =
//                    new FullScreenContentCallback() {
//                        @Override
//                        public void onAdDismissedFullScreenContent() {
//                            // Set the reference to null so isAdAvailable() returns false.
//                            AppOpenAdManager.this.appOpenAd = null;
//                            isShowingAd1 = true;
//                            fetchAd(currentActivity);
//                        }
//
//                        @Override
//                        public void onAdFailedToShowFullScreenContent(AdError adError) {
//                            Log.e("testtag", "onAdFailedToShowFullScreenContent: " + adError.getMessage());
//                        }
//
//                        @Override
//                        public void onAdShowedFullScreenContent() {
//                            isShowingAd1 = true;
//                        }
//                    };
//
//            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
//            appOpenAd.show(currentActivity);
//
//        } else {
//            Log.d(LOG_TAG, "Can not show ad.");
//            fetchAd(currentActivity);
//        }
//    }
//
//
//    /**
//     * Creates and returns ad request.
//     */
//    private AdRequest getAdRequest() {
//        return new AdRequest.Builder().build();
//
//
//    }
//
//    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
//        long dateDifference = (new Date()).getTime() - this.loadTime;
//        long numMilliSecondsPerHour = 3600000;
//        return (dateDifference < (numMilliSecondsPerHour * numHours));
//    }
//
//    /**
//     * Utility method that checks if ad exists and can be shown.
//     */
//    public boolean isAdAvailable() {
//        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
//    }
//
//    @Override
//    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
//
//    }
//
//    @Override
//    public void onActivityStarted(@NonNull Activity activity) {
//        currentActivity = activity;
//    }
//
//    @Override
//    public void onActivityResumed(@NonNull Activity activity) {
//        currentActivity = activity;
//    }
//
//    @Override
//    public void onActivityPaused(@NonNull Activity activity) {
//
//    }
//
//    @Override
//    public void onActivityStopped(@NonNull Activity activity) {
//
//    }
//
//    @Override
//    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
//
//    }
//
//    @Override
//    public void onActivityDestroyed(@NonNull Activity activity) {
//        currentActivity = null;
//    }
//}