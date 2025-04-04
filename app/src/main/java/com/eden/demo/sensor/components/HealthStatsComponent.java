package com.eden.demo.sensor.components;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.eden.demo.sensor.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Component for displaying health statistics in the top-left corner
 * (Time, Battery, Signal, Date, etc.)
 */
public class HealthStatsComponent implements GlassesComponent {
    private final Context mContext;
    private final LayoutInflater mInflater;
    
    // Views for 3D mode
    private View mRootView3D;
    private LinearLayout mHudLayout3D;
    private TextView mTimeDisplay3D;
    private ProgressBar mBatteryBar3D;
    private LinearLayout mSignalLayout3D;
    private LinearLayout mSegmentBar3D;
    private TextView mDayDisplay3D;
    private TextView mMonthDisplay3D;
    
    // Views for 2D mode
    private View mRootView2D;
    private LinearLayout mHudLayout2D;
    private TextView mTimeDisplay2D;
    private ProgressBar mBatteryBar2D;
    private LinearLayout mSignalLayout2D;
    private LinearLayout mSegmentBar2D;
    private TextView mDayDisplay2D;
    private TextView mMonthDisplay2D;
    
    // State variables
    private boolean mIs3DMode = true;
    private boolean mIsVisible = true;
    private Handler mUpdateHandler;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private int mSignalStrength = 0;
    private SimpleDateFormat mTimeFormat;
    private SimpleDateFormat mMonthFormat;
    
    // Constants
    private static final int MAX_SIGNAL_BARS = 15;
    private static final int MAX_SEGMENT_BARS = 10;
    private Drawable mSignalSegmentDrawable;
    private Drawable mSegmentItemDrawable;
    
    public HealthStatsComponent(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        
        // Initialize date formats
        mTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        mMonthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        
        // Initialize drawables
        mSignalSegmentDrawable = context.getDrawable(R.drawable.signal_bar_item);
        mSegmentItemDrawable = context.getDrawable(R.drawable.segment_bar_item);
    }
    
    @Override
    public void initialize() {
        // Initialize 3D view
        mRootView3D = mInflater.inflate(R.layout.component_health_stats_3d, null);
        mHudLayout3D = mRootView3D.findViewById(R.id.hud_layout_3d);
        mTimeDisplay3D = mRootView3D.findViewById(R.id.time_display_3d);
        mBatteryBar3D = mRootView3D.findViewById(R.id.battery_bar_3d);
        mSignalLayout3D = mRootView3D.findViewById(R.id.signal_layout_3d);
        mSegmentBar3D = mRootView3D.findViewById(R.id.segment_bar_3d);
        mDayDisplay3D = mRootView3D.findViewById(R.id.day_display_3d);
        mMonthDisplay3D = mRootView3D.findViewById(R.id.month_display_3d);
        
        // Initialize 2D view
        mRootView2D = mInflater.inflate(R.layout.component_health_stats_2d, null);
        mHudLayout2D = mRootView2D.findViewById(R.id.hud_layout_2d);
        mTimeDisplay2D = mRootView2D.findViewById(R.id.time_display_2d);
        mBatteryBar2D = mRootView2D.findViewById(R.id.battery_bar_2d);
        mSignalLayout2D = mRootView2D.findViewById(R.id.signal_layout_2d);
        mSegmentBar2D = mRootView2D.findViewById(R.id.segment_bar_2d);
        mDayDisplay2D = mRootView2D.findViewById(R.id.day_display_2d);
        mMonthDisplay2D = mRootView2D.findViewById(R.id.month_display_2d);
        
        // Apply the custom Rajdhani font to all text views
        Typeface rajdhaniTypeface = ResourcesCompat.getFont(mContext, R.font.rajdhani_medium);
        if (rajdhaniTypeface != null) {
            applyFontToAllTextViews(rajdhaniTypeface);
        }
        
        // Create signal and segment bars
        createSignalBars(mSignalLayout3D);
        createSignalBars(mSignalLayout2D);
        createSegmentBars(mSegmentBar3D);
        createSegmentBars(mSegmentBar2D);
        
        // Initialize signal strength monitoring
        initializeSignalStrengthMonitoring();
        
        // Start updates
        startUpdates();
    }
    
