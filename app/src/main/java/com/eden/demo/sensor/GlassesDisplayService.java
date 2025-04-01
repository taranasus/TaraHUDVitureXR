package com.eden.demo.sensor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Presentation;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.eden.demo.sensor.databinding.GlassesDisplayBinding;
import com.viture.sdk.ArCallback;
import com.viture.sdk.ArManager;
import com.viture.sdk.Constants;

/**
 * A foreground service that maintains the XR glasses display even when the app is in the background
 */
public class GlassesDisplayService extends Service {
    private static final String TAG = "GlassesDisplayService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "TaraHUD_Channel";
    
    // Display management
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener;
    private GlassesPresentation mGlassesPresentation;
    private Display mExternalDisplay;
    
    // VITURE SDK
    private ArManager mArManager;
    private ArCallback mCallback;
    private int mSdkInitSuccess = -1;
    private boolean mIs3DModeEnabled = true; // Default to 3D mode
    
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
        
        // Initialize display management
        initDisplayManager();
        
        // Initialize VITURE SDK
        initVitureSDK();
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
        
        // Clean up the presentation
        if (mGlassesPresentation != null) {
            mGlassesPresentation.dismiss();
            mGlassesPresentation = null;
        }
        
        // Unregister display listener
        if (mDisplayManager != null && mDisplayListener != null) {
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        }
        
        // Release VITURE SDK
        if (mArManager != null) {
            if (mCallback != null) {
                mArManager.unregisterCallback(mCallback);
            }
            mArManager.release();
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
    
    /**
     * Initialize the DisplayManager to detect external displays (XR glasses)
     */
    private void initDisplayManager() {
        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        
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
        Display[] displays = mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        
        if (displays.length > 0) {
            // Use the first external display
            if (mGlassesPresentation == null || mExternalDisplay == null) {
                mExternalDisplay = displays[0];
                showPresentation(mExternalDisplay);
            }
        } else {
            Log.d(TAG, "No external display found");
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
        mGlassesPresentation = new GlassesPresentation(this, display);
        
        try {
            mGlassesPresentation.show();
            Log.d(TAG, "Presentation shown on external display");
        } catch (WindowManager.InvalidDisplayException e) {
            Log.e(TAG, "Unable to show presentation on external display", e);
            mGlassesPresentation = null;
        }
    }
    
    /**
     * Initialize the VITURE SDK for XR glasses
     */
    private void initVitureSDK() {
        mArManager = ArManager.getInstance(this);
        mSdkInitSuccess = mArManager.init();
        mArManager.setLogOn(true);
        
        // Enable 3D mode automatically after initialization
        if (mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
            enable3DMode();
        }
        
        mCallback = new ArCallback() {
            @Override
            public void onEvent(int msgId, byte[] event, long timestamp) {
                Log.d(TAG, "onEvent msgId: " + msgId);

                if (msgId == Constants.EVENT_ID_INIT) {
                    mSdkInitSuccess = byteArrayToInt(event, 0, event.length);
                    Log.d(TAG, "SDK initialization: " + mSdkInitSuccess);
                    
                    // Enable 3D mode once initialization is confirmed successful
                    if (mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
                        enable3DMode();
                    }
                } else if (msgId == Constants.EVENT_ID_3D) {
                    int state = byteArrayToInt(event, 0, event.length);
                    final String stateStr = (state == Constants.STATE_ON ? "ON" : "OFF");
                    Log.d(TAG, "3D mode state: " + stateStr);
                }
            }

            @Override
            public void onImu(long timestamp, byte[] imuData) {
                // Not using IMU data
            }
        };
        
        // Register the callback
        mArManager.registerCallback(mCallback);
    }
    
    /**
     * Enable 3D mode for the XR glasses
     */
    private void enable3DMode() {
        int result = mArManager.set3D(true);
        if (result == Constants.ERR_SET_SUCCESS) {
            mIs3DModeEnabled = true;
            Log.d(TAG, "3D mode enabled successfully");
            
            // Update presentation if it exists
            if (mGlassesPresentation != null) {
                mGlassesPresentation.setDisplayMode(true);
            }
        } else {
            Log.e(TAG, "Failed to enable 3D mode: " + result);
        }
    }
    
    /**
     * Toggle between 2D and 3D display modes
     */
    public boolean toggleDisplayMode(boolean enable3D) {
        if (mArManager != null && mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
            int result = mArManager.set3D(enable3D);
            if (result == Constants.ERR_SET_SUCCESS) {
                mIs3DModeEnabled = enable3D;
                
                // Update the presentation's display mode if it exists
                if (mGlassesPresentation != null) {
                    mGlassesPresentation.setDisplayMode(enable3D);
                }
                return true;
            }
        }
        return false;
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
     * Get the current 3D mode state
     */
    public boolean is3DModeEnabled() {
        return mIs3DModeEnabled;
    }
    
    /**
     * Utility method to convert byte array to int
     */
    private int byteArrayToInt(byte[] bytes, int offset, int length) {
        if (bytes == null) {
            return 0;
        }
        int value = 0;
        int len = bytes.length;
        if (offset > len || offset < 0) {
            return 0;
        }
        int right = offset + length;
        if (right > len) {
            right = len;
        }
        for (int i = offset; i < right; i++) {
            int shift = (i - offset) * 8;
            value += (bytes[i] & 0x000000FF) << shift;
        }
        return value;
    }
    
    /**
     * Custom Presentation class for displaying content on the XR glasses
     */
    private class GlassesPresentation extends Presentation {
        private TextView mGlassesRightEyeText;
        private TextView mGlasses2DText;
        private GlassesDisplayBinding mGlassesBinding;
        private boolean mCurrentlyIn3DMode = true;
        
        public GlassesPresentation(Context context, Display display) {
            super(context, display);
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
            
            // Get references to the green box text views
            mGlassesRightEyeText = mGlassesBinding.glassesRightEyeText; // For 3D mode
            mGlasses2DText = mGlassesBinding.glasses2dText; // For 2D mode
            
            // Set initial display mode
            setDisplayMode(mIs3DModeEnabled);
            
            // Enable immersive mode for the presentation
            enablePresentationImmersiveMode();
        }
        
        /**
         * Set the display mode (2D or 3D)
         */
        public void setDisplayMode(boolean is3DMode) {
            mCurrentlyIn3DMode = is3DMode;
            
            if (is3DMode) {
                // In 3D mode: 3840x1080, only right text visible
                if (mGlassesRightEyeText != null) {
                    mGlassesRightEyeText.setVisibility(View.VISIBLE);
                }
                if (mGlasses2DText != null) {
                    mGlasses2DText.setVisibility(View.GONE);
                }
            } else {
                // In 2D mode: 1920x1080, both eyes see the same text
                if (mGlassesRightEyeText != null) {
                    mGlassesRightEyeText.setVisibility(View.GONE);
                }
                if (mGlasses2DText != null) {
                    mGlasses2DText.setVisibility(View.VISIBLE);
                }
            }
        }
        
        /**
         * Set the visibility of the green box
         */
        public void setGreenBoxVisibility(boolean visible) {
            // Update visibility based on current mode
            if (mCurrentlyIn3DMode) {
                if (mGlassesRightEyeText != null) {
                    mGlassesRightEyeText.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            } else {
                if (mGlasses2DText != null) {
                    mGlasses2DText.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
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
}
