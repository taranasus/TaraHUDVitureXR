# VITURE XR Glasses SDK - Getting Started Guide

This guide will walk you through setting up your development environment, creating a new Android project, integrating the VITURE XR Glasses SDK, and building a simple application that interfaces with the glasses.

## Table of Contents

1. [Development Environment Setup](#development-environment-setup)
2. [Creating a New Android Project](#creating-a-new-android-project)
3. [Adding the VITURE SDK](#adding-the-viture-sdk)
4. [USB Permissions Configuration](#usb-permissions-configuration)
5. [Basic SDK Initialization](#basic-sdk-initialization)
6. [Implementing IMU Data Reading](#implementing-imu-data-reading)
7. [Building and Running on a Device](#building-and-running-on-a-device)
8. [Troubleshooting](#troubleshooting)

## Development Environment Setup

### Required Software

- **Android Studio**: Version 4.2 or higher (latest version recommended)
- **JDK**: Version 11 or higher
- **Android SDK**: API level 28 (Android 9) or higher

### Hardware Requirements

- **Android Device**: Running Android 9 (API 28) or newer
- **VITURE XR Glasses**: Pro, One, or Lite models
- **USB-C Cable**: For connecting the glasses to your Android device

## Creating a New Android Project

1. Open Android Studio and select **File > New > New Project**
2. Select **Empty Activity** as the project template and click **Next**
3. Configure your project with the following settings:
   - **Name**: Choose a project name (e.g., "VitureDemo")
   - **Package name**: Use your own domain in reverse notation (e.g., "com.example.vituredemo")
   - **Language**: Java or Kotlin (examples in this guide use Java)
   - **Minimum SDK**: API 28 (Android 9.0) or higher

## Adding the VITURE SDK

### 1. Download the SDK

Download the latest VITURE SDK AAR file from the official source or extract it from the example project.

### 2. Add the AAR to Your Project

1. Create a `libs` folder in your app module if it doesn't exist
2. Copy the VITURE-SDK-1.0.7.aar file (or the latest version) into the `libs` folder

### 3. Configure Gradle

Open your app-level `build.gradle` file and add the following:

```gradle
android {
    // Other configurations...
    
    // Enable viewBinding if you want to use it
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // Other dependencies...
    
    // Add the VITURE SDK
    implementation fileTree(dir: 'libs', includes: ['*.jar', '*.aar'])
}
```

4. Sync your project with Gradle by clicking **Sync Now**

## USB Permissions Configuration

### 1. Add USB Permissions to AndroidManifest.xml

Open your `AndroidManifest.xml` file and add the following permissions:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="your.package.name">

    <!-- USB permissions -->
    <uses-feature android:name="android.hardware.usb.host" />
    
    <application
        ...>
        
        <!-- Your activities and other components -->
        
    </application>
</manifest>
```

### 2. Create a USB Device Filter

Create a new XML resource file in `res/xml/device_filter.xml` with the following content:

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

## Basic SDK Initialization

Create a basic activity to initialize the SDK and handle its lifecycle:

### 1. MainActivity.java

```java
package com.example.vituredemo;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

import com.viture.sdk.ArCallback;
import com.viture.sdk.ArManager;
import com.viture.sdk.Constants;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "VitureDemo";
    
    private TextView statusTextView;
    private TextView imuDataTextView;
    private Switch imuSwitch;
    private Switch mode3DSwitch;
    
    private ArManager arManager;
    private ArCallback arCallback;
    private int sdkInitResult = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        statusTextView = findViewById(R.id.status_text);
        imuDataTextView = findViewById(R.id.imu_data);
        imuSwitch = findViewById(R.id.switch_imu);
        mode3DSwitch = findViewById(R.id.switch_3d);
        
        // Set up UI listeners
        imuSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (sdkInitResult == Constants.ERROR_INIT_SUCCESS) {
                    arManager.setImuOn(isChecked);
                }
            }
        });
        
        mode3DSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (sdkInitResult == Constants.ERROR_INIT_SUCCESS) {
                    arManager.set3D(isChecked);
                }
            }
        });
        
        // Initialize the SDK
        initVitureSDK();
    }
    
    private void initVitureSDK() {
        // Get ArManager instance
        arManager = ArManager.getInstance(this);
        
        // Create ArCallback for handling events and IMU data
        arCallback = new ArCallback() {
            @Override
            public void onEvent(int eventId, byte[] event, long timestamp) {
                Log.d(TAG, "Event received: " + eventId);
                
                if (eventId == Constants.EVENT_ID_INIT) {
                    // Parse the initialization result
                    sdkInitResult = byteArrayToInt(event, 0, event.length);
                    
                    // Update UI on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateInitStatus();
                        }
                    });
                }
            }
            
            @Override
            public void onImu(long timestamp, byte[] imuData) {
                // Process IMU data
                if (imuData.length >= 12) {
                    ByteBuffer buffer = ByteBuffer.wrap(imuData);
                    
                    // Extract Euler angles (in radians)
                    final float roll = buffer.getFloat(0);   // Roll (rotation around front axis)
                    final float pitch = buffer.getFloat(4);  // Pitch (rotation around right axis)
                    final float yaw = buffer.getFloat(8);    // Yaw (rotation around up axis)
                    
                    // Update UI on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateImuData(roll, pitch, yaw);
                        }
                    });
                }
            }
        };
        
        // Register the callback
        arManager.registerCallback(arCallback);
        
        // Initialize the SDK
        sdkInitResult = arManager.init();
        updateInitStatus();
    }
    
    private void updateInitStatus() {
        String status;
        
        switch (sdkInitResult) {
            case Constants.ERROR_INIT_SUCCESS:
                status = "Initialized successfully";
                // Update switch states
                int imuState = arManager.getImuState();
                int mode3DState = arManager.get3DState();
                imuSwitch.setChecked(imuState == Constants.STATE_ON);
                mode3DSwitch.setChecked(mode3DState == Constants.STATE_ON);
                break;
            case Constants.ERROR_INIT_NO_DEVICE:
                status = "No device found. Please connect your VITURE glasses.";
                break;
            case Constants.ERROR_INIT_NO_PERMISSION:
                status = "USB permission denied. Please grant permission.";
                break;
            default:
                status = "Initialization failed with error code: " + sdkInitResult;
                break;
        }
        
        statusTextView.setText(status);
    }
    
    private void updateImuData(float roll, float pitch, float yaw) {
        // Convert radians to degrees for display
        float rollDeg = (float) Math.toDegrees(roll);
        float pitchDeg = (float) Math.toDegrees(pitch);
        float yawDeg = (float) Math.toDegrees(yaw);
        
        String imuText = String.format("Roll: %.2f°\nPitch: %.2f°\nYaw: %.2f°", 
                                      rollDeg, pitchDeg, yawDeg);
        imuDataTextView.setText(imuText);
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
        
        // Re-register callback if needed
        if (arManager != null && arCallback != null) {
            arManager.registerCallback(arCallback);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister callback to prevent memory leaks
        if (arManager != null && arCallback != null) {
            arManager.unregisterCallback(arCallback);
        }
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
}
```

### 2. activity_main.xml

Create a simple layout for your activity:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    tools:context=".MainActivity">

    <TextView
        android:id="@+id/status_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:text="Initializing..."
        android:textSize="16sp"
        android:textStyle="bold" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:layout_marginBottom="24dp">

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

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="8dp"
        android:text="IMU Data:"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/imu_data"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="No data"
        android:layout_marginBottom="24dp"
        android:textSize="16sp" />

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
        android:text="1. Connect your VITURE glasses to your Android device\n2. Toggle the IMU switch to start receiving motion data\n3. Toggle the 3D switch to enable/disable 3D mode" />

</LinearLayout>
```

## Implementing IMU Data Reading

The code above already includes basic IMU data reading in the `onImu` callback method. The data comes as a byte array which we parse into Euler angles (roll, pitch, and yaw).

### Understanding the Coordinate System

The VITURE XR Glasses use the following coordinate system:
- Roll: Rotation around the front axis
- Pitch: Rotation around the right axis
- Yaw: Rotation around the up axis

For more advanced applications, you can also access quaternion data when available:

```java
// In the onImu method, if imuData length is 36 or more bytes
if (imuData.length >= 36) {
    float quaternionW = buffer.getFloat(20);
    float quaternionX = buffer.getFloat(24);
    float quaternionY = buffer.getFloat(28);
    float quaternionZ = buffer.getFloat(32);
    
    // Use quaternion data for more precise rotations
}
```

## Building and Running on a Device

### 1. Connect Your Android Device

1. Connect your Android device to your computer via USB
2. Enable USB debugging on your device (Settings > Developer options > USB debugging)
3. Allow your computer to access your device when prompted

### 2. Connect VITURE XR Glasses

1. Connect your VITURE XR Glasses to your Android device using a USB-C cable
2. If prompted, allow the app to access the USB device

### 3. Build and Run

1. In Android Studio, select your device from the dropdown menu in the toolbar
2. Click the "Run" button (green triangle) to build and install the app
3. The app should launch on your device and begin initializing the SDK
4. Grant any permissions requested by the app
5. Once initialized, you should see "Initialized successfully" in the status text
6. Toggle the IMU switch to start receiving motion data
7. Move your head while wearing the glasses to see the IMU data change

## Troubleshooting

### Common Issues

1. **SDK initialization fails with "No device found"**
   - Ensure your VITURE glasses are properly connected to your Android device
   - Try disconnecting and reconnecting the glasses
   - Check that the USB cable is working properly

2. **SDK initialization fails with "USB permission denied"**
   - Make sure to grant USB permissions when prompted
   - Check that your `AndroidManifest.xml` includes the USB host feature
   - Verify that your `device_filter.xml` file includes the correct vendor and product IDs

3. **No IMU data appears**
   - Ensure the IMU switch is toggled on
   - Check that the SDK was initialized successfully
   - Verify that the glasses are properly connected and powered on

4. **3D mode does not change**
   - Some content may not support 3D mode
   - Ensure the glasses firmware is up to date
   - Verify that the SDK was initialized successfully

### Logging

To help diagnose issues, you can enable SDK logging:

```java
// After getting the ArManager instance
arManager.setLogOn(true);
```

Then check the logs in Android Studio's Logcat with the tag "VitureDemo" or filter for SDK-related messages.

## Next Steps

Once you have successfully set up the basic application and confirmed it's receiving IMU data from the VITURE XR Glasses, you can explore more advanced features:

- Implementing head tracking for UI elements
- Creating immersive 3D experiences
- Optimizing performance for different content types
- Implementing custom gesture recognition based on head movements

Refer to the other documentation guides for more detailed information on each feature set.
