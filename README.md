# TaraHUDVirtuoXR

A custom implementation using the VITURE XR Glasses SDK to create a minimal HUD display with phone controls.

## Overview

This project demonstrates a dual-display Heads-Up Display (HUD) implementation for the VITURE XR Glasses. It showcases the following features:

- Separate phone and glasses displays
- Toggle between 2D and 3D modes
- Show/hide green "ONLINE" status indicator
- True fullscreen immersive experience
- Portrait mode for phone controls

## Implementation Details

The app showcases several key capabilities of the VITURE XR Glasses SDK:

1. **Display Mode Control**: 
   - Toggle between 2D mode (1920×1080) and 3D mode (3840×1080)
   - In 3D mode, the green "ONLINE" box is visible only in the right eye
   - In 2D mode, the green "ONLINE" box is visible in both eyes

2. **Dual Display Management**:
   - Phone UI provides control buttons and status information
   - Glasses display shows the HUD elements
   - DisplayManager API detects when glasses are connected
   - Presentation API shows different content on the glasses

3. **Visual Elements**:
   - Black background on glasses for minimal visual interference
   - Green-bordered box with "ONLINE" text
   - Phone status updates and control buttons

4. **Background Service**:
   - Persistent foreground service maintains the glasses display even when the app is in background
   - HUD remains visible when using other apps or when the phone screen is locked
   - Notification provides easy access back to the control UI

## Building and Running

This project is designed to be built and run using Android Studio:

1. Open the project in Android Studio
2. Connect your Android device with the VITURE XR Glasses attached
3. Build and run the app on your device
4. The phone will show the control UI, while the glasses will show the HUD

## Phone Controls

The phone UI provides the following controls:

1. "Display Green Box" - Shows the green "ONLINE" indicator on the glasses
2. "Hide Green Box" - Hides the green "ONLINE" indicator on the glasses
3. "3D Mode" toggle - Switches between 2D and 3D display modes
4. Status text - Shows information about connections and current state

## SDK Documentation

For more comprehensive information about the VITURE XR Glasses SDK, please refer to the documentation guides:

1. [Getting Started](Documentation/getting-started.md)
2. [IMU Data Processing](Documentation/feature-imu-data-processing.md)
3. [Display Mode Control](Documentation/feature-display-mode-control.md)
4. [Device Management & Lifecycle](Documentation/feature-device-management.md)
5. [Event Callbacks](Documentation/feature-event-callbacks.md)

## Supported Devices

- VITURE Pro XR Glasses
- VITURE One XR Glasses
- VITURE Lite XR Glasses

## Requirements

- Android 8.0 (API level 28) or higher
- USB-C connection to the VITURE XR Glasses

## Additional Resources

- **ExampleProjectAndDocumentation.zip**: This file contains an example project that demonstrates the capabilities of the Viture XR SDK. It's based on the project found at [Viture's official website](https://first.viture.com/developer/viture-sdk-for-android) (version 1.0.7) but has been modified to ensure it builds and runs correctly at the time of writing. This can be useful as a reference for understanding the SDK features beyond what's implemented in this project.

## Development Workflow

When making changes to this project, please follow these steps:

1. Make all necessary code changes and test them thoroughly
2. Update the README.md file as the final step to document any new features or changes
3. Use Git to track and commit your changes:
   ```
   git add .
   git commit -m "Brief description of changes"
   git push
   ```

**Important Note:** Always update the README.md last, just before running the Git commands to commit and push your changes.

## Architecture Overview

The application uses a client-server architecture pattern within the app, with a modular separation of responsibilities:

1. **GlassesDisplayService** (Server Coordinator)
   - Runs as a foreground service with a persistent notification
   - Coordinates between specialized manager components
   - Provides a clean public API for the Activity to use
   - Continues to run in the background when other apps are in focus

2. **VitureSDKManager** (SDK Interface)
   - Handles all VITURE SDK initialization and lifecycle
   - Manages SDK callbacks and events
   - Controls 3D mode settings
   - Provides an event listener interface for state changes

3. **DisplayPresentationManager** (Display Handler)
   - Manages external display detection and connection events
   - Handles presentation creation and lifecycle
   - Controls what's shown on the glasses display
   - Provides an event listener interface for display changes

4. **GlassesPresentation** (UI Component)
   - Implements the actual UI displayed on the glasses
   - Handles immersive mode and fullscreen settings
   - Controls visibility of UI elements based on display mode

5. **FullscreenActivity** (Client)
   - Provides the user interface for controlling the HUD
   - Binds to the GlassesDisplayService using Android's service binding mechanism
   - Sends commands to the service based on user interaction
   - Updates UI to reflect the current state of the service

This modular separation allows for better maintainability and a clear separation of concerns within the codebase, while still enabling the HUD to remain active even when the activity is destroyed or other apps are in the foreground.


## Development Notes

### Background Service Implementation
The background service uses Android's `Service` class with the `startForeground()` method to keep the service running even when the app is in the background. This approach requires:

1. The `FOREGROUND_SERVICE` permission in AndroidManifest.xml
2. Creating a notification channel (for Android 8.0+)
3. Displaying a persistent notification while the service is running
4. Proper lifecycle management between Activity and Service

### Testing
When testing background functionality:
1. Launch the app and ensure the HUD is visible on the glasses
2. Press the home button or switch to another app
3. The HUD should remain visible on the glasses
4. Tap the persistent notification to return to the control UI

### Known Limitations
- The service will be killed if the app is force-stopped by the user or system
- Some devices with aggressive battery optimization may still stop the service
- For complete persistence, consider enabling "Ignore battery optimizations" for the app (requires additional permissions)

