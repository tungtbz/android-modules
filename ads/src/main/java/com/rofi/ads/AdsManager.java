package com.rofi.ads;

import android.app.Activity;

public class AdsManager {
    private final String TAG = "AdsManager";
    private static AdsManager mInstance = null;
    private IAdsService _adsService;
    private AdsEventListener _adsEventListener;

    public void Init(Activity activity, IAdsService adsService) {
        _adsService = adsService;
        _adsEventListener = new AdsEventListener() {
            @Override
            public void onVideoRewardLoaded() {

            }

            @Override
            public void onVideoRewardRewarded() {

            }

            @Override
            public void onVideoRewardUserRewarded() {

            }

            @Override
            public void onInterLoaded() {

            }

            @Override
            public void onInterDisplayed() {

            }

            @Override
            public void onAdClicked() {

            }
        };
        _adsService.SetEventListener(_adsEventListener);
        _adsService.Init(activity);
    }

    public boolean IsRewardReady() {
        return _adsService.IsRewardReady();
    }
}
