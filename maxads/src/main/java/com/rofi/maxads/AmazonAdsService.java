package com.rofi.maxads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdNetwork;
import com.amazon.device.ads.DTBAdNetworkInfo;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.amazon.device.ads.MRAIDPolicy;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.sdk.AppLovinSdkUtils;

public class AmazonAdsService {
    public void Init(Activity activity, String appId) {
        // Amazon requires an 'Activity' instance
        AdRegistration.getInstance(appId, activity);
        AdRegistration.setAdNetworkInfo(new DTBAdNetworkInfo(DTBAdNetwork.MAX));
        AdRegistration.setMRAIDSupportedVersions(new String[]{"1.0", "2.0", "3.0"});
        AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM);

        AdRegistration.enableTesting(true);
        AdRegistration.enableLogging(true);
    }

    public void loadBannerAd(Activity activity, String bannerId, DTBAdCallback dtbAdCallback) {
        String amazonAdSlotId;
        MaxAdFormat adFormat;

        amazonAdSlotId = bannerId;
        adFormat = MaxAdFormat.BANNER;

        // Raw size will be 320x50 for BANNERs on phones, and 728x90 for LEADERs on tablets
        AppLovinSdkUtils.Size rawSize = adFormat.getSize();
        DTBAdSize size = new DTBAdSize(rawSize.getWidth(), rawSize.getHeight(), amazonAdSlotId);

        DTBAdRequest adLoader = new DTBAdRequest();
        adLoader.setSizes(size);
        adLoader.loadAd(dtbAdCallback);
    }

    public void loadMRECAd(String amazonAdSlotId, DTBAdCallback callback) {
        DTBAdRequest adLoader = new DTBAdRequest();
        adLoader.setSizes(new DTBAdSize(300, 250, amazonAdSlotId));
        adLoader.loadAd(callback);
    }

    public void loadInterAd(String amazonAdSlotId, DTBAdCallback callback) {
        DTBAdRequest adLoader = new DTBAdRequest();
        adLoader.setSizes(new DTBAdSize.DTBInterstitialAdSize(amazonAdSlotId));
        adLoader.loadAd(callback);
    }


    public void loadRewardAd(String amazonAdSlotId, DTBAdCallback callback) {
        DTBAdRequest adLoader = new DTBAdRequest();
        adLoader.setSizes(new DTBAdSize.DTBVideo(320, 480, amazonAdSlotId));
        adLoader.loadAd(callback);
    }
}
