package com.eden.demo.sensor.hud;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eden.demo.sensor.HealthStats;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Class responsible for updating HUD elements (time, battery, date)
 */
public class HudElementsUpdater {
    private static final String TAG = "HudElementsUpdater";
    
    // Context
    private Context mContext;
    
    // Handler for periodic updates
    private Handler mUpdateHandler;
    private boolean mIsUpdating = false;
    
    // Date formats
    private SimpleDateFormat mTimeFormat;
    private SimpleDateFormat mMonthFormat;
    
    // UI elements for 2D mode (now the only mode)
    private HealthStats mHealthStats2D;
    
    // Callback interface for signal strength updates
    private ProgressBar mBatteryBar2D;
    private TextView mDayDisplay2D;
    private TextView mMonthDisplay2D;
    
    // Callback interface for signal strength updates
    public interface SignalStrengthUpdateListener {
        void onUpdateSignalDisplay();
    }
    
    private SignalStrengthUpdateListener mSignalListener;

    /**
     * Constructor
     * 
     * @param context Application context
     * @param signalListener Listener for signal strength updates
     */
    public HudElementsUpdater(Context context, SignalStrengthUpdateListener signalListener) {
        mContext = context;
        mSignalListener = signalListener;
        
        // Initialize date formats
        mTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        mMonthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        
        // Initialize handler for updates
        mUpdateHandler = new Handler();
    }

    /**
     * Set the UI elements for 2D mode
     * 
     * @param healthStats HealthStats view containing all 2D mode UI elements
     */
    public void set2DModeElements(HealthStats healthStats) {
        mHealthStats2D = healthStats;
    }
    
    /**
     * Start periodic updates for HUD elements
     */
    public void startUpdates() {
        if (mIsUpdating) {
            return;
        }
        
        mIsUpdating = true;
        updateAllElements(); // Update immediately
        
        // Schedule periodic updates
        mUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsUpdating) {
                    updateAllElements();
                    // Schedule next update in 1 second
                    mUpdateHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }
    
    /**
     * Stop periodic updates
     */
    public void stopUpdates() {
        mIsUpdating = false;
        if (mUpdateHandler != null) {
            mUpdateHandler.removeCallbacksAndMessages(null);
        }
    }
    
    /**
     * Update all HUD elements
     */
    public void updateAllElements() {
        updateTimeDisplay();
        updateBatteryDisplay();
        updateDateDisplay();
        
        // Update signal display through listener
        if (mSignalListener != null) {
            mSignalListener.onUpdateSignalDisplay();
        }
    }
    
    /**
     * Update the time display
     */
    private void updateTimeDisplay() {
        String currentTime = mTimeFormat.format(new Date());

        if (mHealthStats2D != null) {
            mHealthStats2D.updateTime(currentTime);
        }
    }
    
    /**
     * Update the battery display
     */
    private void updateBatteryDisplay() {
        int batteryPct = getBatteryPercentage();
        
        if (mHealthStats2D != null) {
            mHealthStats2D.updateBatteryLevel(batteryPct);
        }
    }
    
    /**
     * Update the date display
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

        // Update 2D mode displays (now the only mode)
        if (mHealthStats2D != null) {
            mHealthStats2D.updateDate(dayDisplay, monthDisplay);
        }
    }

    /**
     * Get the current battery percentage
     * 
     * @return Battery percentage (0-100)
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
    
    /**
     * Release resources
     */
    public void release() {
        stopUpdates();
        mSignalListener = null;
    }
}
