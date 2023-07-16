package com.rofi.analytic;
import android.app.Activity;

public interface IAnalytic {
    void Init(Activity activity);

    void LogEvent(Activity activity, String eventName, String eventData);

    void RevenueTracking(Activity activity, String adFormat, String adUnitId, String adSource, double value);

    void ClickToAd(Activity activity, String adUnitId);
}
