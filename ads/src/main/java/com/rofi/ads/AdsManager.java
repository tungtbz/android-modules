package com.rofi.ads;

import android.app.Activity;

public class AdsManager {
    private static AdsManager mInstace = null;
    private final String TAG = "AdsManager";
    private static AdsManager mInstance = null;
    private IAdsService _adsService;


    public static AdsManager getInstance() {
        if (null == mInstace) {
            mInstace = new AdsManager();
        }
        return mInstace;
    }

    public void Init(Activity activity, IAdsService adsService) {
        _adsService = adsService;
    }

    public boolean IsRewardReady() {
        return _adsService.IsRewardReady();
    }
}
