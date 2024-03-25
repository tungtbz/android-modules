package com.rofi.ads;

import android.app.Activity;

public class AdsBridge {
    public static void ShowBanner() {
        AdsManager.getInstance().GetService().ShowBanner(activity);
    }
}
