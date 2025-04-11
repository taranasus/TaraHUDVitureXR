package com.eden.demo.sensor;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.maps.MapView;

import com.eden.demo.sensor.databinding.GlassesDisplayBinding;
import com.eden.demo.sensor.hud.DisplayModeManager;
import com.eden.demo.sensor.hud.HudElementsUpdater;
import com.eden.demo.sensor.hud.MinimapManager;
import com.eden.demo.sensor.hud.SegmentBarsManager;
import com.eden.demo.sensor.hud.SignalStrengthMonitor;

/**
 * Custom Presentation class for displaying content on the XR glasses with Cyberpunk styled HUD
 */
public class GlassesPresentation extends Presentation implements 
        SignalStrengthMonitor.SignalStrengthListener,
        HudElementsUpdater.SignalStrengthUpdateListener,
        DisplayModeManager.DisplayModeChangeListener {
    
    private static final String TAG = "GlassesHUD";
    
    // Layout bindings
    private GlassesDisplayBinding mGlassesBinding;
    
    // UI elements for 3D mode
    private TextView mTimeDisplay3D;
    private ProgressBar mBatteryBar3D;
    private LinearLayout mSignalLayout3D;
    private LinearLayout mSegmentBar3D;
    private TextView mDayDisplay3D;
    private TextView mMonthDisplay3D;
    private LinearLayout mHudLayout3D;
    
    // UI elements for 2D mode
    private HealthStats mHealthStats2D;
    private LinearLayout mHudLayout2D;
    
    // Map views for 2D and 3D modes
    private MapView mMapView2D;
    private MapView mMapView3D;
    
    // HUD component managers
    private SignalStrengthMonitor mSignalMonitor;
    private SegmentBarsManager mSegmentBarsManager;
    private HudElementsUpdater mHudUpdater;
    private DisplayModeManager mDisplayModeManager;
    private MinimapManager mMinimapManager;
    
    /**
     * Constructor
     * 
     * @param context Application context
     * @param display Display to show the presentation on
     */
    public GlassesPresentation(Context context, Display display) {
        super(context, display);
        
        // Initialize component managers
        mSignalMonitor = new SignalStrengthMonitor(context, this);
        mSegmentBarsManager = new SegmentBarsManager(context);
        mHudUpdater = new HudElementsUpdater(context, this);
        mDisplayModeManager = new DisplayModeManager(this);
        mMinimapManager = new MinimapManager(context);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Request full screen for the presentation
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Inflate the glasses layout
        mGlassesBinding = GlassesDisplayBinding.inflate(getLayoutInflater());
        setContentView(mGlassesBinding.getRoot());
        
        // Initialize UI references
        initializeUIReferences();
        
        // Apply the custom Rajdhani font to all text views
        applyCustomFont();
        
        // Create bar segments
        mSegmentBarsManager.createSignalBars(mSignalLayout3D);
        
        // Set UI elements for HUD updater
        mHudUpdater.set3DModeElements(mTimeDisplay3D, mBatteryBar3D, mDayDisplay3D, mMonthDisplay3D);
 mHudUpdater.set2DModeElements(mHealthStats2D);
        
 // Initialize signal strength monitoring
        mSignalMonitor.initialize();
        
        // Initialize minimap
        initializeMinimap();
        
        // Set initial display mode (always 2D mode)
        mDisplayModeManager.setDisplayMode(false);
        
        // Enable immersive mode for the presentation
        enablePresentationImmersiveMode();
        
        // Start HUD updates
        mHudUpdater.startUpdates();
    }
    
    /**
     * Initialize UI references from the layout binding
     */
    private void initializeUIReferences() {
        // 3D mode UI elements
        mHudLayout3D = mGlassesBinding.hudLayoutRightEye;
        mTimeDisplay3D = mGlassesBinding.timeDisplayRight;
        mBatteryBar3D = mGlassesBinding.batteryBarRight;
        mSignalLayout3D = mGlassesBinding.signalLayoutRight;
        mSegmentBar3D = mGlassesBinding.segmentBarRight;
        mDayDisplay3D = mGlassesBinding.dayDisplayRight;
        mMonthDisplay3D = mGlassesBinding.monthDisplayRight;
        
        // 2D mode UI elements
        
        mHealthStats2D = mGlassesBinding.healthStats2d;
        
        // Minimap views
        mMapView3D = mGlassesBinding.minimap3d;
        mMapView2D = mGlassesBinding.minimap2d;
    }
    
    /**
     * Initialize the minimap
     * This is called during onCreate and can be called again when location permission is granted
     */
    public void initializeMinimap() {
        if (mMinimapManager != null && mMapView2D != null && mMapView3D != null) {
            // Set map views
            mMinimapManager.setMapViews(mMapView2D, mMapView3D);
            
            // Initialize with saved instance state (null for first creation)
            mMinimapManager.onCreate(null);
            
            // Start location updates if the map is ready
            mMinimapManager.startLocationUpdates();
        }
    }
    
    /**
     * Apply the custom Rajdhani font to all text views
     */
    private void applyCustomFont() {
        Typeface rajdhaniTypeface = ResourcesCompat.getFont(getContext(), R.font.rajdhani_medium);
        if (rajdhaniTypeface != null) {
            // 3D mode text views
            mTimeDisplay3D.setTypeface(rajdhaniTypeface);
            mDayDisplay3D.setTypeface(rajdhaniTypeface);
            mMonthDisplay3D.setTypeface(rajdhaniTypeface);
            
            // 2D mode text views
        }
    }
    
    /**
     * Enable immersive mode for the presentation
     */
    private void enablePresentationImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | 
                                WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // For Android 10 and below
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
            View decorView = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(flags);
        }
    }
    
    /**
     * Clean up resources when the presentation is dismissed
     */
    @Override
    public void dismiss() {
        // Stop HUD updates
        if (mHudUpdater != null) {
            mHudUpdater.release();
        }
        
        // Stop signal strength monitoring
        if (mSignalMonitor != null) {
            mSignalMonitor.release();
        }
        
        // Release display mode manager
        if (mDisplayModeManager != null) {
            mDisplayModeManager.release();
        }
        
        // Release minimap resources
        if (mMinimapManager != null) {
            mMinimapManager.onDestroy();
        }
        
        super.dismiss();
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
        }
        
        if (mDisplayModeManager != null) {
            mDisplayModeManager.setDisplayMode(false);
        }
        
        // Update minimap display mode (always 2D)
        if (mMinimapManager != null) {
            mMinimapManager.setDisplayMode(false);
        }
    }
    
    /**
     * Set the visibility of the HUD display
     * 
     * @param visible True to show the HUD, false to hide it
     */
    public void setGreenBoxVisibility(boolean visible) {
        if (mDisplayModeManager != null) {
            mDisplayModeManager.setHudVisibility(visible);
        }
    }
    
    /**
     * Check if the HUD is currently visible
     * 
     * @return True if the HUD is visible in the current display mode
     */
    public boolean isGreenBoxVisible() {
        return mDisplayModeManager != null && mDisplayModeManager.isHudVisible();
    }
    
    /**
     * Reinitialize signal strength monitoring
     * This is called when the READ_PHONE_STATE permission is granted
     */
    public void reinitializeSignalStrengthMonitoring() {
        Log.d(TAG, "Reinitializing signal strength monitoring");
        
        if (mSignalMonitor != null) {
            mSignalMonitor.reinitialize();
        }
    }
    
    //
    // Interface implementations
    //
    
    /**
     * Called when the signal strength changes
     * Implementation of SignalStrengthMonitor.SignalStrengthListener
     */
    @Override
    public void onSignalStrengthChanged(int signalStrength, int maxBars) {
        Log.d(TAG, "Signal strength changed: " + signalStrength + " out of " + maxBars);
        
        // Update signal bars
        mSegmentBarsManager.updateSignalBars(mSignalLayout3D, signalStrength);
        mHealthStats2D.updateSignalStrength(signalStrength);
    }
    
    /**
     * Called when the HUD elements need to be updated
     * Implementation of HudElementsUpdater.SignalStrengthUpdateListener
     */
    @Override
    public void onUpdateSignalDisplay() {
        // Get current signal strength from monitor
        if (mSignalMonitor != null) {
            int signalStrength = mSignalMonitor.getSignalStrength();
            
            // Update signal bars
            mSegmentBarsManager.updateSignalBars(mSignalLayout3D, signalStrength);
        }
    }
    
    /**
     * Called when the display mode changes
     * Implementation of DisplayModeManager.DisplayModeChangeListener
     */
    @Override
    public void onDisplayModeChanged(boolean is3DMode) {
        Log.d(TAG, "Display mode changed to: " + (is3DMode ? "3D" : "2D"));
        
        // Force signal strength update
        if (mSignalMonitor != null) {
            int signalStrength = mSignalMonitor.getSignalStrength();
            Log.d(TAG, "Forcing signal strength update: " + signalStrength);
            
            // Update signal bars directly
            mSegmentBarsManager.updateSignalBars(mSignalLayout3D, signalStrength);
        }
        
        // Update all HUD elements
        mHudUpdater.updateAllElements();
        
        // Update minimap display mode
        if (mMinimapManager != null) {
            mMinimapManager.setDisplayMode(is3DMode);
        }
    }
    
    /**
     * Handle lifecycle events for the presentation
     */
    
    @Override
    protected void onStart() {
        super.onStart();
        if (mMinimapManager != null) {
            mMinimapManager.onResume();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (mMinimapManager != null) {
            mMinimapManager.onPause();
        }
    }
    
    /**
     * Handle low memory condition
     * This is not an override since Presentation doesn't have this method
     */
    public void handleLowMemory() {
        if (mMinimapManager != null) {
            mMinimapManager.onLowMemory();
        }
    }
    
    /**
     * Save instance state for the minimap
     * This is not an override since the signature is different from Presentation
     */
    public void saveMinimapState(Bundle outState) {
        if (mMinimapManager != null) {
            mMinimapManager.onSaveInstanceState(outState);
        }
    }
}
