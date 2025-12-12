package com.rofi.ads;

import android.app.Activity;
import android.util.Log;

/**
 * AdsManager - Singleton class to manage ads service instance
 * Thread-safe implementation with proper lifecycle management
 */
public class AdsManager {
    private static volatile AdsManager mInstance = null;
    private static final Object sLock = new Object();
    private final String TAG = "AdsManager";
    
    // Thread-safe variables using volatile
    private volatile IAdsService _adsService;
    private volatile boolean _isReadyToShowInter;
    private volatile boolean _isInitialized;

    // Private constructor to prevent direct instantiation
    private AdsManager() {
        _isReadyToShowInter = false;
        _isInitialized = false;
    }

    /**
     * Thread-safe singleton implementation using double-checked locking
     */
    public static AdsManager getInstance() {
        if (mInstance == null) {
            synchronized (sLock) {
                if (mInstance == null) {
                    mInstance = new AdsManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * Initialize the ads service with proper validation and error handling
     * @param adsService The ads service implementation to use
     * @return true if initialization successful, false otherwise
     */
    public synchronized boolean init(IAdsService adsService) {
        if (adsService == null) {
            Log.e(TAG, "Cannot initialize AdsManager with null service");
            return false;
        }
        
        try {
            _adsService = adsService;
            _isReadyToShowInter = false;
            _isInitialized = true;
            
            Log.d(TAG, "AdsManager initialized successfully with service: " + 
                  adsService.getClass().getSimpleName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdsManager", e);
            _isInitialized = false;
            return false;
        }
    }

    /**
     * Get the current ads service instance
     * @return The ads service or null if not initialized
     */
    public IAdsService getService() {
        if (!_isInitialized) {
            Log.w(TAG, "AdsManager not initialized. Call init() first.");
            return null;
        }
        return _adsService;
    }

    /**
     * Validate if the provided service implements required methods
     * @param service The service to validate
     * @return true if service is valid, false otherwise
     */
    private boolean validateService(IAdsService service) {
        try {
            // Basic validation - check if service implements the interface
            return service != null;
        } catch (Exception e) {
            Log.e(TAG, "Service validation failed", e);
            return false;
        }
    }

    /**
     * Safe method to get service with additional validation
     * @return The ads service if available and valid, null otherwise
     */
    public IAdsService getSafeService() {
        IAdsService service = getService();
        if (service != null && validateService(service)) {
            return service;
        }
        Log.w(TAG, "Service not available or invalid");
        return null;
    }

    /**
     * Mark that the manager is ready to show interstitial ads
     */
    public synchronized void setReadyToShowInter() {
        if (!_isInitialized) {
            Log.w(TAG, "Cannot set ready state - AdsManager not initialized");
            return;
        }
        
        _isReadyToShowInter = true;
        Log.d(TAG, "AdsManager ready to show interstitial ads");
    }

    /**
     * Check if the manager is ready to show interstitial ads
     * @return true if ready and initialized, false otherwise
     */
    public boolean isReadyToShowInter() {
        return _isInitialized && _isReadyToShowInter;
    }

    /**
     * Check if the AdsManager has been properly initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return _isInitialized;
    }

    /**
     * Get the current ads service type/name for debugging
     * @return Service class name or "Not initialized"
     */
    public String getServiceType() {
        if (_adsService != null) {
            return _adsService.getClass().getSimpleName();
        }
        return "Not initialized";
    }

    /**
     * Cleanup method to reset the manager state
     * Should be called when the manager is no longer needed
     */
    public synchronized void cleanup() {
        try {
            Log.d(TAG, "Cleaning up AdsManager");
            
            _adsService = null;
            _isReadyToShowInter = false;
            _isInitialized = false;
            
            Log.d(TAG, "AdsManager cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during AdsManager cleanup", e);
        }
    }

    /**
     * Reset the ready state for interstitial ads
     * Useful when ads fail to load or after showing an ad
     */
    public synchronized void resetReadyState() {
        _isReadyToShowInter = false;
        Log.d(TAG, "AdsManager ready state reset");
    }
}
