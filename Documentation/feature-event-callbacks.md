# VITURE XR Glasses SDK - Event Callbacks

This guide explains how to work with the event callback system in the VITURE XR Glasses SDK, which is essential for receiving and handling various events and data from the glasses.

## Table of Contents

1. [Introduction to Event Callbacks](#introduction-to-event-callbacks)
2. [The ArCallback Interface](#the-arcallback-interface)
3. [Registering and Unregistering Callbacks](#registering-and-unregistering-callbacks)
4. [Event Types](#event-types)
5. [IMU Data Callback](#imu-data-callback)
6. [Processing Events on the UI Thread](#processing-events-on-the-ui-thread)
7. [Working with Multiple Callbacks](#working-with-multiple-callbacks)
8. [Example Implementation](#example-implementation)
9. [Troubleshooting](#troubleshooting)

## Introduction to Event Callbacks

The VITURE XR Glasses SDK uses a callback mechanism to communicate events and data from the glasses to your application. This asynchronous approach allows your application to respond to changes without continuously polling the device for updates.

Key events communicated through callbacks include:
- Initialization status
- Display mode changes (2D/3D)
- IMU data updates (head tracking)
- Other device state changes

By properly implementing callbacks, your application can maintain real-time awareness of the glasses' state and react appropriately to user actions and device changes.

## The ArCallback Interface

The core of the callback system is the `ArCallback` interface, which your application must implement to receive events:

```java
public abstract class ArCallback {
    /**
     * Callback for event changes from the XR Glasses, such as 3D/2D switch event.
     * @param eventId EVENT ID (EVENT_ID_*)
     * @param event Event data (little-endian)
     * @param timestamp Timestamp in milliseconds
     */
    public void onEvent(int eventId, byte[] event, long timestamp) {
        // Override this method to handle events
    }
    
    /**
     * Callback for IMU Data (big-endian)
     * @param timestamp Timestamp in milliseconds
     * @param imuData The IMU data: the first 12 bytes represent Euler values,
     *       Format:
     *       - uint8_t eulerRoll[4];
     *       - uint8_t eulerPitch[4];
     *       - uint8_t eulerYaw[4];
     *       (Each value is a 32-bit float in big-endian format)
     */
    public void onImu(long timestamp, byte[] imuData) {
        // Override this method to handle IMU data
    }
}
```

### Implementing ArCallback

To implement the callback, create a class that extends `ArCallback` and override the methods:

```java
private ArCallback arCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        // Handle events based on eventId
        switch (eventId) {
            case Constants.EVENT_ID_INIT:
                handleInitEvent(event);
                break;
            case Constants.EVENT_ID_3D:
                handle3DEvent(event);
                break;
            // Handle other events
        }
    }
    
    @Override
    public void onImu(long timestamp, byte[] imuData) {
        // Process IMU data
        processImuData(timestamp, imuData);
    }
};
```

## Registering and Unregistering Callbacks

To receive events, you need to register your callback with the `ArManager`:

```java
// Get ArManager instance
ArManager arManager = ArManager.getInstance(context);

// Register callback
arManager.registerCallback(arCallback);
```

It's equally important to unregister callbacks when they're no longer needed:

```java
// Unregister callback
arManager.unregisterCallback(arCallback);
```

### Lifecycle Management

Always align callback registration and unregistration with your application's lifecycle:

```java
@Override
protected void onResume() {
    super.onResume();
    // Register callback when activity becomes visible
    arManager.registerCallback(arCallback);
}

@Override
protected void onPause() {
    super.onPause();
    // Unregister callback when activity is no longer visible
    arManager.unregisterCallback(arCallback);
}
```

This ensures that:
- Your application receives events when it's in the foreground
- You prevent memory leaks by releasing references when they're not needed
- You don't process events when your UI isn't visible

## Event Types

The SDK reports several event types through the `onEvent` callback, each identified by a unique event ID:

| Event ID Constant | Value | Description |
|-------------------|-------|-------------|
| `Constants.EVENT_ID_INIT` | 0 | Initialization status event |
| `Constants.EVENT_ID_BRIGHTNESS` | 0x0301 | Brightness change event |
| `Constants.EVENT_ID_3D` | 0x0302 | 3D/2D mode change event |
| `Constants.EVENT_ID_VOICE` | 0x0304 | Voice-related event |

### Handling Different Event Types

Different event types provide different data structures in the `event` byte array:

```java
@Override
public void onEvent(int eventId, byte[] event, long timestamp) {
    switch (eventId) {
        case Constants.EVENT_ID_INIT:
            // Parse initialization event
            int initStatus = byteArrayToInt(event, 0, event.length);
            handleInitializationStatus(initStatus);
            break;
            
        case Constants.EVENT_ID_3D:
            // Parse 3D mode event
            int mode3DState = byteArrayToInt(event, 0, event.length);
            boolean is3DMode = (mode3DState == Constants.STATE_ON);
            handle3DModeChange(is3DMode);
            break;
            
        // Handle other event types
    }
}
```

### Event Data Parsing

The `event` parameter is a byte array containing event-specific data. You'll need to parse this data according to the event type:

```java
// Helper method to convert byte array to integer
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
    
    // Little-endian byte order for event data
    for (int i = offset; i < right; i++) {
        int shift = (i - offset) * 8;
        value += (bytes[i] & 0x000000FF) << shift;
    }
    
    return value;
}
```

## IMU Data Callback

The SDK provides a separate callback method, `onImu`, specifically for receiving IMU (Inertial Measurement Unit) data:

```java
@Override
public void onImu(long timestamp, byte[] imuData) {
    // Process IMU data
    if (imuData.length >= 12) {
        ByteBuffer buffer = ByteBuffer.wrap(imuData);
        
        // Extract Euler angles (in radians)
        float roll = buffer.getFloat(0);   // Roll (rotation around front axis)
        float pitch = buffer.getFloat(4);  // Pitch (rotation around right axis)
        float yaw = buffer.getFloat(8);    // Yaw (rotation around up axis)
        
        // Process orientation data
        processOrientationData(roll, pitch, yaw);
    }
    
    // Check for quaternion data (if available)
    if (imuData.length >= 36) {
        ByteBuffer buffer = ByteBuffer.wrap(imuData);
        
        // Extract quaternion data
        float quaternionW = buffer.getFloat(20);
        float quaternionX = buffer.getFloat(24);
        float quaternionY = buffer.getFloat(28);
        float quaternionZ = buffer.getFloat(32);
        
        // Process quaternion data
        processQuaternionData(quaternionW, quaternionX, quaternionY, quaternionZ);
    }
}
```

### IMU Data Format

The `imuData` parameter contains a byte array with the following structure:

- **Bytes 0-11**: Euler angles (roll, pitch, yaw)
  - Each angle is a 32-bit float in big-endian format
  - Roll: bytes 0-3
  - Pitch: bytes 4-7
  - Yaw: bytes 8-11

- **Bytes 20-35**: Quaternion (if available, requires firmware up to 01.0.02.007_20231212)
  - Each component is a 32-bit float in big-endian format
  - W: bytes 20-23
  - X: bytes 24-27
  - Y: bytes 28-31
  - Z: bytes 32-35

### Enabling IMU Data

To receive IMU data, you must enable it after initialization:

```java
// Enable IMU data reporting
if (arManager.init() == Constants.ERROR_INIT_SUCCESS) {
    arManager.setImuOn(true);
}
```

You can also control the IMU reporting frequency:

```java
// Set IMU frequency (60Hz, 90Hz, 120Hz, or 240Hz)
arManager.setImuFrequency(Constants.IMU_FREQUENCE_90);
```

## Processing Events on the UI Thread

Since callback methods are typically called from a background thread, you must use the UI thread to update the UI:

```java
@Override
public void onEvent(int eventId, byte[] event, long timestamp) {
    if (eventId == Constants.EVENT_ID_3D) {
        final int mode = byteArrayToInt(event, 0, event.length);
        
        // Update UI on the main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI3DMode(mode == Constants.STATE_ON);
            }
        });
    }
}

@Override
public void onImu(long timestamp, byte[] imuData) {
    if (imuData.length >= 12) {
        ByteBuffer buffer = ByteBuffer.wrap(imuData);
        final float roll = buffer.getFloat(0);
        final float pitch = buffer.getFloat(4);
        final float yaw = buffer.getFloat(8);
        
        // Update UI on the main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUIWithRotation(roll, pitch, yaw);
            }
        });
    }
}
```

### Optimizing Performance

For high-frequency callbacks like IMU data, avoid excessive UI updates:

```java
private long lastUiUpdateTime = 0;
private static final long UI_UPDATE_INTERVAL_MS = 33; // ~30 FPS

@Override
public void onImu(long timestamp, byte[] imuData) {
    // Process data
    processImuData(imuData);
    
    // Throttle UI updates
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastUiUpdateTime > UI_UPDATE_INTERVAL_MS) {
        lastUiUpdateTime = currentTime;
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }
}
```

## Working with Multiple Callbacks

You can register multiple callbacks with the `ArManager`:

```java
// Register main activity callback
arManager.registerCallback(mainActivityCallback);

// Register another callback for a different component
arManager.registerCallback(headTrackingCallback);
```

### When to Use Multiple Callbacks

Using multiple callbacks can be beneficial in several scenarios:

1. **Separation of concerns**: Different components can handle different aspects of the glasses' functionality
2. **Modular architecture**: Components can be added or removed without affecting the main callback
3. **Specialized processing**: Some callbacks might focus on specific tasks like UI updates, while others handle data recording

### Implementation Example

```java
// Main callback for UI updates
private ArCallback uiCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        // Update UI based on events
    }
    
    @Override
    public void onImu(long timestamp, byte[] imuData) {
        // Update UI with IMU data at reduced frequency
    }
};

// Data processing callback
private ArCallback dataCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        // Record events for analysis
    }
    
    @Override
    public void onImu(long timestamp, byte[] imuData) {
        // Process and record IMU data at full frequency
    }
};

// Register both callbacks
arManager.registerCallback(uiCallback);
arManager.registerCallback(dataCallback);

// Don't forget to unregister both
@Override
protected void onPause() {
    super.onPause();
    arManager.unregisterCallback(uiCallback);
    arManager.unregisterCallback(dataCallback);
}
```

## Example Implementation

Here's a complete example of a class that handles VITURE XR Glasses callbacks:

```java
public class VitureEventHandler {
    private static final String TAG = "VitureEventHandler";
    
    private final Context context;
    private final ArManager arManager;
    private final ArCallback arCallback;
    private final EventListener eventListener;
    
    // Interface for event listeners
    public interface EventListener {
        void onInitialized(boolean success);
        void on3DModeChanged(boolean enabled);
        void onImuDataUpdated(float roll, float pitch, float yaw);
    }
    
    public VitureEventHandler(Context context, EventListener listener) {
        this.context = context;
        this.eventListener = listener;
        
        // Get ArManager instance
        arManager = ArManager.getInstance(context);
        
        // Create callback
        arCallback = new ArCallback() {
            @Override
            public void onEvent(int eventId, byte[] event, long timestamp) {
                handleEvent(eventId, event, timestamp);
            }
            
            @Override
            public void onImu(long timestamp, byte[] imuData) {
                handleImuData(timestamp, imuData);
            }
        };
    }
    
    public void initialize() {
        // Initialize SDK
        int result = arManager.init();
        
        // Register callback
        arManager.registerCallback(arCallback);
        
        // Report initial state
        if (result == Constants.ERROR_INIT_SUCCESS) {
            eventListener.onInitialized(true);
            
            // Get and report initial states
            reportInitialStates();
        } else {
            eventListener.onInitialized(false);
        }
    }
    
    public void release() {
        // Unregister callback
        arManager.unregisterCallback(arCallback);
    }
    
    private void reportInitialStates() {
        // Get and report 3D mode state
        int mode3DState = arManager.get3DState();
        if (mode3DState >= 0) {
            eventListener.on3DModeChanged(mode3DState == Constants.STATE_ON);
        }
    }
    
    private void handleEvent(int eventId, byte[] event, long timestamp) {
        switch (eventId) {
            case Constants.EVENT_ID_INIT:
                int initResult = byteArrayToInt(event, 0, event.length);
                boolean success = (initResult == Constants.ERROR_INIT_SUCCESS);
                
                // Report on main thread
                final boolean finalSuccess = success;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        eventListener.onInitialized(finalSuccess);
                    }
                });
                break;
                
            case Constants.EVENT_ID_3D:
                int modeState = byteArrayToInt(event, 0, event.length);
                final boolean is3DEnabled = (modeState == Constants.STATE_ON);
                
                // Report on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        eventListener.on3DModeChanged(is3DEnabled);
                    }
                });
                break;
                
            default:
                Log.d(TAG, "Unhandled event ID: " + eventId);
                break;
        }
    }
    
    private void handleImuData(long timestamp, byte[] imuData) {
        if (imuData.length >= 12) {
            ByteBuffer buffer = ByteBuffer.wrap(imuData);
            
            // Extract Euler angles (in radians)
            final float roll = buffer.getFloat(0);
            final float pitch = buffer.getFloat(4);
            final float yaw = buffer.getFloat(8);
            
            // Report on main thread (throttled)
            if (shouldUpdateImuUI()) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        eventListener.onImuDataUpdated(roll, pitch, yaw);
                    }
                });
            }
        }
    }
    
    // UI update throttling
    private long lastImuUpdateTime = 0;
    private static final long IMU_UI_UPDATE_INTERVAL_MS = 33; // ~30 FPS
    
    private boolean shouldUpdateImuUI() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastImuUpdateTime > IMU_UI_UPDATE_INTERVAL_MS) {
            lastImuUpdateTime = currentTime;
            return true;
        }
        return false;
    }
    
    // Helper method for byte array conversion
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
    
    // Public methods to control the glasses
    
    public void setImuEnabled(boolean enabled) {
        arManager.setImuOn(enabled);
    }
    
    public void set3DMode(boolean enabled) {
        arManager.set3D(enabled);
    }
    
    public void setImuFrequency(int frequency) {
        arManager.setImuFrequency(frequency);
    }
}
```

### Using the Event Handler

```java
public class MyActivity extends AppCompatActivity implements VitureEventHandler.EventListener {
    private VitureEventHandler vitureHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        
        // Create handler
        vitureHandler = new VitureEventHandler(this, this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Initialize and register
        vitureHandler.initialize();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Release
        vitureHandler.release();
    }
    
    // Event listener implementations
    
    @Override
    public void onInitialized(boolean success) {
        if (success) {
            showMessage("Glasses connected successfully");
            
            // Enable IMU data
            vitureHandler.setImuEnabled(true);
        } else {
            showMessage("Failed to connect to glasses");
        }
    }
    
    @Override
    public void on3DModeChanged(boolean enabled) {
        showMessage("3D mode: " + (enabled ? "enabled" : "disabled"));
        
        // Update UI to reflect current 3D mode
        updateUI3DMode(enabled);
    }
    
    @Override
    public void onImuDataUpdated(float roll, float pitch, float yaw) {
        // Update UI with orientation data
        updateRotationDisplay(roll, pitch, yaw);
    }
    
    // UI helper methods
    
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void updateUI3DMode(boolean enabled) {
        // Update UI controls for 3D mode
    }
    
    private void updateRotationDisplay(float roll, float pitch, float yaw) {
        // Update rotation display in UI
    }
}
```

## Troubleshooting

### Common Callback Issues

#### 1. Events Not Received

**Symptoms:**
- Callback methods are not being called
- No initialization events or IMU data received

**Solutions:**
- Ensure you've registered the callback with `arManager.registerCallback(arCallback)`
- Check that the SDK was initialized successfully
- Verify the glasses are properly connected
- For IMU data, ensure you've enabled it with `arManager.setImuOn(true)`
- Check if the callback is being unregistered prematurely

#### 2. Memory Leaks

**Symptoms:**
- Increased memory usage over time
- Application crashes after extended use

**Solutions:**
- Always unregister callbacks in `onPause` or `onDestroy`
- Avoid strong references to Activities or Fragments in callbacks
- Consider using weak references if callbacks outlive their parent components

```java
private static class MyCallback extends ArCallback {
    private final WeakReference<MyActivity> activityRef;
    
    MyCallback(MyActivity activity) {
        this.activityRef = new WeakReference<>(activity);
    }
    
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        MyActivity activity = activityRef.get();
        if (activity != null && !activity.isFinishing()) {
            activity.handleEvent(eventId, event);
        }
    }
}
```

#### 3. UI Thread Violations

**Symptoms:**
- Application crashes with `CalledFromWrongThreadException`
- UI updates don't appear

**Solutions:**
- Always update UI elements on the main thread
- Use `runOnUiThread`, `Handler(Looper.getMainLooper())`, or similar mechanisms

#### 4. High CPU Usage

**Symptoms:**
- Excessive battery drain
- Application becomes sluggish

**Solutions:**
- Throttle UI updates, especially for high-frequency IMU data
- Perform heavy processing on background threads
- Consider reducing IMU frequency if full rate isn't needed

#### 5. Incorrect Data Parsing

**Symptoms:**
- IMU data shows wrong values or unexpected behavior
- Event data is not interpreted correctly

**Solutions:**
- Remember that IMU data uses big-endian format
- Event data uses little-endian format
- Double-check byte offsets when parsing data
- Verify buffer wraparound logic
- Check for proper conversion between radians and degrees if needed

### Debugging Tips

1. **Add Logging**

```java
@Override
public void onEvent(int eventId, byte[] event, long timestamp) {
    Log.d(TAG, "Event received: ID=" + eventId + ", timestamp=" + timestamp);
    // Log event data
    if (event != null) {
        Log.d(TAG, "Event data: " + bytesToHex(event));
    }
    
    // Process event
}

private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02X ", b));
    }
    return sb.toString();
}
```

2. **Test with Simplified Callbacks**

Create a simple test callback to verify basic functionality:

```java
ArCallback testCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        Log.d(TAG, "Event received: " + eventId);
    }
    
    @Override
    public void onImu(long timestamp, byte[] imuData) {
        Log.d(TAG, "IMU data received, length: " + (imuData != null ? imuData.length : 0));
    }
};

// Register test callback
arManager.registerCallback(testCallback);
```

3. **Verify Registration Status**

If available, check if your callback is properly registered:

```java
// After registering
arManager.registerCallback(arCallback);
Log.d(TAG, "Callback registered");

// Test if IMU is enabled
int imuState = arManager.getImuState();
Log.d(TAG, "IMU state after registration: " + imuState);
```

## Conclusion

The event callback system in the VITURE XR Glasses SDK provides a powerful mechanism for receiving and responding to real-time data from the glasses. By properly implementing, registering, and managing callbacks, you can create responsive and interactive applications that take full advantage of the glasses' capabilities.

Key points to remember:
- Implement the `ArCallback` interface to receive events and IMU data
- Register callbacks when your application is active and unregister when inactive
- Process events according to their type and data format
- Update UI elements on the main thread
- Optimize performance for high-frequency data like IMU updates
- Consider using multiple callbacks for separation of concerns

For more information on specific features and capabilities, refer to the other documentation guides in this series.
