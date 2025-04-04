package com.eden.demo.sensor;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.eden.demo.sensor.databinding.GlassesDisplayBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Custom Presentation class for displaying content on the XR glasses with Cyberpunk styled HUD
 */
public class GlassesPresentation extends Presentation {
    // Layout bindings
    private GlassesDisplayBinding mGlassesBinding;
    private boolean mCurrentlyIn3DMode = true;
    
    // Handler for periodic updates
    private Handler mTimeUpdateHandler;
    
    // UI elements for 3D mode
    private TextView mTimeDisplay3D;
    private ProgressBar mBatteryBar3D;
    private LinearLayout mSignalLayout3D;
    private TextView mDayDisplay3D;
    private TextView mMonthDisplay3D;
    private LinearLayout mHudLayout3D;
    
    // UI elements for 2D mode
    private TextView mTimeDisplay2D;
    private ProgressBar mBatteryBar2D;
    private LinearLayout mSignalLayout2D;
    private TextView mDayDisplay2D;
    private TextView mMonthDisplay2D;
    private LinearLayout mHudLayout2D;
    
    // Phone state monitoring
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private int mSignalStrength = 0;
    
    // Date formats
    private SimpleDateFormat mTimeFormat;
    private SimpleDateFormat mMonthFormat;
    
    // Signal bar segments
    private static final int MAX_SIGNAL_BARS = 15;
    private Drawable mSignalSegmentDrawable;
    
    public GlassesPresentation(Context context, Display display) {
        super(context, display);
        
        // Initialize date formats
        mTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        mMonthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        
        // Initialize signal segment drawable
        mSignalSegmentDrawable = context.getDrawable(R.drawable.signal_bar_item);
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
        
        // Initialize UI references for 3D mode
        mHudLayout3D = mGlassesBinding.hudLayoutRightEye;
        mTimeDisplay3D = mGlassesBinding.timeDisplayRight;
        mBatteryBar3D = mGlassesBinding.batteryBarRight;
        mSignalLayout3D = mGlassesBinding.signalLayoutRight;
        mDayDisplay3D = mGlassesBinding.dayDisplayRight;
        mMonthDisplay3D = mGlassesBinding.monthDisplayRight;
        
        // Initialize UI references for 2D mode
        mHudLayout2D = mGlassesBinding.hudLayout2d;
        mTimeDisplay2D = mGlassesBinding.timeDisplay2d;
        mBatteryBar2D = mGlassesBinding.batteryBar2d;
        mSignalLayout2D = mGlassesBinding.signalLayout2d;
        mDayDisplay2D = mGlassesBinding.dayDisplay2d;
        mMonthDisplay2D = mGlassesBinding.monthDisplay2d;
        
        // Apply the custom Rajdhani font to all text views
        Typeface rajdhaniTypeface = ResourcesCompat.getFont(getContext(), R.font.rajdhani_medium);
        if (rajdhaniTypeface != null) {
            applyFontToAllTextViews(rajdhaniTypeface);
        }
        
        // Create signal bar segments
        createSignalBars(mSignalLayout3D);
        createSignalBars(mSignalLayout2D);
        
        // Initialize telephony manager for signal strength
        initializeSignalStrengthMonitoring();
        
        // Set initial display mode
        setDisplayMode(true); // Default to 3D mode
        
        // Enable immersive mode for the presentation
        enablePresentationImmersiveMode();
        
        // Start the timer to update all HUD elements
        startHudUpdates();
    }

    /**
     * Apply the Rajdhani font to all text views in the HUD
     */
    private void applyFontToAllTextViews(Typeface typeface) {
        // 3D mode text views
        mTimeDisplay3D.setTypeface(typeface);
        mDayDisplay3D.setTypeface(typeface);
        mMonthDisplay3D.setTypeface(typeface);
        
        // 2D mode text views
        mTimeDisplay2D.setTypeface(typeface);
        mDayDisplay2D.setTypeface(typeface);
        mMonthDisplay2D.setTypeface(typeface);
    }
    
    /**
     * Create signal strength bar segments
     */
    private void createSignalBars(LinearLayout container) {
        container.removeAllViews();
        
        // Add spacing at the beginning
        View spacer = new View(getContext());
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                5, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(spacer, spacerParams);
        
        // Create individual signal segments with small gaps between them
        for (int i = 0; i < MAX_SIGNAL_BARS; i++) {
            View segment = new View(getContext());
            segment.setBackground(mSignalSegmentDrawable.getConstantState().newDrawable());
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    5, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMarginEnd(2);
            
            container.addView(segment, params);
        }
    }
    
