package com.eden.demo.sensor;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Manager class responsible for handling external display detection and presentation management
 */
public class DisplayPresentationManager {
    private static final String TAG = "DisplayPresentManager";

    private Context mContext;
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener;
    private GlassesPresentation mGlassesPresentation;
    private Display mExternalDisplay;
    private boolean mIs3DModeEnabled = true; // Default to 3D mode

    // Callback interface for display events
    public interface DisplayEventListener {
        void onExternalDisplayConnected(boolean connected);
    }

    private DisplayEventListener mEventListener;

    /**
     * Constructor
     * 
     * @param context Application context
     * @param listener Event listener for display events
     */
    public DisplayPresentationManager(Context context, DisplayEventListener listener) {
        mContext = context;
        mEventListener = listener;
        initDisplayManager();
    }

    /**
     * Initialize the DisplayManager to detect external displays (XR glasses)
     */
    private void initDisplayManager() {
        mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        
        // Create display listener to detect when XR glasses are connected/disconnected
        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                Log.d(TAG, "Display added: " + displayId);
                checkForExternalDisplay();
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                Log.d(TAG, "Display removed: " + displayId);
                if (mGlassesPresentation != null && 
                    mExternalDisplay != null && 
                    mExternalDisplay.getDisplayId() == displayId) {
                    
                    mGlassesPresentation.dismiss();
                    mGlassesPresentation = null;
                    mExternalDisplay = null;
                    
                    // Notify listener that display is disconnected
                    if (mEventListener != null) {
                        mEventListener.onExternalDisplayConnected(false);
                    }
                    
                    Log.d(TAG, "XR glasses disconnected");
                }
                checkForExternalDisplay();
            }

            @Override
            public void onDisplayChanged(int displayId) {
                Log.d(TAG, "Display changed: " + displayId);
                // Handle if needed
            }
        };
        
        // Register the display listener
        mDisplayManager.registerDisplayListener(mDisplayListener, null);
        
        // Check for already connected displays
        checkForExternalDisplay();
    }
    
    /**
     * Check for external displays and set up presentation if found
     */
    private void checkForExternalDisplay() {
        if (mDisplayManager == null) {
            Log.e(TAG, "Cannot check for external displays: DisplayManager is null");
            return;
        }
        
        Display[] displays = mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        
        if (displays.length > 0) {
            // Use the first external display
            if (mGlassesPresentation == null || mExternalDisplay == null) {
                mExternalDisplay = displays[0];
                showPresentation(mExternalDisplay);
                
                // Notify listener that display is connected
                if (mEventListener != null) {
                    mEventListener.onExternalDisplayConnected(true);
                }
            }
        } else {
            Log.d(TAG, "No external display found");
            
            // Notify listener that no display is connected
            if (mEventListener != null && (mGlassesPresentation != null || mExternalDisplay != null)) {
                mEventListener.onExternalDisplayConnected(false);
            }
            
            // Clean up any existing presentation
            if (mGlassesPresentation != null) {
                mGlassesPresentation.dismiss();
                mGlassesPresentation = null;
                mExternalDisplay = null;
            }
        }
    }
    
    /**
     * Create and show the presentation on the external display (XR glasses)
     */
    private void showPresentation(Display display) {
        if (display == null) return;
        
        // Dismiss any existing presentation
        if (mGlassesPresentation != null) {
            mGlassesPresentation.dismiss();
            mGlassesPresentation = null;
        }
        
        // Create a new presentation
        mGlassesPresentation = new GlassesPresentation(mContext, display);
        
        try {
            mGlassesPresentation.show();
            // Set the initial display mode
            mGlassesPresentation.setDisplayMode(mIs3DModeEnabled);
            Log.d(TAG, "Presentation shown on external display");
        } catch (WindowManager.InvalidDisplayException e) {
            Log.e(TAG, "Unable to show presentation on external display", e);
            mGlassesPresentation = null;
        }
    }

    /**
     * Update the display mode (2D or 3D)
     */
    public void setDisplayMode(boolean is3DMode) {
        mIs3DModeEnabled = is3DMode;
        if (mGlassesPresentation != null) {
            mGlassesPresentation.setDisplayMode(is3DMode);
        }
    }

    /**
     * Show or hide the green box on the glasses presentation
     */
    public boolean showGreenBox(boolean show) {
        if (mGlassesPresentation != null) {
            mGlassesPresentation.setGreenBoxVisibility(show);
            return true;
        }
        return false;
    }
    
    /**
     * Check if the glasses are connected
     */
    public boolean areGlassesConnected() {
        return mGlassesPresentation != null && mExternalDisplay != null;
    }
    
    /**
     * Clean up resources when no longer needed
     */
    public void release() {
        if (mDisplayManager != null && mDisplayListener != null) {
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        }
        
        if (mGlassesPresentation != null) {
            mGlassesPresentation.dismiss();
            mGlassesPresentation = null;
        }
        
        mExternalDisplay = null;
    }
}
