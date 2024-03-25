package com.tbase.maxads;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.unity3d.player.UnityPlayer;

public class Utils {
    static Activity getCurrentActivity() {
        return UnityPlayer.currentActivity;
    }

    static void runSafelyOnUiThread(Activity activity, final Runnable runner) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    runner.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    static String retrieveSdkKey() {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null)
            return null;
        Bundle metaData = retrieveMetadata(currentActivity.getApplicationContext());
        if (metaData != null) {
            String sdkKey = metaData.getString("applovin.sdk.key");
            return (sdkKey != null) ? sdkKey : "";
        }
        return null;
    }

    private static Bundle retrieveMetadata(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), 128);
            return ai.metaData;
        } catch (Throwable throwable) {
            return null;
        }
    }
}
