package com.rofi.admobadshelper;

import android.app.Activity;
import android.content.Context;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

/**
 * The Google Mobile Ads SDK provides the User Messaging Platform (Google's IAB Certified consent
 * management platform) as one solution to capture consent for users in GDPR impacted countries.
 * This is an example and you can choose another consent management platform to capture consent.
 */
public final class GoogleMobileAdsConsentManager {
    private static GoogleMobileAdsConsentManager instance;
    private final ConsentInformation consentInformation;
    private boolean isConsentFlowFinished;

    public boolean IsConsentFlowFinished() {
        return isConsentFlowFinished;
    }

    public void bypassConsentFlow(){
        isConsentFlowFinished = true;
    }

    /**
     * Private constructor
     */
    private GoogleMobileAdsConsentManager(Context context) {
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    /**
     * Public constructor
     */
    public static GoogleMobileAdsConsentManager getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleMobileAdsConsentManager(context);
        }

        return instance;
    }

    /**
     * Interface definition for a callback to be invoked when consent gathering is complete.
     */
    public interface OnConsentGatheringCompleteListener {
        void consentGatheringComplete(FormError error);
    }

    /**
     * Helper variable to determine if the app can request ads.
     */
    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    /**
     * Helper variable to determine if the privacy options form is required.
     */
    public boolean isPrivacyOptionsRequired() {
        return consentInformation.getPrivacyOptionsRequirementStatus()
                == PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /**
     * Helper method to call the UMP SDK methods to request consent information and load/present a
     * consent form if necessary.
     */
    public void gatherConsent(
            Activity activity, OnConsentGatheringCompleteListener onConsentGatheringCompleteListener) {
        // For testing purposes, you can force a DebugGeography of EEA or NOT_EEA.
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                 .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                // Check your logcat output for the hashed device ID e.g.
                // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345")" to use
                // the debug functionality.
                .addTestDeviceHashedId("0B6312C522BE88E9F10AAC1F7CF2FC41")
                .build();

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .setConsentDebugSettings(debugSettings)
                .build();

        // Requesting an update to consent information should be called on every app launch.
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () ->
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                activity,
                                formError -> {
                                    // Consent has been gathered.
                                    onConsentGatheringCompleteListener.consentGatheringComplete(formError);
                                    isConsentFlowFinished = true;
                                }),
                requestConsentError -> {
                    onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError);
                    isConsentFlowFinished = true;
                }

        );
    }

    /**
     * Helper method to call the UMP SDK method to present the privacy options form.
     */
    public void showPrivacyOptionsForm(
            Activity activity,
            OnConsentFormDismissedListener onConsentFormDismissedListener) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener);
    }
}