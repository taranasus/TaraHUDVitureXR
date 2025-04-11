package com.eden.demo.sensor.hud;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Class responsible for managing display modes (2D/3D) in the HUD
 */
public class DisplayModeManager {
    private static final String TAG = "DisplayModeManager";
    
    // Display mode
    private boolean mCurrentlyIn3DMode = true;
    
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
     * Set the display mode (2D or 3D)
     * 
     * @param is3DMode True for 3D mode, false for 2D mode
     */
    public void setDisplayMode(boolean is3DMode) {
        Log.d(TAG, "Setting display mode: " + (is3DMode ? "3D" : "2D"));
        mCurrentlyIn3DMode = is3DMode;
        
        if (is3DMode) {
            // In 3D mode: 3840x1080, only show the right eye HUD
            if (mHudLayout3D != null) {
                mHudLayout3D.setVisibility(View.VISIBLE);
            }
            if (mHudLayout2D != null) {
                mHudLayout2D.setVisibility(View.GONE);
            }
        } else {
            // In 2D mode: 1920x1080, show the 2D HUD
            if (mHudLayout3D != null) {
                mHudLayout3D.setVisibility(View.GONE);
            }
            if (mHudLayout2D != null) {
                mHudLayout2D.setVisibility(View.VISIBLE);
            }
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
        
        // Update visibility based on current mode
        if (mCurrentlyIn3DMode) {
            if (mHudLayout3D != null) {
                mHudLayout3D.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        } else {
            if (mHudLayout2D != null) {
                mHudLayout2D.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    /**
     * Check if the HUD is currently visible
     * 
     * @return True if the HUD is visible in the current display mode
     */
    public boolean isHudVisible() {
        if (mCurrentlyIn3DMode) {
            return mHudLayout3D != null && 
                   mHudLayout3D.getVisibility() == View.VISIBLE;
        } else {
            return mHudLayout2D != null && 
                   mHudLayout2D.getVisibility() == View.VISIBLE;
        }
    }
    
    /**
     * Get the current display mode
     * 
     * @return True if in 3D mode, false if in 2D mode
     */
    public boolean isIn3DMode() {
        return mCurrentlyIn3DMode;
    }
    
    /**
     * Release resources
     */
    public void release() {
        mListener = null;
    }
}
