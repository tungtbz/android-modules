package com.rofi.ads;

public class TAdsPlugin {
    public static void SetBackgroundCallback(AdsManager.BackgroundCallback backgroundCallback) {
        AdsManager.getInstance().GetService().setBackgroundCallback(backgroundCallback);
    }

    public static void ShowBanner(String position) {
        AdsManager.getInstance().GetService().showBanner(position);
    }

    public static void HideBanner() {
        AdsManager.getInstance().GetService().hideBanner();
    }
}
