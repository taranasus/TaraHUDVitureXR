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

The application uses a client-server architecture pattern within the app:

1. **GlassesDisplayService** (Server)
   - Runs as a foreground service with a persistent notification
   - Manages VITURE SDK initialization and lifecycle
   - Handles display connection and presentation
   - Controls the HUD content (2D/3D modes, visibility)
   - Continues to run in the background when other apps are in focus

2. **FullscreenActivity** (Client)
   - Provides the user interface for controlling the HUD
   - Binds to the GlassesDisplayService using Android's service binding mechanism
   - Sends commands to the service based on user interaction
   - Updates UI to reflect the current state of the service

This separation allows the HUD to remain active even when the activity is destroyed or other apps are in the foreground.

## Next dev tasks:
- **Screen-Off Display Persistence**: Implement wake lock functionality to keep the HUD visible on the glasses even when the phone screen is turned off, allowing for extended use without needing to keep the phone screen on (requires WAKE_LOCK permission and will increase battery consumption)

## Next Testing Task:
- Test background service implementation: Verify that the glasses HUD continues to work when the app is in the background and a different app is open. The HUD should remain visible on the glasses even when the phone is showing other applications.

## Next human tasks:
- Design an official HUD layout to then figure out what features to build into the glassess

## Feature Ideas:
- Display Date and Time
- Main UI interface that's always up HUD for regular outside usage
- Various different UIs for specialized needs with a main home interface (Just like the menu system in a game)
- Make interface look like Cyberpunk 2077 interface
![Example 1](https://kagi.com/proxy/01-HUD_res-1920x1080.jpg?c=r3ruTN54uRUfKZ7YQWAyRjrcWRhLwbKbHxR-z9yws1vBMoyguZdt02IJ_DYtUKGHZ-LtcvlFORMi4p4yTKXBIvm_BSb4rpHkbTn7XBjOeAziuDqXFJjVldjTFfXFJPlS)
![Example 2](https://kagi.com/proxy/cyberpunk-2077-inventory.png?c=r3ruTN54uRUfKZ7YQWAyRjrcWRhLwbKbHxR-z9yws1uB_4i6PVbzr6ks6OEhVQjOHEeevQkGFDxu_CXxQlR9j6CXEZ0iMgF07OQ5Tcf9YkbUO0eeZW23OP1Esh-dpnEMFieNCfGsWdSjhaPahY7voA%3D%3D)
![Example 3](https://kagi.com/proxy/Cyberpunk-207712292020-013429-95236.jpg?c=Wm3gB90_xO0KDyFYSPobHLotF6fiM7Cgw5qArYgphVg2VIQvgm8tyurnj5qk29uuLvwSwosK_H-oCpkCvQ3b7Prnk9jNYcangX1zMSIbX8qytgNVJczleUJxhzjYA0gk)
![Example 4](https://kagi.com/proxy/omg7z3u0e3p91.jpg?c=MHaoEHf4JA4T1dYEo1CR0X0TUe2ouvSbn8yjRBD1I_nC9ho-4N4vcnXNlOXXk3q9J45pfeetiT5ugwGR9vm_pvbhpHMDb08-TlkMtfqRU4p9HVI_baJZN8l4eE0RJzT9TXUkqMoHZbRFb7ynNgrGoqaFR8YFnhu3Uan0GiGU4C5_KGNIb8JZk5-fc_7fvK0g)
![Example 5](https://kagi.com/proxy/vo82brtkyk491.jpg?c=TklOzPjLPioJ5YMJT75bSoRpDc5CNyG1ip-t0-zqb3GpJjA69-hJwXUeCbIcFHEI)
- Small to-do list widget with like the top 2-3 items of the day remaining to look like mission objectives
- Make a new display mode where, when set to 3D mode, the images for the two eyes are siwtched around (left image to right eye, right image to left eye)
- Pump the phone cameras feeds into the glassess to see what the phone sees
- Real-time audio to text conversion that gets displayed to the user inside the glassess as it happens
- Real-time sending of all text to a central storage location for later processing by an AI
- Ability to take a picture of what the camera feeds on the phon are showing to upload to the central AI location for storage
- Always on AI voice operation that allows the user to control the glassess and their actions by simply speaking them
- Tight AI integration with the glasses and phone features so the AI can operate the phone independently
- Video feed of the phone's main screen to the glassess within the glassess HUD UI so the user can see what's on the phone screen
- Integrate with AI agents that can operate the phone to do actions like navigate to apps and stuff.
- Music widget inside the glassess with waveform cause I like waveforms
- Notifications display
- A little Bluetooth controller with some buttons that fits in one hand in order to control the UI without getting the phone out and simulate that Videogame experience. Needs at least 6 buttson (up,down,left,right,confirm,cancel)

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

## ON HOLD
- Make it automatically switch the glasses to be transparent (not opaque) when the app starts - Not possible in current version of SDK (1.0.7) but dev team said they're working on it so might be in the next version when it drops. On hold for now.
