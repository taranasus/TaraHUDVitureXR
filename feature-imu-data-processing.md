# VITURE XR Glasses SDK - IMU Data Processing

This guide explains how to work with IMU (Inertial Measurement Unit) data from the VITURE XR Glasses, which is essential for implementing head tracking and motion-based interactions in your application.

## Table of Contents

1. [Introduction to IMU Data](#introduction-to-imu-data)
2. [Accessing IMU Data](#accessing-imu-data)
3. [Understanding the Coordinate System](#understanding-the-coordinate-system)
4. [Working with Euler Angles](#working-with-euler-angles)
5. [Working with Quaternions](#working-with-quaternions)
6. [Controlling IMU Frequency](#controlling-imu-frequency)
7. [Implementing Head Tracking](#implementing-head-tracking)
8. [Example: Moving an Object with Head Movement](#example-moving-an-object-with-head-movement)
9. [Performance Considerations](#performance-considerations)
10. [Troubleshooting](#troubleshooting)

## Introduction to IMU Data

The Inertial Measurement Unit (IMU) in the VITURE XR Glasses provides real-time data about the orientation and movement of the user's head. This data is crucial for creating immersive experiences that respond to the user's head movements.

The IMU provides two main types of orientation data:
- **Euler angles** (roll, pitch, yaw) - simpler to understand but can suffer from gimbal lock
- **Quaternions** - more complex but provide smoother rotations without gimbal lock

## Accessing IMU Data

To access IMU data from the VITURE XR Glasses, you need to:

1. Initialize the SDK (see the Getting Started guide)
2. Register an ArCallback to receive IMU events
3. Enable IMU data streaming
4. Process the incoming data in the onImu method

### Basic Implementation

```java
// Create your ArCallback
ArCallback arCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long timestamp) {
        // Handle other events here
    }

    @Override
    public void onImu(long timestamp, byte[] imuData) {
        // Process IMU data
        if (imuData.length >= 12) {
            ByteBuffer buffer = ByteBuffer.wrap(imuData);
            
            // Extract Euler angles (in radians)
            float roll = buffer.getFloat(0);   // Roll (rotation around front axis)
            float pitch = buffer.getFloat(4);  // Pitch (rotation around right axis)
            float yaw = buffer.getFloat(8);    // Yaw (rotation around up axis)
            
            // Process the orientation data...
        }
    }
};

// Register the callback
arManager.registerCallback(arCallback);

// Enable IMU data streaming
arManager.setImuOn(true);
```

## Understanding the Coordinate System

The VITURE XR Glasses use a right-handed coordinate system:

- **Roll**: Rotation around the front axis (X) - tilting your head left and right
- **Pitch**: Rotation around the right axis (Y) - looking up and down
- **Yaw**: Rotation around the up axis (Z) - turning your head left and right

![VITURE Glasses Coordinates](Screenshot%202025-03-12%20at%2014.28.04.jpg)

Values are provided in radians, not degrees. If you need to display or work with degrees, convert using:

```java
float rollDegrees = (float) Math.toDegrees(rollRadians);
float pitchDegrees = (float) Math.toDegrees(pitchRadians);
float yawDegrees = (float) Math.toDegrees(yawRadians);
```

## Working with Euler Angles

Euler angles are the simplest way to understand rotation. The IMU data provides three Euler angles:

```java
// In the onImu method
float roll = buffer.getFloat(0);   // Rotation around front axis (X)
float pitch = buffer.getFloat(4);  // Rotation around right axis (Y)
float yaw = buffer.getFloat(8);    // Rotation around up axis (Z)
```

### Practical Uses for Euler Angles

1. **UI Navigation**: Use yaw (left/right) and pitch (up/down) to navigate menus or move a cursor
2. **Camera Control**: Map head movements to camera rotations in games or 3D applications
3. **Simple Gestures**: Detect simple head gestures like nodding (pitch) or shaking (yaw)

### Example: Moving a Cursor with Head Movement

```java
private void updateCursorPosition(float yaw, float pitch) {
    // Convert angular movement to screen coordinates
    // This example assumes:
    // - Center of screen is starting point
    // - Sensitivity factors control how much movement occurs per degree
    float screenX = screenCenterX + (yawSensitivity * yaw);
    float screenY = screenCenterY + (pitchSensitivity * pitch);
    
    // Clamp to screen boundaries
    screenX = Math.max(0, Math.min(screenWidth, screenX));
    screenY = Math.max(0, Math.min(screenHeight, screenY));
    
    // Update cursor position
    cursorView.setX(screenX);
    cursorView.setY(screenY);
}
```

## Working with Quaternions

Quaternions provide a more robust way to represent 3D rotations without the issues of gimbal lock that can affect Euler angles. The SDK provides quaternion data in newer firmware versions.

```java
// In the onImu method, check for quaternion data (minimum 36 bytes)
if (imuData.length >= 36) {
    float quaternionW = buffer.getFloat(20);
    float quaternionX = buffer.getFloat(24);
    float quaternionY = buffer.getFloat(28);
    float quaternionZ = buffer.getFloat(32);
    
    // Use quaternion data for rotation
}
```

### Converting Quaternions to Rotation Matrices

For many Android graphics applications, you'll need to convert quaternions to rotation matrices:

```java
private float[] quaternionToMatrix(float qW, float qX, float qY, float qZ) {
    float[] matrix = new float[16];
    
    float sqw = qW * qW;
    float sqx = qX * qX;
    float sqy = qY * qY;
    float sqz = qZ * qZ;
    
    // Inverted quaternion is faster than inverse matrix
    float invs = 1 / (sqx + sqy + sqz + sqw);
    
    // Row 1
    matrix[0] = (sqx - sqy - sqz + sqw) * invs;
    matrix[1] = 2.0f * (qX * qY + qZ * qW) * invs;
    matrix[2] = 2.0f * (qX * qZ - qY * qW) * invs;
    matrix[3] = 0.0f;
    
    // Row 2
    matrix[4] = 2.0f * (qX * qY - qZ * qW) * invs;
    matrix[5] = (-sqx + sqy - sqz + sqw) * invs;
    matrix[6] = 2.0f * (qY * qZ + qX * qW) * invs;
    matrix[7] = 0.0f;
    
    // Row 3
    matrix[8] = 2.0f * (qX * qZ + qY * qW) * invs;
    matrix[9] = 2.0f * (qY * qZ - qX * qW) * invs;
    matrix[10] = (-sqx - sqy + sqz + sqw) * invs;
    matrix[11] = 0.0f;
    
    // Row 4
    matrix[12] = 0.0f;
    matrix[13] = 0.0f;
    matrix[14] = 0.0f;
    matrix[15] = 1.0f;
    
    return matrix;
}
```

### Using Quaternions with OpenGL

For OpenGL applications, you can use quaternions to rotate 3D objects:

```java
// In your OpenGL renderer
public void onDrawFrame(GL10 gl) {
    // Clear the rendering surface
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    
    // Set up the camera view
    Matrix.setLookAtM(viewMatrix, 0, 
                     0, 0, -3,  // Eye position
                     0, 0, 0,   // Look at position
                     0, 1, 0);  // Up vector
    
    // Apply head rotation from quaternion
    float[] rotationMatrix = quaternionToMatrix(quaternionW, quaternionX, 
                                               quaternionY, quaternionZ);
    
    // Apply rotation to model matrix
    Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, identityMatrix, 0);
    
    // Combine model, view, and projection matrices
    Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
    Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
    
    // Draw objects with the combined matrix
    myObject.draw(mvpMatrix);
}
```

## Controlling IMU Frequency

The VITURE SDK allows you to control the frequency at which IMU data is reported. Higher frequencies provide smoother motion but consume more resources.

```java
// Set IMU frequency to one of the predefined rates
// Constants.IMU_FREQUENCE_60  = 0 (60Hz)
// Constants.IMU_FREQUENCE_90  = 1 (90Hz)
// Constants.IMU_FREQUENCE_120 = 2 (120Hz)
// Constants.IMU_FREQUENCE_240 = 3 (240Hz)
arManager.setImuFrequency(Constants.IMU_FREQUENCE_90);

// You can also check the current frequency
int currentFreq = arManager.getCurImuFrequency();
```

### Choosing the Right Frequency

- **60Hz**: Suitable for most UI-based applications
- **90Hz**: Good balance for most AR/VR applications
- **120Hz**: Better for fast-paced games or applications requiring precise tracking
- **240Hz**: Highest precision, but consumes more battery and processing power

## Implementing Head Tracking

### Basic Head Tracking

Here's a complete example of basic head tracking that moves a view based on head rotation:

```java
public class HeadTrackingActivity extends AppCompatActivity {
    private static final String TAG = "HeadTracking";
    
    private View targetView;
    private ArManager arManager;
    private ArCallback arCallback;
    
    private float baseYaw = 0;
    private float basePitch = 0;
    private boolean isCalibrated = false;
    
    // Sensitivity factors (adjust as needed)
    private final float yawSensitivity = 20;   // pixels per degree
    private final float pitchSensitivity = 20; // pixels per degree
    
    private int screenWidth;
    private int screenHeight;
    private float centerX;
    private float centerY;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_tracking);
        
        targetView = findViewById(R.id.target_view);
        
        // Get screen dimensions
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        centerX = screenWidth / 2f;
        centerY = screenHeight / 2f;
        
        // Initialize SDK
        initVitureSDK();
        
        // Add a button to reset calibration
        Button calibrateBtn = findViewById(R.id.calibrate_button);
        calibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCalibrated = false;
            }
        });
    }
    
    private void initVitureSDK() {
        arManager = ArManager.getInstance(this);
        
        arCallback = new ArCallback() {
            @Override
            public void onEvent(int eventId, byte[] event, long timestamp) {
                // Handle initialization and other events
            }
            
            @Override
            public void onImu(long timestamp, byte[] imuData) {
                if (imuData.length >= 12) {
                    ByteBuffer buffer = ByteBuffer.wrap(imuData);
                    
                    // Extract Euler angles (in radians)
                    float roll = buffer.getFloat(0);
                    float pitch = buffer.getFloat(4);
                    float yaw = buffer.getFloat(8);
                    
                    // Convert to degrees for easier calculations
                    float yawDeg = (float) Math.toDegrees(yaw);
                    float pitchDeg = (float) Math.toDegrees(pitch);
                    
                    // Calibrate if needed
                    if (!isCalibrated) {
                        baseYaw = yawDeg;
                        basePitch = pitchDeg;
                        isCalibrated = true;
                    }
                    
                    // Calculate relative movement
                    final float relativeYaw = yawDeg - baseYaw;
                    final float relativePitch = pitchDeg - basePitch;
                    
                    // Update UI on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateViewPosition(relativeYaw, relativePitch);
                        }
                    });
                }
            }
        };
        
        // Register callback and enable IMU
        arManager.registerCallback(arCallback);
        arManager.setImuOn(true);
        
        // Set optimal frequency for head tracking
        arManager.setImuFrequency(Constants.IMU_FREQUENCE_90);
    }
    
    private void updateViewPosition(float yaw, float pitch) {
        // Calculate new position based on head movement
        float newX = centerX + (yaw * yawSensitivity);
        float newY = centerY + (pitch * pitchSensitivity);
        
        // Clamp to screen boundaries
        newX = Math.max(0, Math.min(screenWidth - targetView.getWidth(), newX));
        newY = Math.max(0, Math.min(screenHeight - targetView.getHeight(), newY));
        
        // Update view position
        targetView.setX(newX);
        targetView.setY(newY);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (arManager != null && arCallback != null) {
            arManager.registerCallback(arCallback);
            arManager.setImuOn(true);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (arManager != null && arCallback != null) {
            arManager.setImuOn(false);
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

### XML Layout for Head Tracking Example

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Target view that will move with head tracking -->
    <View
        android:id="@+id/target_view"
        android:layout_width="50dp"
        android:layout_height="50dp"
        android:background="@drawable/circle_cursor"
        android:layout_gravity="center" />

    <!-- Calibration button -->
    <Button
        android:id="@+id/calibrate_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="16dp"
        android:text="Recalibrate" />

</FrameLayout>
```

### Cursor Drawable (circle_cursor.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#FF4081" />
    <stroke
        android:width="2dp"
        android:color="#FFFFFF" />
</shape>
```

## Example: Moving an Object with Head Movement

Building on the previous example, let's implement a simple game where the player moves an object to collect targets:

```java
public class HeadControlGameActivity extends AppCompatActivity {
    private View playerObject;
    private View targetObject;
    private TextView scoreText;
    private int score = 0;
    
    private ArManager arManager;
    private ArCallback arCallback;
    
    private float baseYaw = 0;
    private float basePitch = 0;
    private boolean isCalibrated = false;
    
    private final float sensitivity = 15; // Adjust as needed
    private final int objectSize = 50;    // Size of player object in dp
    
    private int screenWidth;
    private int screenHeight;
    private Random random = new Random();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_game);
        
        playerObject = findViewById(R.id.player_object);
        targetObject = findViewById(R.id.target_object);
        scoreText = findViewById(R.id.score_text);
        
        // Get screen dimensions
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        
        // Initialize SDK
        initVitureSDK();
        
        // Place initial target
        placeTargetRandomly();
    }
    
    private void initVitureSDK() {
        arManager = ArManager.getInstance(this);
        
        arCallback = new ArCallback() {
            @Override
            public void onEvent(int eventId, byte[] event, long timestamp) {
                // Handle other events
            }
            
            @Override
            public void onImu(long timestamp, byte[] imuData) {
                if (imuData.length >= 12) {
                    ByteBuffer buffer = ByteBuffer.wrap(imuData);
                    
                    float roll = buffer.getFloat(0);
                    float pitch = buffer.getFloat(4);
                    float yaw = buffer.getFloat(8);
                    
                    // Convert to degrees
                    float yawDeg = (float) Math.toDegrees(yaw);
                    float pitchDeg = (float) Math.toDegrees(pitch);
                    
                    // Calibrate if needed
                    if (!isCalibrated) {
                        baseYaw = yawDeg;
                        basePitch = pitchDeg;
                        isCalibrated = true;
                    }
                    
                    // Calculate relative movement
                    final float relativeYaw = yawDeg - baseYaw;
                    final float relativePitch = pitchDeg - basePitch;
                    
                    // Update on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            movePlayerObject(relativeYaw, relativePitch);
                            checkCollision();
                        }
                    });
                }
            }
        };
        
        // Register callback and enable IMU
        arManager.registerCallback(arCallback);
        arManager.setImuOn(true);
        arManager.setImuFrequency(Constants.IMU_FREQUENCE_90);
    }
    
    private void movePlayerObject(float yaw, float pitch) {
        // Calculate new position
        float newX = playerObject.getX() - (yaw * sensitivity);
        float newY = playerObject.getY() + (pitch * sensitivity);
        
        // Keep within screen bounds
        newX = Math.max(0, Math.min(screenWidth - playerObject.getWidth(), newX));
        newY = Math.max(0, Math.min(screenHeight - playerObject.getHeight(), newY));
        
        // Update position
        playerObject.setX(newX);
        playerObject.setY(newY);
    }
    
    private void placeTargetRandomly() {
        int maxX = screenWidth - targetObject.getWidth();
        int maxY = screenHeight - targetObject.getHeight();
        
        targetObject.setX(random.nextInt(maxX));
        targetObject.setY(random.nextInt(maxY));
    }
    
    private void checkCollision() {
        // Calculate centers of both objects
        float playerCenterX = playerObject.getX() + playerObject.getWidth() / 2;
        float playerCenterY = playerObject.getY() + playerObject.getHeight() / 2;
        
        float targetCenterX = targetObject.getX() + targetObject.getWidth() / 2;
        float targetCenterY = targetObject.getY() + targetObject.getHeight() / 2;
        
        // Calculate distance between centers
        float distance = (float) Math.sqrt(
            Math.pow(playerCenterX - targetCenterX, 2) +
            Math.pow(playerCenterY - targetCenterY, 2)
        );
        
        // If objects are close enough, count as collision
        float collisionThreshold = (playerObject.getWidth() + targetObject.getWidth()) / 2;
        if (distance < collisionThreshold) {
            // Increment score
            score++;
            scoreText.setText("Score: " + score);
            
            // Move target to new location
            placeTargetRandomly();
        }
    }
    
    // Lifecycle methods same as previous example
}
```

## Performance Considerations

### 1. IMU Frequency Selection

Choose the appropriate IMU frequency based on your application needs:
- For simple UI navigation, 60Hz is usually sufficient
- For most AR/VR applications, 90Hz provides a good balance
- Only use 120Hz or 240Hz if you need highly responsive tracking

### 2. Processing on Background Threads

The onImu callback may be called very frequently. To avoid UI jitter:
- Keep computation in the callback as light as possible
- For heavy processing, use a background thread
- Only update the UI on the main thread

Example of using a background thread for processing:

```java
private ExecutorService executor = Executors.newSingleThreadExecutor();

// In your ArCallback
@Override
public void onImu(long timestamp, byte[] imuData) {
    if (imuData.length >= 12) {
        // Make a copy of the data to avoid issues with reused buffers
        final byte[] imuDataCopy = Arrays.copyOf(imuData, imuData.length);
        
        // Process on background thread
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Do heavy computation here
                ByteBuffer buffer = ByteBuffer.wrap(imuDataCopy);
                float[] processedData = processImuData(buffer);
                
                // Update UI on main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUIWithProcessedData(processedData);
                    }
                });
            }
        });
    }
}