    @Override
    public void setDisplayMode(boolean is3DMode) {
        mIs3DMode = is3DMode;
        updateVisibility();
    }
    
    @Override
    public void setVisibility(boolean visible) {
        mIsVisible = visible;
        updateVisibility();
    }
    
    private void updateVisibility() {
        if (mRootView3D != null) {
            mRootView3D.setVisibility(
                mIsVisible && mIs3DMode ? View.VISIBLE : View.GONE);
        }
        
        if (mRootView2D != null) {
            mRootView2D.setVisibility(
                mIsVisible && !mIs3DMode ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void update() {
        updateTimeDisplay();
        updateBatteryDisplay();
        updateSignalDisplay();
        updateDateDisplay();
    }
    
    @Override
    public View get3DView() {
        return mRootView3D;
    }
    
    @Override
    public View get2DView() {
        return mRootView2D;
    }
    
    @Override
    public void cleanup() {
        if (mUpdateHandler != null) {
            mUpdateHandler.removeCallbacksAndMessages(null);
            mUpdateHandler = null;
        }
        
        if (mTelephonyManager != null && mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
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
     * Start periodic updates for HUD display elements
     */
    private void startUpdates() {
        mUpdateHandler = new Handler();
        update(); // Initial update
        
        // Schedule periodic updates
        mUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                update();
                // Schedule next update in 1 second
                mUpdateHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    /**
     * Create signal strength bar segments
     */
    private void createSignalBars(LinearLayout container) {
        container.removeAllViews();
        
        // No spacing at the beginning to align with bars above
        View spacer = new View(mContext);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(spacer, spacerParams);
        
        // Create individual signal segments with small gaps between them
        for (int i = 0; i < MAX_SIGNAL_BARS; i++) {
            View segment = new View(mContext);
            segment.setBackground(mSignalSegmentDrawable.getConstantState().newDrawable());
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    5, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMarginEnd(2);
            
            container.addView(segment, params);
        }
    }
    
    /**
     * Create segment bar items
     */
    private void createSegmentBars(LinearLayout container) {
        container.removeAllViews();
        
        // Add spacing at the beginning
        View spacer = new View(mContext);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                5, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(spacer, spacerParams);
        
        // Create individual segments with gaps between them
        for (int i = 0; i < MAX_SEGMENT_BARS; i++) {
            View segment = new View(mContext);
            segment.setBackground(mSegmentItemDrawable.getConstantState().newDrawable());
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    8, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMarginEnd(3);
            
            container.addView(segment, params);
        }
        
        // Set 50% of segments visible
        updateSegmentBars(container, MAX_SEGMENT_BARS / 2);
    }
    
    /**
     * Initialize monitoring of signal strength
     */
    private void initializeSignalStrengthMonitoring() {
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        
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
     * Update the segment bars (50% filled)
     */
    private void updateSegmentBars(LinearLayout container, int visibleCount) {
        if (container == null || container.getChildCount() <= 1) return;
        
        // Skip the first child which is the spacer
        for (int i = 1; i < container.getChildCount(); i++) {
            View segment = container.getChildAt(i);
            
            // Show half the segments
            if (i - 1 < visibleCount) {
                segment.setVisibility(View.VISIBLE);
            } else {
                segment.setVisibility(View.INVISIBLE);
            }
        }
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
        Intent batteryStatus = mContext.registerReceiver(null, iFilter);
        
        int level = 0;
        int scale = 1;
        
        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        }
        
        // Convert to percentage
        return (int)(level * 100 / (float)scale);
    }
}
