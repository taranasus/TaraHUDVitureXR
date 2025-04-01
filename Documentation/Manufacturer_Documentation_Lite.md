# VITURE One Android SDK Guide

## Introduction

The VITURE One SDK for Android offers a set of APIs designed to enable the VITURE One XR Glasses' head tracking features. This SDK empowers developers to seamlessly access IMU (Inertial Measurement Unit) data.

Download the SDK and view release history.

| Version | Release notes | Release date |
|---------|--------------|--------------|
| V1.0.1 | • Able to access and get the IMU data.<br>• Switch between 3D and 2D Mode of the XR Glasses. | |
| V1.0.5 | Supports other models of VITURE GLASSES including Lite | 2024-02-22 |
| V1.0.7 | Support VITURE Pro | 2024-05-13 |

## Key functionalities and features include:

1. **USB Initialization & Device Access:**
   - Enables initialization of the USB library to access and interact with the XR glasses.

2. **IMU Data Handling:**
   - Facilitates the reception and handling of Inertial Measurement Unit (IMU) data.
   - Allows control over IMU event reporting, enabling or pausing IMU data stream.

3. **Event Callbacks:**
   - Supports callback mechanisms to receive events and changes from the XR glasses.
   - Callbacks for IMU data and other event changes provide flexibility in handling device responses.

4. **Device Control:**
   - Provides methods to control the XR glasses states, such as switching resolutions (e.g., 3840x1080 or 1920x1080).

5. **Error Handling:**
   - Defines error codes and error states for various command executions, allowing developers to handle potential issues gracefully.

6. **Resource Management:**
   - Includes functions to release resources, ensuring proper cleanup post-usage.

Overall, the SDK empowers developers to initialize, control, and interact with the XR glasses, managing IMU data, device states, and event callbacks effectively while handling potential errors.

## Compatibility

Supports Android 8 and newer versions.

## Quickstart

A typical sequence might resemble the following:

1. Initialize the 'usblib' and set up the IMU callback.
2. Control the IMU data reading thread by enabling or pausing it and utilize the IMU data
3. Release the USB resources

The (0,0,0) coordinates of Glasses are shown in the following picture:

![VITURE Glasses Coordinates](Screenshot%202025-03-12%20at%2014.28.04.jpg)

## Integrating using Gradle

Add the VITURE SDK AAR to the 'libs' folder of your project and include the dependency:

```gradle
dependencies {
    implementation files('libs/VITURE-SDK-1.0.1.aar')
}
```

## Initializing

```java
import com.viture.sdk.ArCallback;
import com.viture.sdk.ArManager;
import com.viture.sdk.Constants;
```

```kotlin
fun init() {
    ArManager mArManager = ArManager.getInstance(context);
    //Register the ArCallback
    mArManager.registerCallback(mCallback);
    //This API initializes the native SDK and requests USB permission.
    //If mSdkInitSuccess is 0, you can retrieve IMU data or control the XR Glasses.
    //Otherwise, there might not be connected devices or insufficient USB permission.
    int mSdkInitSuccess = mArManager.init();
}

ArCallback mCallback = new ArCallback() {
    @Override
    public void onEvent(int eventId, byte[] event, long l) {
        //The initialization state may be reported by the onEvent function.
        //event: (little-endian)
        if (eventId == Constants.EVENT_ID_INIT) {
            mSdkInitSuccess = byteArrayToInt(event, 0, event.length);
        }
    }
    
    @Override
    public void onImu(long ts, byte[] imu) {
        int len = imu.length;
        ByteBuffer byteBuffer = ByteBuffer.wrap(imu);
        //Euler  (big-endian)
        float eulerRoll = byteBuffer.getFloat(0); //roll --> front-axis
        float eulerPitch = byteBuffer.getFloat(4);// pitch -> right-axis
        float eulerYaw = byteBuffer.getFloat(8);// yaw --> up-axis
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                move(eulerYaw, eulerPitch);
            }
        });
        
        //Quaternions (Firmwire requires up to 01.0.02.007_20231212)
        if (len >= 36) {
            float quaternionW = byteBuffer.getFloat(20);
            float quaternionX = byteBuffer.getFloat(24);
            float quaternionY = byteBuffer.getFloat(28);
            float quaternionZ = byteBuffer.getFloat(32);
        }
    }
};
```

## API reference

### ArManager

Main class to interact with the XR glasses

