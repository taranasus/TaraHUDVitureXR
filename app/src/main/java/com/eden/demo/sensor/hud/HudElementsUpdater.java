package com.eden.demo.sensor.hud;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    
    // UI elements for 3D mode
    private TextView mTimeDisplay3D;
    private ProgressBar mBatteryBar3D;
    private TextView mDayDisplay3D;
    private TextView mMonthDisplay3D;
    
    // UI elements for 2D mode
    private TextView mTimeDisplay2D;
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
     * Set the UI elements for 3D mode
     * 
     * @param timeDisplay Time display TextView
     * @param batteryBar Battery progress bar
     * @param dayDisplay Day display TextView
     * @param monthDisplay Month display TextView
     */
    public void set3DModeElements(TextView timeDisplay, ProgressBar batteryBar, 
                                 TextView dayDisplay, TextView monthDisplay) {
        mTimeDisplay3D = timeDisplay;
        mBatteryBar3D = batteryBar;
        mDayDisplay3D = dayDisplay;
        mMonthDisplay3D = monthDisplay;
    }
    
    /**
     * Set the UI elements for 2D mode
     * 
     * @param timeDisplay Time display TextView
     * @param batteryBar Battery progress bar
     * @param dayDisplay Day display TextView
     * @param monthDisplay Month display TextView
     */
    public void set2DModeElements(TextView timeDisplay, ProgressBar batteryBar, 
                                 TextView dayDisplay, TextView monthDisplay) {
        mTimeDisplay2D = timeDisplay;
        mBatteryBar2D = batteryBar;
        mDayDisplay2D = dayDisplay;
        mMonthDisplay2D = monthDisplay;
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
        
        if (mTimeDisplay3D != null) {
            mTimeDisplay3D.setText(currentTime);
        }
        
        if (mTimeDisplay2D != null) {
            mTimeDisplay2D.setText(currentTime);
        }
    }
    
    /**
     * Update the battery display
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
