package com.eden.demo.sensor.hud;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Class responsible for managing display mode in the HUD (2D mode only)
 */
public class DisplayModeManager {
    private static final String TAG = "DisplayModeManager";
    
    // Display mode - always 2D mode
    private boolean mCurrentlyIn3DMode = false;
    
    // UI layouts
    private LinearLayout mHudLayout3D;
    private LinearLayout mHudLayout2D;
    
    // Callback interface for display mode changes
    public interface DisplayModeChangeListener {
        void onDisplayModeChanged(boolean is3DMode);
    }
    
    private DisplayModeChangeListener mListener;
    
    /**
     * Constructor
     * 
     * @param listener Listener for display mode changes
     */
    public DisplayModeManager(DisplayModeChangeListener listener) {
        mListener = listener;
    }
    
    /**
     * Set the UI layouts
     * 
     * @param hudLayout3D Layout for 3D mode
     * @param hudLayout2D Layout for 2D mode
     */
    public void setLayouts(LinearLayout hudLayout3D, LinearLayout hudLayout2D) {
        mHudLayout3D = hudLayout3D;
        mHudLayout2D = hudLayout2D;
    }
    
    /**
     * Set the display mode (always forces 2D mode)
     * 
     * @param is3DMode Ignored - always sets to 2D mode
     */
    public void setDisplayMode(boolean is3DMode) {
        // Always force 2D mode regardless of the parameter
        if (is3DMode) {
            Log.d(TAG, "3D mode requested but forcing 2D mode");
        } else {
            Log.d(TAG, "Setting display mode: 2D");
        }
        
        // Always set to 2D mode
        mCurrentlyIn3DMode = false;
        
        // Always show 2D mode HUD, hide 3D mode HUD
        if (mHudLayout3D != null) {
            mHudLayout3D.setVisibility(View.GONE);
        }
        if (mHudLayout2D != null) {
            mHudLayout2D.setVisibility(View.VISIBLE);
        }
        
        // Notify listener of display mode change
        if (mListener != null) {
            mListener.onDisplayModeChanged(is3DMode);
        }
    }
    
    /**
     * Set the visibility of the HUD display
     * 
     * @param visible True to show the HUD, false to hide it
     */
    public void setHudVisibility(boolean visible) {
        Log.d(TAG, "Setting HUD visibility: " + (visible ? "visible" : "hidden"));
        
        // Always update 2D mode visibility only
        if (mHudLayout2D != null) {
            mHudLayout2D.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Check if the HUD is currently visible
     * 
     * @return True if the HUD is visible
     */
    public boolean isHudVisible() {
        // Only check 2D mode visibility
        return mHudLayout2D != null && 
               mHudLayout2D.getVisibility() == View.VISIBLE;
    }
    
    /**
     * Get the current display mode
     * 
     * @return Always false (2D mode)
     */
    public boolean isIn3DMode() {
        return false;
    }
    
    /**
     * Release resources
     */
    public void release() {
        mListener = null;
    }
}