```java
/**
 * Initializes the native USB environment.
 * @return ERROR_INIT_* based on init status.
 */
public int init() {}

/**
 * Release the native environment.
 */
public void release() {}

/**
 * Initializes the native USB environment.
 * @return ERROR_INIT_* based on init status.
 */
public void registerCallback(ArCallback callback) {}

/**
 * Release the listener.
 * @param callback The callback to unregister.
 */
public void unregisterCallback(ArCallback callback) {}

/**
 * Gets the IMU reporting state.
 * @return 1 if IMU data reporting is on, 0 for off, <0 for error (STATE_*).
 */
public int getImuState() {}

/**
 * Gets the resolution state of the XR Glasses.
 * @return 1 for 3840*1080, 0 for 1920*1080, <0 for error (STATE_*).
 */
public int get3DState() {}

/**
 * Retrieves the current IMU report frequency.
 * @return Current report frequency (IMU_FREQUENCE_*).
 */
public int getCurImuFrequency() {}

/**
 * Switches the resolution of the XR Glasses.
 * @param on true: 3840*1080  false: 1920*1080.
 * @return Error code (ERR_SET_*).
 */
public int set3D(boolean on) {}

/**
 * Control the IMU report frequency.
 * @param mode 0x00 for 60Hz, 0x01 for 90Hz, 0x02 for 120Hz, 0x03 for 240Hz.
 * @return Error code (ERR_SET_*).
 */
public int setImuFrequency(int mode) {}

/**
 * Opens or pauses the IMU event reporting.
 * @param on true to enable the IMU data reporting.
 * @return Error code (ERR_SET_*).
 */
public int setImuOn(boolean on) {}
```

## Error Codes and Constants for APIs

```java
public class Constants {
    //Status of XR Glasses for get* APIs.
    public static final int STATE_ON = 1;
    public static final int STATE_OFF = 0;
    public static final int STATE_ERR_WRITE_FAIL = -1;
    public static final int STATE_ERR_RSP_ERROR = -2;
    public static final int STATE_ERR_TIMEOUT = -3;
    
    //Event IDs
    public static final int EVENT_ID_INIT = 0;
    public static final int EVENT_ID_BRIGHTNESS = 0x0301;
    public static final int EVENT_ID_3D = 0x0302;
    public static final int EVENT_ID_VOICE = 0x0304;
    
    //INIT RESULT
    public static final int ERROR_INIT_SUCCESS = 0;
    public static final int ERROR_INIT_NO_DEVICE = -1;
    public static final int ERROR_INIT_NO_PERMISSION = -2;
    public static final int ERROR_INIT_UNKOWN = -3;
    
    //Error Codes for set* APIs.
    public static final int ERR_SET_SUCCESS = 0;
    public static final int ERR_SET_FAILURE = 1;
    public static final int ERR_SET_INVALID_ARGUMENT = 2;
    public static final int ERR_SET_NOT_ENOUGH_MEMORY = 3;
    public static final int ERR_SET_UNSUPPORTED_CMD = 4;
    public static final int ERR_SET_CRC_MISMATCH = 5;
    public static final int ERR_SET_VER_MISMATCH = 6;
    public static final int ERR_SET_MSG_ID_MISMATCH = 7;
    public static final int ERR_SET_MSG_STX_MISMATCH = 8;
    public static final int ERR_SET_CODE_NOT_WRITTEN = 9;
    
    //IMU Frequency List
    public static final int IMU_FREQUENCE_60  = 0;
    public static final int IMU_FREQUENCE_90  = 1;
    public static final int IMU_FREQUENCE_120 = 2;
    public static final int IMU_FREQUENCE_240 = 3;
}
```

## IMU Data structures

This data consists of Big Endian 32-bit floats and is returned by the onImu function within the ArCallback interface.

```java
public abstract class ArCallback {
    /**
     * Callback for event changes from the XR Glasses, such as 3D/2D switch event.
     * @param msgid EVENT ID (EVENT_ID_*).
     * @param event Event data (little-endian)
     * @param ts Timestamp
     */
    public void onEvent(int msgid, byte[] event, long ts) {
    }
    
    /**
     * Callback for IMU Data (big-endian)
     * @param ts Timestamp
     * @param imu The IMU data: the first 12 bytes represent Euler values,
     *       Format:
     *       - uint8_t eulerRoll[4];
     *       - uint8_t eulerPitch[4];
     *       - uint8_t eulerYaw[4];
     *       (Each value is a 32-bit float in big-endian format)
     */
    public void onImu(long ts, byte[] imu) {
        // Implement IMU data handling code here.
    }
}
