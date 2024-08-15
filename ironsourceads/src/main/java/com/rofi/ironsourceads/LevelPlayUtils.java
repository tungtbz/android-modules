package com.rofi.ironsourceads;

import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;

import org.json.JSONObject;

public class LevelPlayUtils {
    public static String adInfoToString(LevelPlayAdInfo adInfo) {
        JSONObject adInfoData = new JSONObject();
        try {
            adInfoData.put("adUnitId", adInfo.getAdUnitId());
            adInfoData.put("adSize", adSizeToString(adInfo.getAdSize()));
            adInfoData.put("adFormat", adInfo.getAdFormat());
            adInfoData.put("placementName", adInfo.getPlacementName());
            adInfoData.put("auctionId", adInfo.getAuctionId());
            adInfoData.put("country", adInfo.getCountry());
            adInfoData.put("ab", adInfo.getAb());
            adInfoData.put("segmentName", adInfo.getSegmentName());
            adInfoData.put("adNetwork", adInfo.getAdNetwork());
            adInfoData.put("instanceName", adInfo.getInstanceName());
            adInfoData.put("instanceId", adInfo.getInstanceId());
            adInfoData.put("revenue", adInfo.getRevenue());
            adInfoData.put("precision", adInfo.getPrecision());
            adInfoData.put("encryptedCPM", adInfo.getEncryptedCPM());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return adInfoData.toString();
    }

    public static String configurationToString(LevelPlayConfiguration config) {
        JSONObject configData = new JSONObject();
        try {
            configData.put("isAdQualityEnabled", config.isAdQualityEnabled());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configData.toString();
    }

    public static String initErrorToString(LevelPlayInitError error) {
        JSONObject errorData = new JSONObject();
        try {
            errorData.put("errorCode", error.getErrorCode());
            errorData.put("errorMessage", error.getErrorMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errorData.toString();
    }

    public static String adErrorToString(LevelPlayAdError error) {
        JSONObject errorData = new JSONObject();
        try {
            errorData.put("errorCode", error.getErrorCode());
            errorData.put("errorMessage", error.getErrorMessage());
            errorData.put("adUnitId", error.getAdUnitId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errorData.toString();
    }

    private static String adSizeToString(LevelPlayAdSize adSize) {
        JSONObject adSizeData = new JSONObject();
        try {
            adSizeData.put("description", adSize.getDescription());
            adSizeData.put("width", adSize.getWidth());
            adSizeData.put("height", adSize.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return adSizeData.toString();
    }
}
