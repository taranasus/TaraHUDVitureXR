# VITURE XR Glasses SDK - Display Mode Control

This guide explains how to control the display modes of the VITURE XR Glasses, including switching between 2D and 3D modes and managing resolution settings.

## Table of Contents

1. [Introduction to Display Modes](#introduction-to-display-modes)
2. [Checking the Current Display Mode](#checking-the-current-display-mode)
3. [Switching Between 2D and 3D Modes](#switching-between-2d-and-3d-modes)
4. [Resolution Control](#resolution-control)
5. [Event Handling for Display Mode Changes](#event-handling-for-display-mode-changes)
6. [Best Practices](#best-practices)
7. [Example Implementation](#example-implementation)
8. [Troubleshooting](#troubleshooting)

## Introduction to Display Modes

The VITURE XR Glasses support different display modes to provide both 2D and 3D visual experiences. In the context of the SDK:

- **2D Mode**: Standard display mode with resolution 1920×1080
- **3D Mode**: Stereoscopic display mode with resolution 3840×1080 (effectively 1920×1080 per eye)

The ability to switch between these modes allows your application to provide the appropriate viewing experience based on content type or user preference.

## Checking the Current Display Mode

Before making changes to the display mode, you may want to check the current state:

```java
// Get the current 3D state
int mode3DState = arManager.get3DState();

if (mode3DState == Constants.STATE_ON) {
    // Currently in 3D mode (3840×1080)
    Log.d(TAG, "Display is in 3D mode");
} else if (mode3DState == Constants.STATE_OFF) {
    // Currently in 2D mode (1920×1080)
    Log.d(TAG, "Display is in 2D mode");
} else {
    // Error occurred
    Log.e(TAG, "Failed to get display mode: " + mode3DState);
}
```

Possible return values:
- `Constants.STATE_ON` (1): 3D mode is active
- `Constants.STATE_OFF` (0): 2D mode is active
- Negative values: Error occurred (see [Error Handling](#error-handling))

## Switching Between 2D and 3D Modes

To change the display mode, use the `set3D` method:

```java
// Switch to 3D mode
int result = arManager.set3D(true);

// or switch to 2D mode
int result = arManager.set3D(false);

// Check if the operation was successful
if (result == Constants.ERR_SET_SUCCESS) {
    Log.d(TAG, "Display mode changed successfully");
} else {
    Log.e(TAG, "Failed to change display mode: " + result);
}
```

### Parameters

- `boolean on`: 
  - `true` - Switch to 3D mode (3840×1080)
  - `false` - Switch to 2D mode (1920×1080)

### Return Values

The method returns an error code indicating success or failure:
- `Constants.ERR_SET_SUCCESS` (0): Operation successful
- Other values: Error occurred (see [Error Handling](#error-handling))

## Resolution Control

The resolution of the VITURE XR Glasses is tied to the display mode:

- 2D Mode: 1920×1080 (Full HD)
- 3D Mode: 3840×1080 (effectively 1920×1080 per eye)

When you switch between 2D and 3D modes using the `set3D` method, the resolution changes automatically.

### Considerations for Content Display

When developing content for different modes:

1. **2D Mode**:
   - Content is displayed at 1920×1080 resolution
   - Suitable for standard video, UI elements, and non-stereoscopic content

2. **3D Mode**:
   - Content is displayed at 3840×1080 resolution (1920×1080 per eye)
   - For side-by-side stereoscopic content
   - Each eye receives half of the horizontal resolution

## Event Handling for Display Mode Changes

You can listen for display mode changes using the ArCallback's `onEvent` method:

```java
ArCallback arCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        if (eventId == Constants.EVENT_ID_3D) {
            // Parse the event data
            int state = byteArrayToInt(event, 0, event.length);
            
            // Update UI or app state based on the new display mode
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateDisplayModeUI(state == Constants.STATE_ON);
                }
            });
        }
    }
    
    @Override
    public void onImu(long timestamp, byte[] imuData) {
        // IMU data handling
    }
};
```

### Event Data Format

When you receive a `Constants.EVENT_ID_3D` event, the event data is a byte array that can be converted to an integer representing the current state:
- If the converted integer is `Constants.STATE_ON` (1), the glasses are in 3D mode
- If the converted integer is `Constants.STATE_OFF` (0), the glasses are in 2D mode

### Helper Method for Byte Array Conversion

```java
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
```

## Best Practices

### 1. Match Content to Display Mode

- Use 2D mode for standard UI, text, and non-stereoscopic content
- Use 3D mode only for content designed for stereoscopic viewing

### 2. Provide User Control

- Allow users to toggle between 2D and 3D modes based on their preferences
- Remember user preferences for future app sessions

### 3. Save and Restore State

- Remember the display mode when your app is paused or resumed
- Update the glasses' mode to match your app's state when resumed

```java
@Override
protected void onPause() {
    super.onPause();
    // Save current display mode
    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    prefs.edit().putBoolean("display_mode_3d", is3DModeActive).apply();
}

@Override
protected void onResume() {
    super.onResume();
    // Restore display mode
    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    boolean saved3DMode = prefs.getBoolean("display_mode_3d", false);
    
    // Apply saved mode
    if (arManager != null) {
        arManager.set3D(saved3DMode);
    }
}
```

### 4. Smooth Transitions

- Consider adding visual transitions when switching modes
- Inform users when the display mode is changing

## Example Implementation

Here's a complete example of a simple activity that allows toggling between 2D and 3D modes:

```java
public class DisplayModeActivity extends AppCompatActivity {
    private static final String TAG = "DisplayMode";
    
    private ArManager arManager;
    private ArCallback arCallback;
    private Switch mode3DSwitch;
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_mode);
        
        // Initialize UI
        statusText = findViewById(R.id.status_text);
        mode3DSwitch = findViewById(R.id.switch_3d);
        
        // Initialize SDK
        initVitureSDK();
        
        // Set up the display mode switch
        mode3DSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (arManager != null) {
                    int result = arManager.set3D(isChecked);
                    
                    if (result == Constants.ERR_SET_SUCCESS) {
                        updateStatusText(isChecked ? "Switched to 3D mode" : "Switched to 2D mode");
                    } else {
                        updateStatusText("Failed to change mode: " + result);
                        // Revert switch without triggering the listener
                        buttonView.setOnCheckedChangeListener(null);
                        buttonView.setChecked(!isChecked);
                        buttonView.setOnCheckedChangeListener(this);
                    }
                }
            }
        });
    }
    
    private void initVitureSDK() {
        arManager = ArManager.getInstance(this);
        
        arCallback = new ArCallback() {
            @Override
            public void onEvent(int eventId, byte[] event, long timestamp) {
                Log.d(TAG, "Event received: " + eventId);
                
                if (eventId == Constants.EVENT_ID_INIT) {
                    // SDK initialization event
                    int initResult = byteArrayToInt(event, 0, event.length);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (initResult == Constants.ERROR_INIT_SUCCESS) {
                                updateStatusText("SDK initialized successfully");
                                updateDisplayModeUI();
                            } else {
                                updateStatusText("SDK initialization failed: " + initResult);
                                mode3DSwitch.setEnabled(false);
                            }
                        }
                    });
                } else if (eventId == Constants.EVENT_ID_3D) {
                    // Display mode changed event
                    int displayMode = byteArrayToInt(event, 0, event.length);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update switch without triggering the listener
                            mode3DSwitch.setOnCheckedChangeListener(null);
                            mode3DSwitch.setChecked(displayMode == Constants.STATE_ON);
                            mode3DSwitch.setOnCheckedChangeListener(mode3DSwitch.getOnCheckedChangeListener());
                            
                            updateStatusText(displayMode == Constants.STATE_ON ? 
                                    "Display mode: 3D" : "Display mode: 2D");
                        }
                    });
                }
            }
            
            @Override
            public void onImu(long timestamp, byte[] imuData) {
                // IMU handling not needed for this example
            }
        };
        
        // Register callback
        arManager.registerCallback(arCallback);
        
        // Initialize SDK
        int initResult = arManager.init();
        if (initResult == Constants.ERROR_INIT_SUCCESS) {
            updateStatusText("SDK initialized successfully");
            updateDisplayModeUI();
        } else {
            updateStatusText("SDK initialization failed: " + initResult);
            mode3DSwitch.setEnabled(false);
        }
    }
    
    private void updateDisplayModeUI() {
        // Get current display mode
        int currentMode = arManager.get3DState();
        
        if (currentMode >= 0) {
            // Update switch without triggering the listener
            mode3DSwitch.setOnCheckedChangeListener(null);
            mode3DSwitch.setChecked(currentMode == Constants.STATE_ON);
            mode3DSwitch.setOnCheckedChangeListener(mode3DSwitch.getOnCheckedChangeListener());
            
            updateStatusText(currentMode == Constants.STATE_ON ? 
                    "Display mode: 3D" : "Display mode: 2D");
        } else {
            updateStatusText("Failed to get display mode: " + currentMode);
            mode3DSwitch.setEnabled(false);
        }
    }
    
    private void updateStatusText(String message) {
        if (statusText != null) {
            statusText.setText(message);
            Log.d(TAG, message);
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
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (arManager != null && arCallback != null) {
            arManager.registerCallback(arCallback);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        if (arManager != null && arCallback != null) {
            arManager.unregisterCallback(arCallback);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (arManager != null) {
            arManager.release();
        }
    }
}
```

### Layout XML

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    tools:context=".DisplayModeActivity">

    <TextView
        android:id="@+id/status_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:text="Initializing..."
        android:textSize="16sp"
        android:textStyle="bold" />

    <Switch
        android:id="@+id/switch_3d"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="3D Mode"
        android:layout_marginBottom="16dp" />
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Display Mode Information:"
        android:textStyle="bold" />
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="2D Mode: 1920×1080 resolution\n3D Mode: 3840×1080 resolution (1920×1080 per eye)" />
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:text="Usage Instructions:"
        android:textStyle="bold" />
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="1. Connect your VITURE glasses to your Android device\n2. Toggle the switch to change between 2D and 3D modes\n3. Use 2D mode for standard content and 3D mode for stereoscopic content" />

</LinearLayout>
```

## Troubleshooting

### Error Handling

When using the `get3DState` and `set3D` methods, you may encounter error codes:

#### get3DState Error Codes

- `Constants.STATE_ERR_WRITE_FAIL` (-1): Failed to write command to the glasses
- `Constants.STATE_ERR_RSP_ERROR` (-2): Received an error response from the glasses
- `Constants.STATE_ERR_TIMEOUT` (-3): Command timed out without response

#### set3D Error Codes

- `Constants.ERR_SET_SUCCESS` (0): Operation successful
- `Constants.ERR_SET_FAILURE` (1): General failure
- `Constants.ERR_SET_INVALID_ARGUMENT` (2): Invalid argument provided
- `Constants.ERR_SET_NOT_ENOUGH_MEMORY` (3): Not enough memory
- `Constants.ERR_SET_UNSUPPORTED_CMD` (4): Command not supported
- `Constants.ERR_SET_CRC_MISMATCH` (5): CRC mismatch in communication
- `Constants.ERR_SET_VER_MISMATCH` (6): Version mismatch
- `Constants.ERR_SET_MSG_ID_MISMATCH` (7): Message ID mismatch
- `Constants.ERR_SET_MSG_STX_MISMATCH` (8): Message STX mismatch
- `Constants.ERR_SET_CODE_NOT_WRITTEN` (9): Code not written

### Common Issues

#### 1. Display Mode Doesn't Change

**Symptoms:**
- The `set3D` method returns success, but the display doesn't change
- No visual difference when toggling between modes

**Solutions:**
- Ensure your content is properly formatted for the selected mode
- Check if the glasses firmware is up to date
- Verify the glasses are properly connected and recognized
- Try disconnecting and reconnecting the glasses

#### 2. Content Appears Stretched or Distorted

**Symptoms:**
- Content looks stretched or distorted in 3D mode
- Images don't align properly in stereoscopic view

**Solutions:**
- Ensure your content is properly formatted for stereoscopic 3D (side-by-side)
- Verify your content resolution matches the expected resolution
- Check if your rendering code properly accounts for the appropriate display mode

#### 3. Inconsistent Mode Behavior

**Symptoms:**
- Display mode changes unexpectedly
- Mode settings don't persist

**Solutions:**
- Register for display mode change events and handle them appropriately
- Verify another app isn't controlling the glasses simultaneously
- Check if your app properly restores state after resuming

### Debugging Tips

1. Enable SDK logging to see detailed information:
   ```java
   arManager.setLogOn(true);
   ```

2. Add extensive logging around display mode changes to track the flow:
   ```java
   Log.d(TAG, "Attempting to change display mode to: " + (enable3D ? "3D" : "2D"));
   int result = arManager.set3D(enable3D);
   Log.d(TAG, "Display mode change result: " + result);
   ```

3. If the SDK indicates success but you see no change, check the current mode first:
   ```java
   int currentMode = arManager.get3DState();
   Log.d(TAG, "Current display mode before change: " + 
         (currentMode == Constants.STATE_ON ? "3D" : "2D"));
   ```

4. Test with simple content first to isolate content-related issues from SDK issues

## Conclusion

The display mode control features of the VITURE XR Glasses SDK allow you to create applications that can seamlessly switch between 2D and 3D viewing experiences. By properly implementing mode switching and following best practices, you can provide users with an optimal viewing experience for different types of content.

For more advanced usage, consider combining display mode changes with other features like IMU data to create immersive interactive experiences that take full advantage of the VITURE XR Glasses capabilities.
