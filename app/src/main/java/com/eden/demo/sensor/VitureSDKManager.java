package com.eden.demo.sensor;

import android.content.Context;
import android.util.Log;

import com.viture.sdk.ArCallback;
import com.viture.sdk.ArManager;
import com.viture.sdk.Constants;

/**
 * Manager class responsible for handling all interactions with the VITURE SDK
 */
public class VitureSDKManager {
    private static final String TAG = "VitureSDKManager";

    private Context mContext;
    private ArManager mArManager;
    private ArCallback mCallback;
    private int mSdkInitSuccess = -1;
    private boolean mIs3DModeEnabled = false; // Always use 2D mode
    private boolean mIsKeepingAlive = false; // Track if we're in keep-alive mode

    // Callback interface for SDK events
    public interface VitureSDKEventListener {
        void onSDKInitializationComplete(boolean success);
        void on3DModeStateChanged(boolean enabled);
    }

    private VitureSDKEventListener mEventListener;

    /**
     * Constructor
     * 
     * @param context Application context
     * @param listener Event listener for SDK callbacks
     */
    public VitureSDKManager(Context context, VitureSDKEventListener listener) {
        mContext = context;
        mEventListener = listener;
        init();
    }

    /**
     * Initialize the VITURE SDK
     */
    private void init() {
        mArManager = ArManager.getInstance(mContext);
        mSdkInitSuccess = mArManager.init();
        mArManager.setLogOn(true);
        
        // We'll defer enabling 3D mode until after all callbacks are registered
        
        mCallback = new ArCallback() {
            @Override
            public void onEvent(int msgId, byte[] event, long timestamp) {
                Log.d(TAG, "onEvent msgId: " + msgId);

                if (msgId == Constants.EVENT_ID_INIT) {
                    mSdkInitSuccess = byteArrayToInt(event, 0, event.length);
                    Log.d(TAG, "SDK initialization: " + mSdkInitSuccess);
                    
                    // Notify listener of initialization result
                    if (mEventListener != null) {
                        boolean success = mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS;
                        mEventListener.onSDKInitializationComplete(success);
                    }
                    
                    // Always ensure 2D mode is enabled once initialization is confirmed successful
                    if (mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
                        ensure2DMode();
                    }
                } else if (msgId == Constants.EVENT_ID_3D) {
                    int state = byteArrayToInt(event, 0, event.length);
                    final boolean isEnabled = state == Constants.STATE_ON;
                    final String stateStr = (isEnabled ? "ON" : "OFF");
                    Log.d(TAG, "3D mode state: " + stateStr);
                    
                    // Notify listener of 3D mode state change
                    if (mEventListener != null) {
                        mEventListener.on3DModeStateChanged(isEnabled);
                    }
                }
            }

            @Override
            public void onImu(long timestamp, byte[] imuData) {
                // Not using IMU data
            }
        };
        
        // Register the callback
        mArManager.registerCallback(mCallback);
    }

    /**
     * Ensure 2D mode is enabled for the XR glasses
     */
    private void ensure2DMode() {
        if (mArManager == null) {
            Log.e(TAG, "Cannot set 2D mode: ArManager is null");
            return;
        }
        
        int result = mArManager.set3D(false);
        if (result == Constants.ERR_SET_SUCCESS) {
            mIs3DModeEnabled = false;
            Log.d(TAG, "2D mode enabled successfully");
            
            // Notify listener of state change
            if (mEventListener != null) {
                mEventListener.on3DModeStateChanged(false);
            }
        } else {
            Log.e(TAG, "Failed to enable 2D mode: " + result);
        }
    }

    /**
     * Toggle display mode (always forces 2D mode)
     * 
     * @param enable3D Ignored - always sets to 2D mode
     * @return True if the operation was successful
     */
    public boolean toggleDisplayMode(boolean enable3D) {
        if (mArManager != null && mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
            // Always force 2D mode regardless of the parameter
            if (enable3D) {
                Log.d(TAG, "3D mode requested but forcing 2D mode");
            }
            
            int result = mArManager.set3D(false);
            if (result == Constants.ERR_SET_SUCCESS) {
                mIs3DModeEnabled = false;
                return true;
            }
        }
        return false;
    }

    /**
     * Check if SDK has been successfully initialized
     */
    public boolean isInitialized() {
        return mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS;
    }

    /**
     * Get the current 3D mode state
     */
    public boolean is3DModeEnabled() {
        return false; // Always return false (2D mode)
    }

    /**
     * Keep the glasses display active during screen off
     * This is called when the phone screen is turned off
     */
    public void keepDisplayActiveOnScreenOff() {
        if (mArManager == null || mSdkInitSuccess != Constants.ERROR_INIT_SUCCESS) {
            Log.e(TAG, "Cannot keep display active: ArManager is null or not initialized");
            return;
        }

        Log.d(TAG, "Activating keep-alive mode for glasses display");
        
        try {
            // Always refresh with 2D mode to maintain active connection
            int result = mArManager.set3D(false);
            if (result == Constants.ERR_SET_SUCCESS) {
                Log.d(TAG, "Display mode refreshed successfully (2D mode)");
            } else {
                Log.e(TAG, "Failed to refresh display mode: " + result);
            }
            
            // Toggle to 3D and back to 2D to force a refresh
            // This helps maintain the signal to the glasses
            mArManager.set3D(true);
            mArManager.set3D(false);
            
            mIsKeepingAlive = true;
            Log.d(TAG, "Keep-alive mode activated for glasses");
        } catch (Exception e) {
            Log.e(TAG, "Error setting keep-alive mode", e);
        }
    }
    
    /**
     * Reset normal operation when screen turns back on
     */
    public void resetDisplayOnScreenOn() {
        if (mArManager == null || mSdkInitSuccess != Constants.ERROR_INIT_SUCCESS) {
            return;
        }
        
        if (mIsKeepingAlive) {
            Log.d(TAG, "Resetting display after screen on");
            try {
                // Always refresh with 2D mode to ensure display is showing correctly
                mArManager.set3D(false);
                
                mIsKeepingAlive = false;
                Log.d(TAG, "Normal mode restored");
            } catch (Exception e) {
                Log.e(TAG, "Error resetting display", e);
            }
        }
    }
    
    /**
     * Clean up resources when no longer needed
     */
    public void release() {
        if (mArManager != null) {
            // If we're in keep-alive mode, reset it before releasing
            if (mIsKeepingAlive) {
                try {
                    // Always ensure 2D mode before releasing
                    mArManager.set3D(false);
                    mIsKeepingAlive = false;
                } catch (Exception e) {
                    Log.e(TAG, "Error resetting display mode during release", e);
                }
            }
            
            if (mCallback != null) {
                mArManager.unregisterCallback(mCallback);
            }
            mArManager.release();
        }
    }

    /**
     * Utility method to convert byte array to int
     */
    private int byteArrayToInt(byte[] bytes, int offset, int length) {
        if (bytes == null) {
            return 0;
        }
        int value = 0;
        int len = bytes.length;
        if (offset > len || offset < 0) {
            return 0;
        }
        int right = offset + length;
        if (right > len) {
            right = len;
        }
        for (int i = offset; i < right; i++) {
            int shift = (i - offset) * 8;
            value += (bytes[i] & 0x000000FF) << shift;
        }
        return value;
    }
}
