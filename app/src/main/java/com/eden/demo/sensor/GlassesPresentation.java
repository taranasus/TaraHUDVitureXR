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

import com.eden.demo.sensor.databinding.GlassesDisplayBinding;
import com.eden.demo.sensor.hud.DisplayModeManager;
import com.eden.demo.sensor.hud.HudElementsUpdater;
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
    private TextView mTimeDisplay2D;
    private ProgressBar mBatteryBar2D;
    private LinearLayout mSignalLayout2D;
    private LinearLayout mSegmentBar2D;
    private TextView mDayDisplay2D;
    private TextView mMonthDisplay2D;
    private LinearLayout mHudLayout2D;
    
    // HUD component managers
    private SignalStrengthMonitor mSignalMonitor;
    private SegmentBarsManager mSegmentBarsManager;
    private HudElementsUpdater mHudUpdater;
    private DisplayModeManager mDisplayModeManager;
    
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
        mSegmentBarsManager.createSignalBars(mSignalLayout2D);
        mSegmentBarsManager.createSegmentBars(mSegmentBar3D);
        mSegmentBarsManager.createSegmentBars(mSegmentBar2D);
        
        // Set UI elements for HUD updater
        mHudUpdater.set3DModeElements(mTimeDisplay3D, mBatteryBar3D, mDayDisplay3D, mMonthDisplay3D);
        mHudUpdater.set2DModeElements(mTimeDisplay2D, mBatteryBar2D, mDayDisplay2D, mMonthDisplay2D);
        
        // Set layouts for display mode manager
        mDisplayModeManager.setLayouts(mHudLayout3D, mHudLayout2D);
        
        // Initialize signal strength monitoring
        mSignalMonitor.initialize();
        
        // Set initial display mode (3D by default)
        mDisplayModeManager.setDisplayMode(true);
        
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
        mHudLayout2D = mGlassesBinding.hudLayout2d;
        mTimeDisplay2D = mGlassesBinding.timeDisplay2d;
        mBatteryBar2D = mGlassesBinding.batteryBar2d;
        mSignalLayout2D = mGlassesBinding.signalLayout2d;
        mSegmentBar2D = mGlassesBinding.segmentBar2d;
        mDayDisplay2D = mGlassesBinding.dayDisplay2d;
        mMonthDisplay2D = mGlassesBinding.monthDisplay2d;
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
            mTimeDisplay2D.setTypeface(rajdhaniTypeface);
            mDayDisplay2D.setTypeface(rajdhaniTypeface);
            mMonthDisplay2D.setTypeface(rajdhaniTypeface);
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
        
        super.dismiss();
    }
    
    /**
     * Set the display mode (2D or 3D)
     * 
     * @param is3DMode True for 3D mode, false for 2D mode
     */
    public void setDisplayMode(boolean is3DMode) {
        if (mDisplayModeManager != null) {
            mDisplayModeManager.setDisplayMode(is3DMode);
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
        mSegmentBarsManager.updateSignalBars(mSignalLayout2D, signalStrength);
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
            mSegmentBarsManager.updateSignalBars(mSignalLayout2D, signalStrength);
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
            mSegmentBarsManager.updateSignalBars(mSignalLayout2D, signalStrength);
        }
        
        // Update all HUD elements
        mHudUpdater.updateAllElements();
    }
}
