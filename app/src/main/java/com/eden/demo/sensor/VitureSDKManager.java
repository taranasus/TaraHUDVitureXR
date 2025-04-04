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
    private boolean mIs3DModeEnabled = true; // Default to 3D mode
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
                    
                    // Enable 3D mode once initialization is confirmed successful
                    if (mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
                        enable3DMode();
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
     * Enable 3D mode for the XR glasses
     */
    private void enable3DMode() {
        if (mArManager == null) {
            Log.e(TAG, "Cannot enable 3D mode: ArManager is null");
            return;
        }
        
        int result = mArManager.set3D(true);
        if (result == Constants.ERR_SET_SUCCESS) {
            mIs3DModeEnabled = true;
            Log.d(TAG, "3D mode enabled successfully");
            
            // Notify listener of state change
            if (mEventListener != null) {
                mEventListener.on3DModeStateChanged(true);
            }
        } else {
            Log.e(TAG, "Failed to enable 3D mode: " + result);
        }
    }

    /**
     * Toggle between 2D and 3D display modes
     * 
     * @param enable3D True to enable 3D mode, false for 2D mode
     * @return True if the operation was successful
     */
    public boolean toggleDisplayMode(boolean enable3D) {
        if (mArManager != null && mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
            int result = mArManager.set3D(enable3D);
            if (result == Constants.ERR_SET_SUCCESS) {
                mIs3DModeEnabled = enable3D;
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
        return mIs3DModeEnabled;
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
            // Refresh the current display mode to maintain active connection
            int result = mArManager.set3D(mIs3DModeEnabled);
            if (result == Constants.ERR_SET_SUCCESS) {
                Log.d(TAG, "Display mode refreshed successfully");
            } else {
                Log.e(TAG, "Failed to refresh display mode: " + result);
            }
            
            // Set 3D mode again to ensure the display stays active
            // This helps maintain the signal to the glasses
            if (mIs3DModeEnabled) {
                // If already in 3D mode, toggle to 2D and back to force a refresh
                mArManager.set3D(false);
                mArManager.set3D(true);
            } else {
                // If in 2D mode, toggle to 3D and back to force a refresh
                mArManager.set3D(true);
                mArManager.set3D(false);
            }
            
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
                // Refresh the 3D mode to ensure display is showing correctly
                mArManager.set3D(mIs3DModeEnabled);
                
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
                    // Restore original display mode before releasing
                    mArManager.set3D(mIs3DModeEnabled);
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