@Override
protected void onDestroy() {
    super.onDestroy();
    // Shutdown executor when done
    executor.shutdown();
}
```

### 3. Filtering and Smoothing

Raw IMU data may contain noise. Apply filtering to achieve smoother movement:

```java
// Simple exponential smoothing filter
public class SmoothingFilter {
    private float alpha = 0.3f; // Smoothing factor (0-1)
    private float lastValue = 0;
    private boolean isInitialized = false;
    
    public float filter(float newValue) {
        if (!isInitialized) {
            lastValue = newValue;
            isInitialized = true;
            return newValue;
        }
        
        // Apply smoothing
        float smoothedValue = alpha * newValue + (1 - alpha) * lastValue;
        lastValue = smoothedValue;
        return smoothedValue;
    }
    
    public void reset() {
        isInitialized = false;
    }
}

// Usage
private SmoothingFilter yawFilter = new SmoothingFilter();
private SmoothingFilter pitchFilter = new SmoothingFilter();

// Apply to IMU data
float smoothedYaw = yawFilter.filter(yaw);
float smoothedPitch = pitchFilter.filter(pitch);
```

### 4. Battery Optimization

To optimize battery usage:
- Stop IMU data when not needed (e.g., when the app is in the background)
- Use the lowest frequency that meets your needs
- Process data efficiently to minimize CPU usage

```java
@Override
protected void onPause() {
    super.onPause();
    // Disable IMU when app is not visible
    if (arManager != null) {
        arManager.setImuOn(false);
    }
}

