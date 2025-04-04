package com.eden.demo.sensor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
    
    // Wake lock to keep CPU running when screen is off
    private PowerManager.WakeLock mWakeLock;
    
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
        
        // Initialize wake lock to keep CPU running when screen is off
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, 
                "TaraHUD:GlassesDisplayWakeLock");
        mWakeLock.setReferenceCounted(false);
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
        
        // Acquire wake lock to keep CPU running when screen is off
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.d(TAG, "Wake lock acquired");
        }
        
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
        
        // Release wake lock if held
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(TAG, "Wake lock released");
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
                .setContentText("HUD is currently active on XR glasses")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);
                
        return builder.build();
    }
    
    //
    // Public API for the Activity
    //
    
    /**
     * Toggle between 2D and 3D display modes
     * 
     * @param enable3D True to enable 3D mode, false for 2D mode
     * @return True if the operation was successful
     */
    public boolean toggleDisplayMode(boolean enable3D) {
        boolean success = mVitureSDKManager.toggleDisplayMode(enable3D);
        if (success) {
            mDisplayManager.setDisplayMode(enable3D);
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
     * @return True if 3D mode is enabled
     */
    public boolean is3DModeEnabled() {
        return mVitureSDKManager.is3DModeEnabled();
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
