# VITURE XR Glasses SDK - Device Management & Lifecycle

This guide explains how to properly manage the VITURE XR Glasses device throughout your application's lifecycle, including initialization, permission handling, and resource cleanup.

## Table of Contents

1. [Introduction](#introduction)
2. [USB Initialization](#usb-initialization)
3. [Permission Handling](#permission-handling)
4. [Device Connection States](#device-connection-states)
5. [Application Lifecycle Management](#application-lifecycle-management)
6. [Resource Cleanup](#resource-cleanup)
7. [Error Handling](#error-handling)
8. [Example Implementation](#example-implementation)
9. [Troubleshooting](#troubleshooting)

## Introduction

Proper device management is crucial for applications that interface with the VITURE XR Glasses. This includes correctly initializing the USB connection, handling permissions, managing the device throughout the application lifecycle, and releasing resources when they are no longer needed.

Following these best practices ensures reliable operation of the glasses and prevents issues like resource leaks, connection failures, and unexpected behavior.

## USB Initialization

The first step in working with the VITURE XR Glasses is initializing the USB connection. This is done through the `ArManager` class, which provides a singleton instance for interacting with the glasses.

### Getting the ArManager Instance

```java
// Get the ArManager instance
ArManager arManager = ArManager.getInstance(context);
```

The `getInstance` method takes a Context parameter, which should typically be your Activity or Application context.

### Initializing the USB Connection

After obtaining the ArManager instance, you need to initialize the USB connection:

```java
// Initialize the USB connection
int initResult = arManager.init();

// Check if initialization was successful
if (initResult == Constants.ERROR_INIT_SUCCESS) {
    Log.d(TAG, "USB initialization successful");
    // Proceed with SDK usage
} else {
    Log.e(TAG, "USB initialization failed with code: " + initResult);
    // Handle initialization failure
}
```

### Initialization Return Codes

The `init()` method returns one of the following codes:

- `Constants.ERROR_INIT_SUCCESS` (0): Initialization successful
- `Constants.ERROR_INIT_NO_DEVICE` (-1): No VITURE glasses detected
- `Constants.ERROR_INIT_NO_PERMISSION` (-2): USB permission not granted
- `Constants.ERROR_INIT_UNKOWN` (-3): Unknown initialization error

### Asynchronous Initialization Events

In addition to the immediate return value, the initialization status may also be reported asynchronously through the `onEvent` callback:

```java
ArCallback arCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        if (eventId == Constants.EVENT_ID_INIT) {
            int initStatus = byteArrayToInt(event, 0, event.length);
            Log.d(TAG, "Initialization event received: " + initStatus);
            
            // Update UI or app state based on initialization status
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateInitializationStatus(initStatus);
                }
            });
        }
    }
    
    @Override
    public void onImu(long timestamp, byte[] imuData) {
        // Handle IMU data
    }
};

// Register the callback
arManager.registerCallback(arCallback);
```

## Permission Handling

The VITURE XR Glasses requires USB permissions to communicate with your application. There are several steps to handle these permissions properly.

### 1. Update AndroidManifest.xml

First, declare the USB host feature in your AndroidManifest.xml:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="your.package.name">

    <!-- USB host feature declaration -->
    <uses-feature android:name="android.hardware.usb.host" />
    
    <application
        ...>
        
        <!-- Activities and other components -->
        
    </application>
</manifest>
```

### 2. Create USB Device Filter

Create a USB device filter to recognize VITURE XR Glasses when connected:

Create a file in `res/xml/device_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- VITURE XR Glasses filter -->
    <usb-device vendor-id="9477" product-id="48903" />
    <usb-device vendor-id="3368" product-id="48903" />
    <usb-device vendor-id="1017" product-id="38307" />
    <usb-device vendor-id="3873" product-id="32775" />
</resources>
```

### 3. Handle Permission Requests

The ArManager's `init()` method will automatically request USB permissions when needed. It's important to be aware that the permission dialog is system-managed and will appear over your application when required.

### 4. Check for Permission Denial

When initializing the SDK, check if the initialization failed due to permission denial:

```java
int initResult = arManager.init();
if (initResult == Constants.ERROR_INIT_NO_PERMISSION) {
    // Permission was denied
    Toast.makeText(this, "USB permission is required to use VITURE glasses", 
                   Toast.LENGTH_LONG).show();
    // Optionally guide the user to reconnect the device and try again
}
```

## Device Connection States

To ensure your application responds appropriately to device connection and disconnection, you should monitor the device state.

### Checking if Device is Connected

You can check if a device is connected by attempting to initialize the SDK:

```java
int status = arManager.init();
boolean isDeviceConnected = (status == Constants.ERROR_INIT_SUCCESS);
```

You can also monitor device connections and disconnections through the ArCallback:

```java
@Override
public void onEvent(int eventId, byte[] event, long timestamp) {
    if (eventId == Constants.EVENT_ID_INIT) {
        int initStatus = byteArrayToInt(event, 0, event.length);
        boolean isConnected = (initStatus == Constants.ERROR_INIT_SUCCESS);
        
        // Update UI or app functionality based on connection state
        updateConnectionStatus(isConnected);
    }
}
```

### Handling Device Disconnection

If the VITURE XR Glasses are disconnected while your application is running, you should clean up resources and update your UI accordingly:

```java
private void handleDeviceDisconnection() {
    // Disable UI elements that require the glasses
    enableGlassesFeatures(false);
    
    // Show reconnection guidance
    showReconnectionDialog();
    
    // Reset state variables
    isCalibrated = false;
    isGlassesConnected = false;
}
```

## Application Lifecycle Management

Proper lifecycle management ensures that the VITURE XR Glasses work correctly as your application moves through different states (created, resumed, paused, destroyed).

### Activity/Fragment Lifecycle Integration

Here's how to integrate the SDK with your Activity lifecycle:

```java
public class VitureActivity extends AppCompatActivity {
    private ArManager arManager;
    private ArCallback arCallback;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viture);
        
        // Get ArManager instance
        arManager = ArManager.getInstance(this);
        
        // Create callback
        arCallback = new ArCallback() {
            // Implement callback methods
        };
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Initialize the SDK when activity resumes
        int initResult = arManager.init();
        
        // Register callback to receive events
        arManager.registerCallback(arCallback);
        
        // Restore previous state (e.g., IMU enabled/disabled)
        restorePreviousState();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister callback to prevent memory leaks
        arManager.unregisterCallback(arCallback);
        
        // Save current state
        saveCurrentState();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Release resources
        if (arManager != null) {
            arManager.release();
            arManager = null;
        }
    }
    
    private void saveCurrentState() {
        if (arManager != null) {
            // Example: save IMU state
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            prefs.edit()
                .putBoolean("imu_enabled", arManager.getImuState() == Constants.STATE_ON)
                .apply();
        }
    }
    
    private void restorePreviousState() {
        if (arManager != null) {
            // Example: restore IMU state
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            boolean imuEnabled = prefs.getBoolean("imu_enabled", false);
            arManager.setImuOn(imuEnabled);
        }
    }
}
```

### Multiple Activity Support

If your application has multiple activities that use the VITURE XR Glasses, consider these approaches:

#### Option 1: Initialize in Each Activity

Initialize the SDK in each activity, but be careful to release resources properly:

```java
// In each activity that uses VITURE glasses
@Override
protected void onResume() {
    super.onResume();
    arManager = ArManager.getInstance(this);
    arManager.init();
    arManager.registerCallback(arCallback);
}

@Override
protected void onPause() {
    super.onPause();
    if (arManager != null) {
        arManager.unregisterCallback(arCallback);
    }
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (arManager != null && isFinishing()) {
        arManager.release();
        arManager = null;
    }
}
```

#### Option 2: Application-Level Management

Manage the SDK at the application level for better resource management:

```java
// In your Application class
public class MyApplication extends Application {
    private ArManager arManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        arManager = ArManager.getInstance(this);
    }
    
    public ArManager getArManager() {
        return arManager;
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        if (arManager != null) {
            arManager.release();
            arManager = null;
        }
    }
}

// In your activities
@Override
protected void onResume() {
    super.onResume();
    ArManager arManager = ((MyApplication) getApplication()).getArManager();
    arManager.init();
    arManager.registerCallback(arCallback);
}
```

## Resource Cleanup

Proper resource cleanup is essential to avoid memory leaks and ensure the VITURE XR Glasses work correctly with your application.

### Unregistering Callbacks

Always unregister callbacks when they are no longer needed:

```java
@Override
protected void onPause() {
    super.onPause();
    if (arManager != null && arCallback != null) {
        arManager.unregisterCallback(arCallback);
    }
}
```

### Releasing Resources

When your application is finishing or no longer needs the VITURE XR Glasses, release the SDK resources:

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (arManager != null) {
        arManager.release();
        arManager = null;
    }
}
```

The `release()` method performs the following cleanup:
- Closes the USB connection
- Frees native resources
- Resets internal state

### Resource Cleanup Best Practices

1. **Always call `release()` when done**: Call this method when your application is finishing or when you're certain you no longer need the glasses.

2. **Null out references after release**: After calling `release()`, set your `arManager` reference to null to prevent accidental usage.

3. **Unregister callbacks before release**: Always unregister callbacks before calling `release()` to prevent callback invocation during cleanup.

4. **Disable IMU when not visible**: Turn off IMU data streaming when your activity is not visible to save resources:

```java
@Override
protected void onPause() {
    super.onPause();
    if (arManager != null) {
        arManager.setImuOn(false);
        arManager.unregisterCallback(arCallback);
    }
}
```

## Error Handling

Proper error handling ensures your application can respond gracefully to various issues that may occur when working with the VITURE XR Glasses.

### Common Error Scenarios

1. **Initialization Errors**

```java
int initResult = arManager.init();
switch (initResult) {
    case Constants.ERROR_INIT_SUCCESS:
        // Initialization successful
        enableUI(true);
        break;
    case Constants.ERROR_INIT_NO_DEVICE:
        // No device connected
        showError("Please connect your VITURE glasses");
        enableUI(false);
        break;
    case Constants.ERROR_INIT_NO_PERMISSION:
        // Permission denied
        showError("USB permission is required. Please reconnect the glasses and try again.");
        enableUI(false);
        break;
    default:
        // Unknown error
        showError("An unknown error occurred: " + initResult);
        enableUI(false);
        break;
}
```

2. **Command Execution Errors**

```java
int result = arManager.set3D(true);
if (result != Constants.ERR_SET_SUCCESS) {
    // Command failed
    String errorMessage = getErrorMessage(result);
    showError("Failed to set 3D mode: " + errorMessage);
}
```

3. **State Query Errors**

```java
int state = arManager.getImuState();
if (state < 0) {
    // Error querying state
    String errorMessage = getStateErrorMessage(state);
    showError("Failed to get IMU state: " + errorMessage);
}
```

### Error Message Helper

```java
private String getErrorMessage(int errorCode) {
    switch (errorCode) {
        case Constants.ERR_SET_SUCCESS:
            return "Success";
        case Constants.ERR_SET_FAILURE:
            return "General failure";
        case Constants.ERR_SET_INVALID_ARGUMENT:
            return "Invalid argument";
        case Constants.ERR_SET_NOT_ENOUGH_MEMORY:
            return "Not enough memory";
        case Constants.ERR_SET_UNSUPPORTED_CMD:
            return "Unsupported command";
        case Constants.ERR_SET_CRC_MISMATCH:
            return "CRC mismatch";
        case Constants.ERR_SET_VER_MISMATCH:
            return "Version mismatch";
        case Constants.ERR_SET_MSG_ID_MISMATCH:
            return "Message ID mismatch";
        case Constants.ERR_SET_MSG_STX_MISMATCH:
            return "Message STX mismatch";
        case Constants.ERR_SET_CODE_NOT_WRITTEN:
            return "Code not written";
        default:
            return "Unknown error: " + errorCode;
    }
}

private String getStateErrorMessage(int errorCode) {
    switch (errorCode) {
        case Constants.STATE_ERR_WRITE_FAIL:
            return "Failed to write command";
        case Constants.STATE_ERR_RSP_ERROR:
            return "Received error response";
        case Constants.STATE_ERR_TIMEOUT:
            return "Command timed out";
        default:
            return "Unknown error: " + errorCode;
    }
}
```

### Handling Connection Loss

```java
private void checkConnection() {
    // Attempt to get IMU state as a connection check
    int state = arManager.getImuState();
    
    if (state == Constants.STATE_ERR_TIMEOUT) {
        // Connection likely lost
        handleConnectionLoss();
    }
}

private void handleConnectionLoss() {
    // Update UI
    showConnectionLostMessage();
    
    // Attempt to reinitialize
    int result = arManager.init();
    if (result == Constants.ERROR_INIT_SUCCESS) {
        // Reconnected
        showReconnectedMessage();
        restoreSettings();
    } else {
        // Still disconnected
        disableGlassesFunctionality();
    }
}
```

## Example Implementation

Here's a complete example of a base activity that properly manages the VITURE XR Glasses lifecycle:

```java
public class VitureBaseActivity extends AppCompatActivity {
    private static final String TAG = "VitureBaseActivity";
    
    protected ArManager arManager;
    protected ArCallback arCallback;
    protected boolean isGlassesConnected = false;
    protected boolean isImuEnabled = false;
    protected boolean is3DModeEnabled = false;
    
    // UI elements
    protected TextView statusTextView;
    protected Switch imuSwitch;
    protected Switch mode3DSwitch;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        
        // Initialize UI
        initializeUI();
        
        // Create callback
        createArCallback();
        
        // Get ArManager instance
        arManager = ArManager.getInstance(this);
        
        // Enable logging if needed
        arManager.setLogOn(true);
        
        // Set up UI controls
        setupUIControls();
    }
    
    // Override in subclasses to provide layout resource ID
    protected int getLayoutResourceId() {
        return R.layout.activity_viture_base;
    }
    
    protected void initializeUI() {
        statusTextView = findViewById(R.id.status_text);
        imuSwitch = findViewById(R.id.switch_imu);
        mode3DSwitch = findViewById(R.id.switch_3d);
    }
    
    protected void createArCallback() {
        arCallback = new ArCallback() {
            @Override
            public void onEvent(int eventId, byte[] event, long timestamp) {
                Log.d(TAG, "Event received: " + eventId);
                
                if (eventId == Constants.EVENT_ID_INIT) {
                    final int initStatus = byteArrayToInt(event, 0, event.length);
                    isGlassesConnected = (initStatus == Constants.ERROR_INIT_SUCCESS);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleInitializationStatus(initStatus);
                        }
                    });
                }
            }
            
            @Override
            public void onImu(long timestamp, byte[] imuData) {
                // Handle IMU data in subclasses
                handleImuData(timestamp, imuData);
            }
        };
    }
    
    protected void setupUIControls() {
        if (imuSwitch != null) {
            imuSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isGlassesConnected) {
                        int result = arManager.setImuOn(isChecked);
                        if (result == Constants.ERR_SET_SUCCESS) {
                            isImuEnabled = isChecked;
                            updateStatusText(isChecked ? "IMU enabled" : "IMU disabled");
                        } else {
                            // Revert switch without triggering listener
                            buttonView.setOnCheckedChangeListener(null);
                            buttonView.setChecked(!isChecked);
                            buttonView.setOnCheckedChangeListener(this);
                            
                            updateStatusText("Failed to change IMU state: " +
                                    getErrorMessage(result));
                        }
                    }
                }
            });
        }
        
        if (mode3DSwitch != null) {
            mode3DSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isGlassesConnected) {
                        int result = arManager.set3D(isChecked);
                        if (result == Constants.ERR_SET_SUCCESS) {
                            is3DModeEnabled = isChecked;
                            updateStatusText(isChecked ? "3D mode enabled" : "3D mode disabled");
                        } else {
                            // Revert switch without triggering listener
                            buttonView.setOnCheckedChangeListener(null);
                            buttonView.setChecked(!isChecked);
                            buttonView.setOnCheckedChangeListener(this);
                            
                            updateStatusText("Failed to change 3D mode: " +
                                    getErrorMessage(result));
                        }
                    }
                }
            });
        }
    }
    
    protected void handleInitializationStatus(int status) {
        switch (status) {
            case Constants.ERROR_INIT_SUCCESS:
                updateStatusText("Glasses connected successfully");
                enableUI(true);
                updateUIState();
                break;
            case Constants.ERROR_INIT_NO_DEVICE:
                updateStatusText("No glasses detected. Please connect your VITURE glasses.");
                enableUI(false);
                break;
            case Constants.ERROR_INIT_NO_PERMISSION:
                updateStatusText("USB permission denied. Please reconnect and grant permission.");
                enableUI(false);
                break;
            default:
                updateStatusText("Initialization failed with error: " + status);
                enableUI(false);
                break;
        }
    }
    
    protected void enableUI(boolean enable) {
        if (imuSwitch != null) {
            imuSwitch.setEnabled(enable);
        }
        
        if (mode3DSwitch != null) {
            mode3DSwitch.setEnabled(enable);
        }
    }
    
    protected void updateUIState() {
        if (isGlassesConnected) {
            // Get current states
            int imuState = arManager.getImuState();
            int mode3DState = arManager.get3DState();
            
            if (imuState >= 0 && imuSwitch != null) {
                isImuEnabled = (imuState == Constants.STATE_ON);
                imuSwitch.setOnCheckedChangeListener(null);
                imuSwitch.setChecked(isImuEnabled);
                imuSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) imuSwitch.getTag());
            }
            
            if (mode3DState >= 0 && mode3DSwitch != null) {
                is3DModeEnabled = (mode3DState == Constants.STATE_ON);
                mode3DSwitch.setOnCheckedChangeListener(null);
                mode3DSwitch.setChecked(is3DModeEnabled);
                mode3DSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) mode3DSwitch.getTag());
            }
        }
    }
    
    protected void updateStatusText(String message) {
        if (statusTextView != null) {
            statusTextView.setText(message);
        }
        Log.d(TAG, message);
    }
    
    // To be overridden by subclasses
    protected void handleImuData(long timestamp, byte[] imuData) {
        // Default implementation does nothing
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Initialize the SDK
        int initResult = arManager.init();
        isGlassesConnected = (initResult == Constants.ERROR_INIT_SUCCESS);
        
        // Register callback
        arManager.registerCallback(arCallback);
        
        // Update UI based on initialization result
        handleInitializationStatus(initResult);
        
        // Restore state
        restoreState();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Save state
        saveState();
        
        // Unregister callback
        if (arManager != null && arCallback != null) {
            arManager.unregisterCallback(arCallback);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Release resources if activity is finishing
        if (isFinishing() && arManager != null) {
            arManager.release();
            arManager = null;
        }
    }
    
    protected void saveState() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        prefs.edit()
            .putBoolean("imu_enabled", isImuEnabled)
            .putBoolean("3d_mode_enabled", is3DModeEnabled)
            .apply();
    }
    
    protected void restoreState() {
        if (isGlassesConnected) {
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            boolean savedImuState = prefs.getBoolean("imu_enabled", false);
            boolean saved3DState = prefs.getBoolean("3d_mode_enabled", false);
            
            // Apply saved states
            if (savedImuState != isImuEnabled) {
                arManager.setImuOn(savedImuState);
                isImuEnabled = savedImuState;
            }
            
            if (saved3DState != is3DModeEnabled) {
                arManager.set3D(saved3DState);
                is3DModeEnabled = saved3DState;
            }
            
            // Update UI
            updateUIState();
        }
    }
    
    protected int byteArrayToInt(byte[] bytes, int offset, int length) {
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
    
    protected String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case Constants.ERR_SET_SUCCESS:
                return "Success";
            case Constants.ERR_SET_FAILURE:
                return "General failure";
            case Constants.ERR_SET_INVALID_ARGUMENT:
                return "Invalid argument";
            case Constants.ERR_SET_NOT_ENOUGH_MEMORY:
                return "Not enough memory";
            case Constants.ERR_SET_UNSUPPORTED_CMD:
                return "Unsupported command";
            case Constants.ERR_SET_CRC_MISMATCH:
                return "CRC mismatch";
            case Constants.ERR_SET_VER_MISMATCH:
                return "Version mismatch";
            case Constants.ERR_SET_MSG_ID_MISMATCH:
                return "Message ID mismatch";
            case Constants.ERR_SET_MSG_STX_MISMATCH:
                return "Message STX mismatch";
            case Constants.ERR_SET_CODE_NOT_WRITTEN:
                return "Code not written";
            default:
                return "Unknown error: " + errorCode;
        }
    }
}
```

### Sample Layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/status_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:text="Initializing..."
        android:textSize="16sp"
        android:textStyle="bold" />

    <Switch
        android:id="@+id/switch_imu"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:text="IMU Data" />

    <Switch
        android:id="@+id/switch_3d"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="3D Mode" />

</LinearLayout>
```

## Troubleshooting

### Common Issues and Solutions

#### 1. USB Connection Issues

**Symptoms:**
- `init()` returns `ERROR_INIT_NO_DEVICE`
- Device not detected even when physically connected

**Solutions:**
- Check physical connection (try different cable)
- Check USB port on phone/device
- Verify the glasses are powered on
- Try disconnecting and reconnecting the glasses
- Restart the Android device
- Check device_filter.xml includes the correct vendor/product IDs

#### 2. Permission Errors

**Symptoms:**
- `init()` returns `ERROR_INIT_NO_PERMISSION`
- USB permission dialog doesn't appear

**Solutions:**
- Disconnect and reconnect the USB device to trigger permission request again
- Check manifest for proper USB host feature declaration
- Verify you're using a physical device (emulators don't support USB)
- Try using a different USB port
- Check if another app has an exclusive connection to the device

#### 3. Resource Leaks

**Symptoms:**
- Application crashes after repeated connect/disconnect cycles
- Memory usage increases over time
- IMU data continues after activity is paused

**Solutions:**
- Always unregister callbacks in onPause
- Always call release() in onDestroy when activity is finishing
- Disable IMU data streaming in onPause
- Set ArManager reference to null after release

#### 4. Concurrent Access Issues

**Symptoms:**
- Commands fail unexpectedly when multiple activities use the glasses
- Inconsistent state between different parts of your application

**Solutions:**
- Use an application-level ArManager instance
- Implement proper synchronization between components
- Use a service or manager class to centralize VITURE glasses access

### Debugging Tips

1. **Enable SDK Logging**

```java
// Enable detailed SDK logs
arManager.setLogOn(true);
```

2. **Add Logcat Tags**

```java
// Add consistent log tags
private static final String TAG = "VITURE_SDK";

// Log important events
Log.d(TAG, "SDK initialization result: " + initResult);
```

3. **Create a Debug Mode**

```java
private boolean isDebugMode = BuildConfig.DEBUG;

private void debugLog(String message) {
    if (isDebugMode) {
        Log.d(TAG, message);
    }
}
```

4. **Test with Simple Commands**

When encountering issues, test basic functionality first:

```java
// Test basic command execution
int imuState = arManager.getImuState();
debugLog("IMU state check result: " + imuState);

// Test basic setting
int setResult = arManager.setImuOn(true);
debugLog("Set IMU result: " + setResult);
```

5. **Monitor USB Connection Events**

If you suspect USB connection issues, you can add extra monitoring:

```java
// Check if the USB connection is still valid periodically
Handler handler = new Handler(Looper.getMainLooper());
Runnable connectionChecker = new Runnable() {
    @Override
    public void run() {
        if (arManager != null) {
            int state = arManager.getImuState();
            if (state == Constants.STATE_ERR_TIMEOUT) {
                Log.w(TAG, "USB connection appears to be broken");
                handleConnectionLoss();
            }
            handler.postDelayed(this, 5000); // Check every 5 seconds
        }
    }
};

// Start checking in onResume
handler.post(connectionChecker);

// Stop checking in onPause
handler.removeCallbacks(connectionChecker);
```

## Conclusion

Proper device management is critical for applications using the VITURE XR Glasses. By following the guidelines in this document, you can ensure your application:

1. Initializes the USB connection correctly
2. Handles permissions appropriately
3. Manages the device throughout the application lifecycle
4. Cleans up resources properly
5. Handles errors gracefully

These practices will help you create a robust application that delivers a reliable and seamless experience to users of the VITURE XR Glasses.

For more information on specific features, refer to the other documentation guides in this series.
