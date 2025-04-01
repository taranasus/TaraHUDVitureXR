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

## Next dev tasks:
- Make it automatically switch the glasses to be transparent (not opaque) when the app starts

## Next human tasks:
- Design an official HUG layout to then figure out what features to build into the glassess
- Figure out some cool features to add to them

## Feature Ideas:
- Make glassess HUD also work when the app is in the background and a different app is open, I want this thing to be able to run and pump info to the glassess at all times.
- Pump the phone cameras feeds into the glassess to see what the phone sees
- Real-time audio to text conversion that gets displayed to the user inside the glassess as it happens
- Real-time sending of all text to a central storage location for later processing by an AI
- Ability to take a picture of what the camera feeds on the phon are showing to upload to the central AI location for storage
- Always on AI voice operation that allows the user to control the glassess and their actions by simply speaking them
- Tight AI integration with the glasses and phone features so the AI can operate the phone independently
- Video feed of the phone's main screen to the glassess within the glassess HUD UI so the user can see what's on the phone screen
- Music widget inside the glassess with waveform cause I like waveforms
- Notifications display
