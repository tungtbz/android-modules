package com.rofi.ads;

import android.app.Activity;

public class AdsManager {
    private static AdsManager mInstace = null;
    private final String TAG = "AdsManager";
    private static AdsManager mInstance = null;
    private IAdsService _adsService;

    private boolean _isReadyToShowInter;

    public static AdsManager getInstance() {
        if (null == mInstace) {
            mInstace = new AdsManager();
        }
        return mInstace;
    }

    public void Init(IAdsService adsService) {
        _isReadyToShowInter = false;
        _adsService = adsService;
    }

    public IAdsService GetService() {
        return _adsService;
    }

    public void OnReadyToShowInter() {
        _isReadyToShowInter = true;
    }

    public boolean IsReadyToShowInter() {
        return _isReadyToShowInter;
    }
}
