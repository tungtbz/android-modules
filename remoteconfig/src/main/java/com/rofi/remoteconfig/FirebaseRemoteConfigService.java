package com.rofi.remoteconfig;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.rofi.base.Constants;

import java.util.HashMap;
import java.util.Map;


public class FirebaseRemoteConfigService {
    private static final String TAG = FirebaseRemoteConfigService.class.getName();
    private static FirebaseRemoteConfigService mInstance = null;

    private Map<String, Object> map = new HashMap<String, Object>();

    public static FirebaseRemoteConfigService getInstance() {
        if (null == mInstance) {
            mInstance = new FirebaseRemoteConfigService();
        }
        return mInstance;
    }

    FirebaseRemoteConfig mFirebaseRemoteConfig;

    public void Init(Activity activity) {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "setDefaultsAsync Complete");
                        CacheConfigs();
                        mFirebaseRemoteConfig.fetchAndActivate()
                                .addOnCompleteListener(activity, new OnCompleteListener<Boolean>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Boolean> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "fetchAndActivate Complete");
                                            CacheConfigs();
                                        }else{
                                            Log.d(TAG, "fetchAndActivate Failed");
                                        }
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "setDefaultsAsync Failed!");
                    }
                });
    }

    private void CacheConfigs() {
        Log.d(TAG, "CacheConfigs ~~~~~~~~~~~~~~~~");
    }

    public boolean GetBoolean(String key) {
        if (mFirebaseRemoteConfig == null) return false;
        if (map.containsKey(key)) return (boolean) map.get(key);
        return mFirebaseRemoteConfig.getBoolean(key);
    }

    public int GetInt(String key) {
        if (mFirebaseRemoteConfig == null) return 0;
        if (map.containsKey(key)) return ((Double) map.get(key)).intValue();

        return (int) mFirebaseRemoteConfig.getDouble(key);
    }

    public String GetString(String key) {
        if (mFirebaseRemoteConfig == null) return "";
        if (map.containsKey(key)) return (String) map.get(key);
        return mFirebaseRemoteConfig.getString(key);
    }

    public interface ConfigChangedEvent{
        void onRefresh();
    }
}
