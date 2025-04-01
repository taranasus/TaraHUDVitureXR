package com.eden.demo.sensor;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.eden.demo.sensor.databinding.ActivityFullscreenBinding;

/**
 * A simple activity that provides controls for the Viture XR Glasses HUD display.
 * The actual display is managed by GlassesDisplayService which continues to run
 * even when the app is in the background.
 */
public class FullscreenActivity extends AppCompatActivity {
    private static final String TAG = "VitureDemo";

    private ActivityFullscreenBinding binding;
    
    // Service binding
    private GlassesDisplayService mService;
    private boolean mBound = false;
    
    // UI Components
    private Button mBtnShowBox;
    private Button mBtnHideBox;
    private Switch mSwitch3DMode;
    private TextView mStatusText;

    // Defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to the GlassesDisplayService, cast the IBinder and get GlassesDisplayService instance
            GlassesDisplayService.LocalBinder binder = (GlassesDisplayService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            
            // Update UI to reflect service state
            updateUIFromService();
            Log.d(TAG, "Service bound successfully");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected
            mBound = false;
            mService = null;
            updateStatus("Service disconnected");
            Log.d(TAG, "Service unbound unexpectedly");
        }
    };

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
        
        // Start the GlassesDisplayService so it persists even when the activity is not visible
        startGlassesDisplayService();
        
        // Set click listeners
        mBtnShowBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound && mService != null) {
                    boolean success = mService.showGreenBox(true);
                    if (success) {
                        updateStatus("Green box visible");
                    } else {
                        updateStatus("Failed to show green box - glasses not connected");
                    }
                } else {
                    updateStatus("Service not bound");
                }
            }
        });
        
        mBtnHideBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound && mService != null) {
                    boolean success = mService.showGreenBox(false);
                    if (success) {
                        updateStatus("Green box hidden");
                    } else {
                        updateStatus("Failed to hide green box - glasses not connected");
                    }
                } else {
                    updateStatus("Service not bound");
                }
            }
        });
        
        // Set up 3D mode toggle
        mSwitch3DMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleDisplayMode(isChecked);
            }
        });
    }
    
    /**
     * Start the GlassesDisplayService as a foreground service
     */
    private void startGlassesDisplayService() {
        Intent serviceIntent = new Intent(this, GlassesDisplayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "Started GlassesDisplayService");
    }
    
    /**
     * Update UI based on the current service state
     */
    private void updateUIFromService() {
        if (mBound && mService != null) {
            // Update 3D mode switch without triggering the listener
            mSwitch3DMode.setOnCheckedChangeListener(null);
            mSwitch3DMode.setChecked(mService.is3DModeEnabled());
            mSwitch3DMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    toggleDisplayMode(isChecked);
                }
            });
            
            // Update status text based on glasses connection
            if (mService.areGlassesConnected()) {
                updateStatus("XR glasses connected");
            } else {
                updateStatus("No XR glasses detected");
            }
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
        if (mBound && mService != null) {
            boolean success = mService.toggleDisplayMode(enable3D);
            if (success) {
                updateStatus("Display mode set to: " + (enable3D ? "3D" : "2D"));
            } else {
                // Error occurred, revert switch state
                mSwitch3DMode.setChecked(!enable3D);
                updateStatus("Failed to change display mode");
                Toast.makeText(this, "Failed to change display mode", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Service not bound, revert switch state
            mSwitch3DMode.setChecked(!enable3D);
            updateStatus("Service not bound, cannot change mode");
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to GlassesDisplayService
        Intent intent = new Intent(this, GlassesDisplayService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Update UI if already bound to service
        if (mBound && mService != null) {
            updateUIFromService();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