    /**
     * Initialize monitoring of signal strength
     */
    private void initializeSignalStrengthMonitoring() {
        mTelephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        
        if (mTelephonyManager != null) {
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // On Android 10+, use direct methods
                        if (signalStrength.getCellSignalStrengths().size() > 0) {
                            int level = signalStrength.getCellSignalStrengths().get(0).getLevel();
                            // Android provides 0-4 levels, scale to our 0-15 range
                            mSignalStrength = (level * MAX_SIGNAL_BARS) / 4;
                            updateSignalDisplay();
                        }
                    } else {
                        // Older versions, use reflection or simplified approach
                        // For simplicity, just use the legacy getLevel method and scale
                        int level = 2; // Default mid-level
                        try {
                            level = signalStrength.getLevel();
                        } catch (Exception e) {
                            // Fallback if method not available
                        }
                        // Scale from 0-4 to 0-15
                        mSignalStrength = (level * MAX_SIGNAL_BARS) / 4;
                        updateSignalDisplay();
                    }
                }
            };
            
            // Start listening for signal strength changes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires explicit permission check
                try {
                    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                } catch (SecurityException e) {
                    // Handle case where permission is not granted
                    // Default to medium signal level
                    mSignalStrength = MAX_SIGNAL_BARS / 2;
                }
            } else {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
    }
    
    /**
     * Start periodic updates for HUD display elements
     */
    private void startHudUpdates() {
        mTimeUpdateHandler = new Handler();
        updateAllHudElements(); // Update immediately
        
        // Schedule periodic updates
        mTimeUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateAllHudElements();
                // Schedule next update in 1 second
                mTimeUpdateHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    /**
     * Stop all updates when presentation is dismissed
     */
    @Override
    public void dismiss() {
        if (mTimeUpdateHandler != null) {
            mTimeUpdateHandler.removeCallbacksAndMessages(null);
        }
        
        // Stop listening for signal strength updates
        if (mTelephonyManager != null && mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
        super.dismiss();
    }
    
    /**
     * Update all HUD elements (time, battery, signal, date)
     */
    private void updateAllHudElements() {
        updateTimeDisplay();
        updateBatteryDisplay();
        updateSignalDisplay();
        updateDateDisplay();
    }
    
    /**
     * Update the time display in military format
     */
    private void updateTimeDisplay() {
        String currentTime = mTimeFormat.format(new Date());
        
        if (mTimeDisplay3D != null) {
            mTimeDisplay3D.setText(currentTime);
        }
        
        if (mTimeDisplay2D != null) {
            mTimeDisplay2D.setText(currentTime);
        }
    }
    
    /**
     * Update the battery percentage display
     */
    private void updateBatteryDisplay() {
        int batteryPct = getBatteryPercentage();
        
        if (mBatteryBar3D != null) {
            mBatteryBar3D.setProgress(batteryPct);
        }
        
        if (mBatteryBar2D != null) {
            mBatteryBar2D.setProgress(batteryPct);
        }
    }
    
    /**
     * Update the signal strength display
     */
    private void updateSignalDisplay() {
        updateSignalBars(mSignalLayout3D, mSignalStrength);
        updateSignalBars(mSignalLayout2D, mSignalStrength);
    }
    
    /**
     * Update the signal strength bars
     */
    private void updateSignalBars(LinearLayout container, int signalLevel) {
        if (container == null || container.getChildCount() <= 1) return;
        
        // Skip the first child which is the spacer
        for (int i = 1; i < container.getChildCount(); i++) {
            View segment = container.getChildAt(i);
            
            // Activate segments up to the signal level
            // (i - 1 because the first segment is index 1 after spacer)
            if (i - 1 < signalLevel) {
                segment.setVisibility(View.VISIBLE);
            } else {
                segment.setVisibility(View.INVISIBLE);
            }
        }
    }
    
    /**
     * Update the day and month display
     */
    private void updateDateDisplay() {
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        String monthAbbr = mMonthFormat.format(calendar.getTime());
        
        // Format as day of month (e.g., 04)
        String dayDisplay = String.format(Locale.getDefault(), "%02d", dayOfMonth);
        
        // Convert month abbreviation to uppercase
        monthAbbr = monthAbbr.toUpperCase(Locale.getDefault());
        String monthDisplay = monthAbbr;
        
        // Update 3D mode displays
        if (mDayDisplay3D != null) {
            mDayDisplay3D.setText(dayDisplay);
        }
        
        if (mMonthDisplay3D != null) {
            mMonthDisplay3D.setText(monthDisplay);
        }
        
        // Update 2D mode displays
        if (mDayDisplay2D != null) {
            mDayDisplay2D.setText(dayDisplay);
        }
        
        if (mMonthDisplay2D != null) {
            mMonthDisplay2D.setText(monthDisplay);
        }
    }
    
    /**
     * Get the current battery percentage
     */
    private int getBatteryPercentage() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getContext().registerReceiver(null, iFilter);
        
        int level = 0;
        int scale = 1;
        
        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        }
        
        // Convert to percentage
        return (int)(level * 100 / (float)scale);
    }
    
    /**
     * Set the display mode (2D or 3D)
     */
    public void setDisplayMode(boolean is3DMode) {
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
        
        // Update all HUD elements to ensure they're current
        updateAllHudElements();
    }
    
    /**
     * Set the visibility of the HUD display
     */
    public void setGreenBoxVisibility(boolean visible) {
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
    public boolean isGreenBoxVisible() {
        if (mCurrentlyIn3DMode) {
            return mHudLayout3D != null && 
                   mHudLayout3D.getVisibility() == View.VISIBLE;
        } else {
            return mHudLayout2D != null && 
                   mHudLayout2D.getVisibility() == View.VISIBLE;
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
}