@Override
protected void onResume() {
    super.onResume();
    // Re-enable IMU when app is visible
    if (arManager != null) {
        arManager.setImuOn(true);
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Erratic or Jumpy Movement

**Symptoms:**
- Head tracking is not smooth
- Objects move erratically
- Position jumps suddenly

**Solutions:**
- Apply smoothing filters to the IMU data
- Lower the sensitivity of your movement calculations
- Ensure you're not using very small movement thresholds

#### 2. Drift Over Time

**Symptoms:**
- Cursor or objects slowly drift in one direction
- After a while, the center position is off

**Solutions:**
- Implement a recalibration feature
- Use a small deadzone around the center position
- Apply a high-pass filter to eliminate slow drift

```java
// Example deadzone implementation
private void applyDeadzone(float value, float deadzoneSize) {
    if (Math.abs(value) < deadzoneSize) {
        return 0;
    } else {
        // Preserve sign but subtract deadzone size
        return value - Math.signum(value) * deadzoneSize;
    }
}
```

#### 3. Delayed Response

**Symptoms:**
- Noticeable lag between head movement and screen response
- Movement feels sluggish

**Solutions:**
- Increase IMU frequency (e.g., from 60Hz to 90Hz)
- Optimize your processing code to reduce computation time
- Ensure you're not doing heavy work on the main thread
- Reduce alpha value in smoothing filters for more responsive (but potentially less smooth) movement

#### 4. No IMU Data

**Symptoms:**
- onImu callback is never called
- No reaction to head movement

**Solutions:**
- Check that the SDK was initialized successfully
- Verify that IMU data is enabled with arManager.setImuOn(true)
- Confirm that the USB connection is working
- Check that you've registered the callback correctly
- Enable logging with arManager.setLogOn(true) and check logcat for errors

### Logging and Debugging

Add comprehensive logging to help diagnose issues:

```java
@Override
public void onImu(long timestamp, byte[] imuData) {
    if (imuData.length >= 12) {
        ByteBuffer buffer = ByteBuffer.wrap(imuData);
        
        float roll = buffer.getFloat(0);
        float pitch = buffer.getFloat(4);
        float yaw = buffer.getFloat(8);
        
        // Log every few frames to avoid flooding the log
        if (++logCounter % 30 == 0) {
            Log.d(TAG, String.format("IMU: roll=%.2f, pitch=%.2f, yaw=%.2f",
                  Math.toDegrees(roll), Math.toDegrees(pitch), Math.toDegrees(yaw)));
        }
        
        // Continue processing...
    }
}
```

## Conclusion

The IMU data provided by the VITURE XR Glasses enables a wide range of interactive possibilities. By effectively processing and utilizing this data, you can create intuitive head-tracked interfaces, immersive 3D experiences, and novel interaction paradigms.

Remember that effective head tracking requires balancing sensitivity, smoothness, and responsiveness to create a comfortable user experience. Start with the examples provided and adjust parameters based on your specific application requirements.

For more advanced usage, consider combining IMU data with other input methods (touch, controllers, etc.) to create hybrid interaction systems that offer both precision and convenience.
