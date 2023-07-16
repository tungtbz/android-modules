package com.rofi.base;

import android.os.Handler;
import android.util.Log;

public class ThreadUltils {

    private static Handler handler;
    private static Runnable runnable;

    public static void startTask(IThreadTask iThreadTask, long delayTimeInMs) {
        stopTask();
        handler = new Handler();
        runnable = iThreadTask::doTask;

        if (handler == null || runnable == null) {
            return;
        }

        handler.postDelayed(runnable, delayTimeInMs);

    }

    public static void stopTask() {
        try {
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
            handler = null;
            runnable = null;

        } catch (Exception e) {
            Log.e("ThreadUtil:", "Error:" + e.toString());

        }

    }

    public interface IThreadTask {
        void doTask();
    }


}
