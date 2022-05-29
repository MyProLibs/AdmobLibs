package com.common.control.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.common.control.R;
import com.common.control.dialog.PrepareLoadingAdsDialog;
import com.common.control.interfaces.AdCallback;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

public class AdmobManager {
    private static final String TAG = AdmobManager.class.getName();
    private static AdmobManager instance;
    private final Handler handler = new Handler();
    private PrepareLoadingAdsDialog dialog;
    private boolean errorWhenInit;

    public static AdmobManager getInstance() {
        if (instance == null) {
            instance = new AdmobManager();
        }
        return instance;
    }

    private AdmobManager() {

    }

    public void init(Context context, String deviceID) {
        try {
            if (PurchaseManager.getInstance().isPremium(context)) {
                errorWhenInit = true;
                return;
            }
            MobileAds.initialize(context, initializationStatus -> {
            });
            MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                    .setTestDeviceIds(Collections.singletonList(deviceID)).build());
        } catch (Exception e) {
            errorWhenInit = true;
            e.printStackTrace();
        }
    }

    public AdRequest getAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        return builder.build();
    }

    public void destroyTimeout() {
        handler.removeCallbacksAndMessages(null);
    }

    public void loadInterAds(Activity context, String id, AdCallback callback) {
        if (errorWhenInit || PurchaseManager.getInstance().isPremium(context)) {
            if (callback != null) {
                callback.onAdFailedToLoad(null);
            }
            return;
        }
        InterstitialAd.load(context, id, getAdRequest(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                if (callback != null) {
                    callback.onAdFailedToLoad(loadAdError);
                }
                Log.d("TAG2", "onAdFailedToLoad: ");
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                if (callback != null) {
                    callback.interCallback(interstitialAd);
                }
                Log.d("TAG2", "onAdLoaded: ");
            }
        });
    }

    public void showInterstitial(final Activity context, final InterstitialAd mInterstitialAd, final AdCallback callback) {
        showInterstitial(context, mInterstitialAd, callback, true);
    }

    private void showInterstitial(final Activity context, final InterstitialAd mInterstitialAd, final AdCallback callback, final boolean shouldReloadAds) {
        if (errorWhenInit || PurchaseManager.getInstance().isPremium(context)) {
            if (callback != null) {
                callback.onAdClosed();
            }
            return;
        }
        if (mInterstitialAd == null) {
            if (callback != null) {
                callback.onAdFailedToLoad(null);
            }
            return;
        }
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                Log.d("TAG", "The ad was dismissed.");
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (AppOpenManager.getInstance().isInitialized()) {
                    AppOpenManager.getInstance().enableAppResume();
                    Log.d(TAG, "enableAppResume: ");
                }
                if (callback != null) {
                    callback.onAdClosed();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (AppOpenManager.getInstance().isInitialized()) {
                    AppOpenManager.getInstance().enableAppResume();
                    Log.d(TAG, "enableAppResume: ");
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
                Log.d("TAG", "The ad failed to show.");
                if (callback != null) {
                    callback.onAdFailedToLoad(null);
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d("TAG", "The ad was shown.");
            }
        });

        showInterstitialAd(context, mInterstitialAd, callback);
    }

    private void showInterstitialAd(Activity context, final InterstitialAd mInterstitialAd, AdCallback callback) {
        if (errorWhenInit) {
            if (callback != null) {
                callback.onAdFailedToLoad(null);
            }
            return;
        }
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
                && mInterstitialAd != null) {
            try {
                dialog = new PrepareLoadingAdsDialog(context);
                dialog.show();
            } catch (Exception e) {
                dialog = null;
                e.printStackTrace();
            }
            if (AppOpenManager.getInstance().isInitialized()) {
                AppOpenManager.getInstance().disableAppResume();
                Log.d(TAG, "disableAppResume: ");
            }
            if (context == null) {
                if (callback != null) {
                    callback.onAdClosed();
                }
                return;
            }
            new Handler(context.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInterstitialAd.show(context);
                    if (dialog != null) {
                        dialog.clearTextad();
                    }
                }
            }, 1500);
