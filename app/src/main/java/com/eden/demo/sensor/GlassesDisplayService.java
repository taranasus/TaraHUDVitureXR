package com.eden.demo.sensor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * A foreground service that maintains the XR glasses display even when the app is in the background.
 * This service coordinates between the VitureSDKManager and DisplayPresentationManager.
 */
public class GlassesDisplayService extends Service implements 
        VitureSDKManager.VitureSDKEventListener,
        DisplayPresentationManager.DisplayEventListener {
    
    private static final String TAG = "GlassesDisplayService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "TaraHUD_Channel";
    
    // Manager classes
    private VitureSDKManager mVitureSDKManager;
    private DisplayPresentationManager mDisplayManager;
    
    // Wake locks for power management
    private PowerManager.WakeLock mCpuWakeLock; // Keeps CPU running
    private PowerManager.WakeLock mScreenWakeLock; // Keeps screen on

    // Screen state tracking
    private boolean mIsScreenOn = true;
    private BroadcastReceiver mScreenStateReceiver;
    
    // Binder for activity communication
    private final IBinder mBinder = new LocalBinder();
    
    public class LocalBinder extends Binder {
        GlassesDisplayService getService() {
            return GlassesDisplayService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        // Initialize managers - DisplayManager first, then VitureSDKManager
        // This ensures the DisplayManager is ready when VitureSDKManager's callbacks are triggered
        mDisplayManager = new DisplayPresentationManager(this, this);
        mVitureSDKManager = new VitureSDKManager(this, this);
        
        // Initialize wake locks
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        
        // CPU wake lock to keep the service running
        mCpuWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, 
                "TaraHUD:GlassesDisplayCpuWakeLock");
        mCpuWakeLock.setReferenceCounted(false);
        
        // Screen wake lock to keep the screen (and thus glasses) on
        mScreenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "TaraHUD:GlassesDisplayScreenWakeLock");
        mScreenWakeLock.setReferenceCounted(false);
        
        // Initialize and register screen on/off receiver
        registerScreenStateReceiver();
    }
    
    /**
     * Register a receiver for screen on/off events
     */
    private void registerScreenStateReceiver() {
        mScreenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    mIsScreenOn = false;
                    Log.d(TAG, "Screen turned OFF - Reactivating screen to maintain glasses display");
                    
                    // Force the screen to stay on to keep glasses running
                    acquireScreenWakeLock();
                    
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    mIsScreenOn = true;
                    Log.d(TAG, "Screen turned ON");
                    
                    // Refresh the glasses display
                    if (mDisplayManager != null && mDisplayManager.areGlassesConnected()) {
                        Log.d(TAG, "Refreshing glasses display after screen on");
                        mDisplayManager.refreshDisplay();
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenStateReceiver, filter);
        Log.d(TAG, "Screen state receiver registered");
    }
    
    /**
     * Acquire the screen wake lock to force the screen to stay on
     */
    private void acquireScreenWakeLock() {
        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire(10*60*1000L); // 10 minutes timeout for safety
            Log.d(TAG, "Screen wake lock acquired to keep glasses display active");
        }
    }
    
    /**
     * Ensure glasses display stays active by keeping screen on
     */
    private void ensureGlassesDisplayActive() {
        if (mDisplayManager != null && mDisplayManager.areGlassesConnected()) {
            Log.d(TAG, "Forcing display connection to remain active");
            mDisplayManager.ensureDisplayActive();
            
            // Use the VitureSDK to specifically keep glasses powered
            if (mVitureSDKManager != null && mVitureSDKManager.isInitialized()) {
                mVitureSDKManager.keepDisplayActiveOnScreenOff();
            }
            
            // Acquire the screen wake lock to keep the screen on
            acquireScreenWakeLock();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        // Create notification channel for Android O and above
        createNotificationChannel();
        
        // Build a notification for the foreground service
        Notification notification = buildNotification();
        
        // Start as a foreground service
        startForeground(NOTIFICATION_ID, notification);
        
        // Acquire CPU wake lock to keep service running
        if (mCpuWakeLock != null && !mCpuWakeLock.isHeld()) {
            mCpuWakeLock.acquire();
            Log.d(TAG, "CPU wake lock acquired");
        }
        
        // Acquire screen wake lock to keep screen on
        acquireScreenWakeLock();
        
        // If we get killed, restart with our last intent
        return START_REDELIVER_INTENT;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        
        // Unregister the screen state receiver
        if (mScreenStateReceiver != null) {
            try {
                unregisterReceiver(mScreenStateReceiver);
                Log.d(TAG, "Screen state receiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error unregistering screen state receiver", e);
            }
            mScreenStateReceiver = null;
        }
        
        // Release CPU wake lock if held
        if (mCpuWakeLock != null && mCpuWakeLock.isHeld()) {
            mCpuWakeLock.release();
            Log.d(TAG, "CPU wake lock released");
        }
        
        // Release screen wake lock if held
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
            Log.d(TAG, "Screen wake lock released");
        }
        
        // Clean up managers
        if (mDisplayManager != null) {
            mDisplayManager.release();
        }
        
        if (mVitureSDKManager != null) {
            mVitureSDKManager.release();
        }
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TaraHUD Display Service",
                    NotificationManager.IMPORTANCE_LOW); // Low importance to avoid sound and vibration
            
            channel.setDescription("Keeps the XR glasses display active");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Build a notification for the foreground service
     */
    private Notification buildNotification() {
        // Create an intent to open the app when notification is tapped
        Intent notificationIntent = new Intent(this, FullscreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TaraHUD Active")
                .setContentText("HUD is active on XR glasses - Screen kept on for glasses display")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);
                
        return builder.build();
    }
    
    //
    // Public API for the Activity
    //
    
    /**
     * Toggle display mode (always forces 2D mode)
     * 
     * @param enable3D Ignored - always sets to 2D mode
     * @return True if the operation was successful
     */
    public boolean toggleDisplayMode(boolean enable3D) {
        // Always force 2D mode regardless of the parameter
        if (enable3D) {
            Log.d(TAG, "3D mode requested but forcing 2D mode");
        }
        
        boolean success = mVitureSDKManager.toggleDisplayMode(false);
        if (success) {
            mDisplayManager.setDisplayMode(false);
        }
        return success;
    }

    /**
     * Show or hide the green box on the glasses presentation
     * 
     * @param show True to show the green box, false to hide it
     * @return True if the operation was successful
     */
    public boolean showGreenBox(boolean show) {
        return mDisplayManager.showGreenBox(show);
    }
    
    /**
     * Check if the glasses are connected
     * 
     * @return True if glasses are connected
     */
    public boolean areGlassesConnected() {
        return mDisplayManager.areGlassesConnected();
    }
    
    /**
     * Get the current 3D mode state
     * 
     * @return Always false (2D mode)
     */
    public boolean is3DModeEnabled() {
        return false; // Always return false (2D mode)
    }
    
    /**
     * Restart signal strength monitoring after permission is granted
     * This is called from the activity when READ_PHONE_STATE permission is granted
     */
    public void restartSignalMonitoring() {
        Log.d(TAG, "Restarting signal strength monitoring after permission granted");
        
        if (mDisplayManager != null && mDisplayManager.areGlassesConnected()) {
            // Get the current presentation and restart signal monitoring
            GlassesPresentation presentation = mDisplayManager.getGlassesPresentation();
            if (presentation != null) {
                Log.d(TAG, "Reinitializing signal strength monitoring in presentation");
                presentation.reinitializeSignalStrengthMonitoring();
            } else {
                Log.e(TAG, "Cannot restart signal monitoring - presentation is null");
            }
        } else {
            Log.e(TAG, "Cannot restart signal monitoring - glasses not connected");
        }
    }
    
    /**
     * Initialize or refresh minimap after location permission is granted
     * This is called from the activity when ACCESS_FINE_LOCATION permission is granted
     */
    public void initializeMinimap() {
        Log.d(TAG, "Initializing minimap after location permission granted");
        
        if (mDisplayManager != null && mDisplayManager.areGlassesConnected()) {
            // Get the current presentation and initialize minimap
            GlassesPresentation presentation = mDisplayManager.getGlassesPresentation();
            if (presentation != null) {
                Log.d(TAG, "Initializing minimap in presentation");
                presentation.initializeMinimap();
            } else {
                Log.e(TAG, "Cannot initialize minimap - presentation is null");
            }
        } else {
            Log.e(TAG, "Cannot initialize minimap - glasses not connected");
        }
    }
    
    /**
     * Handle low memory condition
     * This is called when the system is low on memory
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        if (mDisplayManager != null && mDisplayManager.areGlassesConnected()) {
            // Get the current presentation and handle low memory
            GlassesPresentation presentation = mDisplayManager.getGlassesPresentation();
            if (presentation != null) {
                presentation.handleLowMemory();
            }
        }
    }
    
    //
    // Callback implementations
    //
    
    @Override
    public void onSDKInitializationComplete(boolean success) {
        Log.d(TAG, "SDK initialization complete: " + success);
    }
    
    @Override
    public void on3DModeStateChanged(boolean enabled) {
        Log.d(TAG, "3D mode state changed: " + enabled);
        // Update display presentation with new 3D mode
        if (mDisplayManager != null) {
            mDisplayManager.setDisplayMode(enabled);
        }
    }
    
    @Override
    public void onExternalDisplayConnected(boolean connected) {
        Log.d(TAG, "External display connection state: " + connected);
    }
}
