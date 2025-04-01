package com.eden.demo.sensor;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.eden.demo.sensor.databinding.ActivityFullscreenBinding;
import com.eden.demo.sensor.databinding.GlassesDisplayBinding;
import com.viture.sdk.ArCallback;
import com.viture.sdk.ArManager;
import com.viture.sdk.Constants;

/**
 * A simple activity that sets the glasses to 3D mode and displays a green box
 * with "ONLINE" text on the right eye in true fullscreen mode.
 * The phone UI has controls to show/hide the green box.
 */
public class FullscreenActivity extends AppCompatActivity {
    private static final String TAG = "VitureDemo";

    private ActivityFullscreenBinding binding;
    private ArManager mArManager;
    private ArCallback mCallback;
    private int mSdkInitSuccess = -1;
    
    // Display management
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener;
    private GlassesPresentation mGlassesPresentation;
    private Display mExternalDisplay;

    // UI Components
    private Button mBtnShowBox;
    private Button mBtnHideBox;
    private Switch mSwitch3DMode;
    private TextView mStatusText;
    private boolean mIs3DModeEnabled = true; // Default to 3D mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set window flags before content view is set
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize UI components
        mBtnShowBox = binding.btnShowBox;
        mBtnHideBox = binding.btnHideBox;
        mSwitch3DMode = binding.switch3dMode;
        mStatusText = binding.statusText;
        
        // Set click listeners
        mBtnShowBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGreenBox(true);
                updateStatus("Green box visible");
            }
        });
        
        mBtnHideBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGreenBox(false);
                updateStatus("Green box hidden");
            }
        });
        
        // Set up 3D mode toggle
        mSwitch3DMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleDisplayMode(isChecked);
            }
        });
        
        // Initialize display management
        initDisplayManager();
        
        // Initialize VITURE SDK
        initVitureSDK();
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
                    updateStatus("XR glasses disconnected");
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
            updateStatus("No XR glasses detected");
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
            updateStatus("XR glasses connected");
            Log.d(TAG, "Presentation shown on external display");
        } catch (WindowManager.InvalidDisplayException e) {
            Log.e(TAG, "Unable to show presentation on external display", e);
            mGlassesPresentation = null;
            updateStatus("Failed to connect to XR glasses");
        }
    }
    
    /**
     * Update the status text on the phone UI
     */
    private void updateStatus(String status) {
        if (mStatusText != null) {
            mStatusText.setText("Status: " + status);
        }
    }
    
    /**
     * Toggle between 2D and 3D display modes
     */
    private void toggleDisplayMode(boolean enable3D) {
        if (mArManager != null && mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
            int result = mArManager.set3D(enable3D);
            if (result == Constants.ERR_SET_SUCCESS) {
                mIs3DModeEnabled = enable3D;
                updateStatus("Display mode set to: " + (enable3D ? "3D" : "2D"));
                
                // Update the presentation's display mode if it exists
                if (mGlassesPresentation != null) {
                    mGlassesPresentation.setDisplayMode(enable3D);
                }
            } else {
                // Error occurred, revert switch state
                mSwitch3DMode.setChecked(!enable3D);
                updateStatus("Failed to change display mode: Error " + result);
                Toast.makeText(this, "Failed to change display mode", Toast.LENGTH_SHORT).show();
            }
        } else {
            // SDK not initialized, revert switch state
            mSwitch3DMode.setChecked(!enable3D);
            updateStatus("SDK not initialized, cannot change mode");
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show or hide the green box on the glasses presentation
     */
    private void showGreenBox(boolean show) {
        if (mGlassesPresentation != null) {
            mGlassesPresentation.setGreenBoxVisibility(show);
        } else {
            Toast.makeText(this, "XR glasses not connected", Toast.LENGTH_SHORT).show();
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
            updateStatus("VITURE SDK initialized");
        } else {
            updateStatus("VITURE SDK initialization failed");
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enable3DMode();
                                updateStatus("VITURE SDK initialized");
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus("VITURE SDK initialization failed");
                            }
                        });
                    }
                } else if (msgId == Constants.EVENT_ID_3D) {
                    int state = byteArrayToInt(event, 0, event.length);
                    final String stateStr = (state == Constants.STATE_ON ? "ON" : "OFF");
                    Log.d(TAG, "3D mode state: " + stateStr);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus("3D mode: " + stateStr);
                        }
                    });
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
            
            // Ensure switch state matches
            if (mSwitch3DMode != null && !mSwitch3DMode.isChecked()) {
                mSwitch3DMode.setChecked(true);
            }
            
            // Update presentation if it exists
            if (mGlassesPresentation != null) {
                mGlassesPresentation.setDisplayMode(true);
            }
        } else {
            Log.e(TAG, "Failed to enable 3D mode: " + result);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check for external displays
        checkForExternalDisplay();
        
        if (mArManager != null && mCallback != null) {
            mArManager.registerCallback(mCallback);
            
            // Re-apply 3D mode when resuming
            if (mSdkInitSuccess == Constants.ERROR_INIT_SUCCESS) {
                enable3DMode();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mArManager != null && mCallback != null) {
            mArManager.unregisterCallback(mCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
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
            mArManager.release();
        }
    }

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
            
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
            });
        }
        
        /**
         * Set the visibility of the green box
         */
        public void setGreenBoxVisibility(boolean visible) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
            });
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