//            if (dialog != null) {
//                dialog.dismiss();
//            }
        } else {
            if (callback != null) {
                callback.onAdClosed();
            }
        }
    }

    public void loadBanner(final Activity mActivity, String id) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container);
        loadBanner(mActivity, id, adContainer, containerShimmer);
    }

    private void loadBanner(final Activity mActivity, String id, final FrameLayout adContainer, final ShimmerFrameLayout containerShimmer) {
        if (errorWhenInit || PurchaseManager.getInstance().isPremium(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        try {
            AdView adView = new AdView(mActivity);
            adView.setAdUnitId(id);
            adContainer.addView(adView);
            AdSize adSize = getAdSize(mActivity);
            adView.setAdSize(adSize);
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            adView.loadAd(getAdRequest());
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    containerShimmer.stopShimmer();
                    adContainer.setVisibility(View.GONE);
                    containerShimmer.setVisibility(View.GONE);
                }


                @Override
                public void onAdLoaded() {
                    containerShimmer.stopShimmer();
                    containerShimmer.setVisibility(View.GONE);
                    adContainer.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AdSize getAdSize(Activity mActivity) {
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth);

    }

    public void loadNative(Context context, String id, FrameLayout placeHolder) {
        loadNative(context, id, placeHolder, R.layout.custom_native);
    }

    public void loadNative(Context context, String id, FrameLayout placeHolder, int customNative) {
        loadUnifiedNativeAd(context, id, new AdCallback() {
            @Override
            public void onNativeAds(NativeAd nativeAd) {
                @SuppressLint("InflateParams")
                NativeAdView nativeAdView =
                        (NativeAdView) LayoutInflater.from(context).inflate(
                                customNative,
                                null
                        );
                populateUnifiedNativeAdView(nativeAd, nativeAdView);
                placeHolder.removeAllViews();
                placeHolder.addView(nativeAdView);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError i) {
                placeHolder.setVisibility(View.GONE);
            }
        });
    }

    private void loadUnifiedNativeAd(Context context, String id, final AdCallback callback) {
        if (callback == null) {
            return;
        }
        if (errorWhenInit || PurchaseManager.getInstance().isPremium(context)) {
            callback.onAdFailedToLoad(null);
            return;
        }
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();
        AdLoader adLoader = new AdLoader.Builder(context, id)
                .forNativeAd(callback::onNativeAds)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        callback.onAdFailedToLoad(loadAdError);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();
        adLoader.loadAd(getAdRequest());
    }


    private void populateUnifiedNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        try {
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            if (nativeAd.getCallToAction() == null) {
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            adView.setIconView(adView.findViewById(R.id.ad_app_icon));
            if (nativeAd.getIcon() == null) {
                adView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(
                        nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            if (nativeAd.getPrice() == null) {
//                adView.getPriceView().setVisibility(View.INVISIBLE);
//            } else {
//                adView.getPriceView().setVisibility(View.VISIBLE);
//                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            if (nativeAd.getStore() == null) {
//                adView.getStoreView().setVisibility(View.INVISIBLE);
//            } else {
//                adView.getStoreView().setVisibility(View.VISIBLE);
//                ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
            if (nativeAd.getStarRating() == null) {
                adView.getStarRatingView().setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) adView.getStarRatingView())
                        .setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adView.setNativeAd(nativeAd);

    }

    @SuppressLint("HardwareIds")
    public String getDeviceId(Context context) {
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return md5(android_id).toUpperCase();
    }

    private String md5(final String s) {
        try {
            MessageDigest digest = MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString;
            hexString = new StringBuilder();
            for (byte b : messageDigest) {
                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & b));
                while (h.length() < 2)
                    h.insert(0, "0");
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException ignored) {
        }
        return "";
    }

    public void isPurchased(boolean purchased) {
        PurchaseManager.getInstance().setPurchased(purchased);
    }
}
