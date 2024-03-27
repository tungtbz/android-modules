package com.rofi.maxads;

import android.app.Activity;

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
}
