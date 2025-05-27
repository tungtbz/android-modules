package com.rofi.remoteconfig;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirebaseRemoteConfigService {
    private static final String TAG = FirebaseRemoteConfigService.class.getName();
    private static FirebaseRemoteConfigService mInstance = null;

    private final Map<String, Object> map = new ConcurrentHashMap<>();
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private final AtomicBoolean isConfigFetched = new AtomicBoolean(false);

    private final List<ConfigChangedEvent> listeners = new CopyOnWriteArrayList<>();

    public static FirebaseRemoteConfigService getInstance() {
        if (null == mInstance) {
            synchronized (FirebaseRemoteConfigService.class) {
                if (null == mInstance) {
                    mInstance = new FirebaseRemoteConfigService();
                }
            }
        }
        return mInstance;
    }

    private FirebaseRemoteConfigService() {
    }

    public boolean IsConfigFetched() {
        return isConfigFetched.get();
    }

    public void Init(Activity activity) {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Thời gian fetch tối thiểu, có thể cấu hình
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
                .addOnCompleteListener(task -> { // Sử dụng lambda cho gọn
                    if (task.isSuccessful()) {
                        Log.d(TAG, "setDefaultsAsync Complete. Defaults are active.");
                        // Giá trị mặc định đã được áp dụng.
                        // Chúng ta vẫn cache chúng để getters có thể
                        // hoạt động ngay cả trước khi fetch thành công.
                        CacheConfigs();
                        // Bây giờ, tìm nạp và kích hoạt cấu hình từ server
                        fetchAndActivateRemoteConfigs(activity);
                    } else {
                        Log.e(TAG, "setDefaultsAsync Failed!", task.getException());
                        // Nếu setDefaultsAsync thất bại, isConfigFetched vẫn là false.
                        // Cân nhắc thông báo lỗi hoặc xử lý phù hợp.
                    }
                });
    }

    private void fetchAndActivateRemoteConfigs(Activity activity) {
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(activity, task -> { // activity ở đây để listener chạy trên UI thread
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "fetchAndActivate " + (updated ? "succeeded and new configs were activated." : "succeeded, no new configs."));
                        CacheConfigs(); // Cache lại các giá trị mới (hoặc giống cũ)
                        isConfigFetched.set(true); // Đánh dấu là đã fetch thành công từ server
                        if (updated) {
                            notifyListeners(); // Thông báo cho các listeners nếu có thay đổi
                        }
                    } else {
                        Log.w(TAG, "fetchAndActivate Failed. Using defaults or last fetched values.", task.getException());
                        // isConfigFetched vẫn là false (hoặc true nếu bạn muốn coi defaults là "fetched" ở mức độ nào đó)
                        // Hiện tại, để false nếu fetch từ server thất bại.
                        // Các giá trị mặc định (hoặc giá trị cache cũ nếu có) vẫn sẽ được sử dụng.
                    }
                });
    }

    private void CacheConfigs() {
        if (mFirebaseRemoteConfig == null) {
            Log.w(TAG, "CacheConfigs called but mFirebaseRemoteConfig is null.");
            return;
        }
        Log.d(TAG, "CacheConfigs ~~~~~~~~~~~~~~~~");
        map.clear(); // Xóa cache cũ

        Map<String, FirebaseRemoteConfigValue> allConfigs = mFirebaseRemoteConfig.getAll();
        for (Map.Entry<String, FirebaseRemoteConfigValue> entry : allConfigs.entrySet()) {
            String key = entry.getKey();
            FirebaseRemoteConfigValue value = entry.getValue();
            try {
                // Cố gắng chuyển đổi sang các kiểu dữ liệu phổ biến
                // Thứ tự quan trọng: boolean trước, rồi số, rồi string
                if (value.getSource() != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) { // VALUE_SOURCE_STATIC là default nếu không có gì khác
                    try {
                        // Thử parse boolean
                        String stringVal = value.asString().toLowerCase();
                        if (stringVal.equals("true") || stringVal.equals("false")) {
                            map.put(key, value.asBoolean());
                            Log.d(TAG, "Cached (Boolean) " + key + ": " + value.asBoolean());
                            continue;
                        }
                    } catch (IllegalArgumentException ignored) {}

                    try {
                        // Thử parse long (cho số nguyên)
                        map.put(key, value.asLong());
                        Log.d(TAG, "Cached (Long) " + key + ": " + value.asLong());
                        continue;
                    } catch (IllegalArgumentException ignored) {}

                    try {
                        // Thử parse double (cho số thực)
                        map.put(key, value.asDouble());
                        Log.d(TAG, "Cached (Double) " + key + ": " + value.asDouble());
                        continue;
                    } catch (IllegalArgumentException ignored) {}
                }
                // Mặc định lưu dưới dạng String
                map.put(key, value.asString());
                Log.d(TAG, "Cached (String) " + key + ": " + value.asString());

            } catch (Exception e) { // Bắt Exception chung để an toàn hơn
                Log.w(TAG, "Could not determine type for key: " + key + ", storing as string. Error: " + e.getMessage());
                map.put(key, value.asString()); // Fallback to string
            }
        }
        Log.d(TAG, "CacheConfigs finished. Map size: " + map.size());
    }

    public boolean GetBoolean(String key) {
        return GetBoolean(key, false); // Cung cấp giá trị mặc định
    }

    public boolean GetBoolean(String key, boolean defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            // Nếu lưu dưới dạng String "true"/"false"
            if (value instanceof String) {
                String stringValue = ((String) value).toLowerCase();
                if ("true".equals(stringValue)) return true;
                if ("false".equals(stringValue)) return false;
            }
            Log.w(TAG, "GetBoolean: Cached value for key '" + key + "' is not a Boolean, it's " + (value != null ? value.getClass().getSimpleName() : "null") + ". Returning from FirebaseRemoteConfig SDK or default.");
        }
        if (mFirebaseRemoteConfig != null) {
            return mFirebaseRemoteConfig.getBoolean(key);
        }
        return defaultValue;
    }

    public int GetInt(String key) {
        return GetInt(key, 0); // Cung cấp giá trị mặc định
    }

    public int GetInt(String key, int defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Long) {
                return ((Long) value).intValue();
            }
            if (value instanceof Double) {
                return ((Double) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    // Bỏ qua, sẽ thử lấy từ SDK
                }
            }
            Log.w(TAG, "GetInt: Cached value for key '" + key + "' is not a recognized number type, it's " + (value != null ? value.getClass().getSimpleName() : "null") + ". Returning from FirebaseRemoteConfig SDK or default.");
        }
        if (mFirebaseRemoteConfig != null) {
            // Firebase Remote Config lưu trữ tất cả các số dưới dạng Double hoặc Long.
            // getDouble() an toàn hơn cho việc chuyển đổi sang int.
            return (int) mFirebaseRemoteConfig.getDouble(key);
        }
        return defaultValue;
    }

    public long GetLong(String key) {
        return GetLong(key, 0L);
    }

    public long GetLong(String key, long defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof Double) { // Có thể mất độ chính xác nếu là số rất lớn
                return ((Double) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    // Bỏ qua
                }
            }
            Log.w(TAG, "GetLong: Cached value for key '" + key + "' is not a recognized number type. Returning from FirebaseRemoteConfig SDK or default.");
        }
        if (mFirebaseRemoteConfig != null) {
            return mFirebaseRemoteConfig.getLong(key);
        }
        return defaultValue;
    }

    public double GetDouble(String key) {
        return GetDouble(key, 0.0);
    }

    public double GetDouble(String key, double defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Long) {
                return ((Long) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    // Bỏ qua
                }
            }
            Log.w(TAG, "GetDouble: Cached value for key '" + key + "' is not a recognized number type. Returning from FirebaseRemoteConfig SDK or default.");
        }
        if (mFirebaseRemoteConfig != null) {
            return mFirebaseRemoteConfig.getDouble(key);
        }
        return defaultValue;
    }


    public String GetString(String key) {
        return GetString(key, ""); // Cung cấp giá trị mặc định
    }

    public String GetString(String key, String defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            // Nếu là kiểu khác, chuyển thành String
            if (value != null) {
                return String.valueOf(value);
            }
            Log.w(TAG, "GetString: Cached value for key '" + key + "' is null, though key exists. Returning from FirebaseRemoteConfig SDK or default.");
        }
        if (mFirebaseRemoteConfig != null) {
            return mFirebaseRemoteConfig.getString(key);
        }
        return defaultValue;
    }

    public void addConfigChangedListener(ConfigChangedEvent listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeConfigChangedListener(ConfigChangedEvent listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        // Đảm bảo việc thông báo được thực hiện trên luồng UI chính
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ConfigChangedEvent listener : listeners) {
                try {
                    listener.onRefresh();
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener: " + listener.getClass().getName(), e);
                }
            }
        });
    }

    public interface ConfigChangedEvent {
        void onRefresh();
    }
}