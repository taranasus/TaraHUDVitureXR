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
    private boolean mIs3DModeEnabled = false; // Always use 2D mode

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
     * Update the display mode (always forces 2D mode)
     */
    public void setDisplayMode(boolean is3DMode) {
        // Always force 2D mode regardless of the parameter
        if (is3DMode) {
            Log.d(TAG, "3D mode requested but forcing 2D mode");
        }
        
        mIs3DModeEnabled = false;
        if (mGlassesPresentation != null) {
            mGlassesPresentation.setDisplayMode(false);
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
     * Ensure the glasses display stays active even when the phone screen is off
     * This is called when the phone screen is turned off
     */
    public void ensureDisplayActive() {
        // If we already have a valid presentation and display, we just need to make sure
        // it's refreshed to prevent it from being disconnected when the screen turns off
        if (mGlassesPresentation != null && mExternalDisplay != null) {
            Log.d(TAG, "Ensuring glasses presentation stays active during screen off");
            
            // Force a display mode update to refresh the connection
            mGlassesPresentation.setDisplayMode(mIs3DModeEnabled);
            
            // If the green box is visible, make sure it stays visible
            if (mGlassesPresentation.isGreenBoxVisible()) {
                mGlassesPresentation.setGreenBoxVisibility(true);
            }
        } else {
            // If we don't have a valid presentation, try to reconnect
            Log.d(TAG, "No active presentation, checking for external display");
            checkForExternalDisplay();
        }
    }
    
    /**
     * Refresh the display after the phone screen turns back on
     * This ensures the presentation is properly displayed after wake
     */
    public void refreshDisplay() {
        Log.d(TAG, "Refreshing display after screen turned on");
        
        // Check if we need to recreate the presentation
        if (mExternalDisplay != null) {
            boolean needsRefresh = false;
            
            // If the presentation is null or dismissed, we need to recreate it
            if (mGlassesPresentation == null) {
                needsRefresh = true;
            } else {
                try {
                    // Check if the presentation window is still valid
                    mGlassesPresentation.getWindow();
                } catch (Exception e) {
                    // If we get an exception, the presentation window is no longer valid
                    Log.e(TAG, "Presentation window is invalid, will recreate", e);
                    needsRefresh = true;
                }
            }
            
            if (needsRefresh) {
                Log.d(TAG, "Recreating presentation after screen on");
                showPresentation(mExternalDisplay);
            } else {
                // Just refresh the existing presentation
                Log.d(TAG, "Refreshing existing presentation");
                mGlassesPresentation.setDisplayMode(mIs3DModeEnabled);
            }
        } else {
            // No external display, check for one
            checkForExternalDisplay();
        }
    }
    
    /**
     * Get the current glasses presentation instance
     * This is used to access the presentation directly for operations like
     * restarting signal strength monitoring after permission is granted
     * 
     * @return The current GlassesPresentation instance, or null if not available
     */
    public GlassesPresentation getGlassesPresentation() {
        return mGlassesPresentation;
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
