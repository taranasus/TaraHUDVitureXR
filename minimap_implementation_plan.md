# Minimap Implementation Plan for TaraHUDVirtuoXR

This document provides a detailed implementation plan for adding a minimap to the TaraHUDVirtuoXR HUD. The minimap will be displayed in the top-right corner of the HUD and will show the user's current location at its center.

## 1. Project Overview

TaraHUDVirtuoXR is an Android application that uses the VITURE XR Glasses SDK to create a HUD display. The project has a modular architecture with several components:

- **GlassesDisplayService**: Coordinates between specialized manager components
- **VitureSDKManager**: Handles SDK initialization and lifecycle
- **DisplayPresentationManager**: Manages external display detection
- **GlassesPresentation**: Implements the UI displayed on the glasses
- **FullscreenActivity**: Provides the user interface for controlling the HUD

The HUD currently displays various elements in the top-left corner (time, battery, signal strength). The task is to add a minimap in the top-right corner that shows the user's current location.

## 2. Dependencies and Requirements

### 2.1 Add Google Maps SDK Dependencies

Add the following dependencies to the app's `build.gradle` file:

```gradle
implementation 'com.google.android.gms:play-services-maps:18.1.0'
implementation 'com.google.android.gms:play-services-location:21.0.1'
```

### 2.2 Google Maps API Key

1. Create a Google Cloud Platform project
2. Enable Maps SDK for Android
3. Generate an API key with appropriate restrictions
4. Add the API key to the project

### 2.3 Update AndroidManifest.xml

Add the following to the `AndroidManifest.xml` file:
- Internet permission
- Fine and coarse location permissions
- Google Maps API key metadata tag in the application element

## 3. Implementation Steps

### 3.1 Create MinimapManager Class

Create a new class called `MinimapManager` in the `com.eden.demo.sensor.hud` package to handle the minimap functionality. This class should:

1. Implement `OnMapReadyCallback` interface
2. Manage MapView instances for both 2D and 3D modes
3. Handle location updates using FusedLocationProviderClient
4. Configure map appearance and settings
5. Update the map with the user's current location
6. Provide methods to handle lifecycle events (resume, pause, destroy, lowMemory)
7. Include methods to toggle between 2D and 3D display modes

Key components to implement:
- Constructor that initializes location services
- Methods to set and initialize map views
- Map configuration (style, UI controls, initial position)
- Location update handling
- Map marker management
- Display mode switching
- Lifecycle management

### 3.2 Create Map Style Resource

Create a new file `map_style_dark.json` in the `app/src/main/res/raw/` directory (create the directory if it doesn't exist). This JSON file should define a dark map style that's suitable for a HUD display, with:
- Dark background colors
- Minimal labels
- Visible roads and landmarks
- Reduced visual clutter

### 3.3 Update Layout Files

#### 3.3.1 Update `glasses_display.xml`

Modify the `app/src/main/res/layout/glasses_display.xml` file to add the minimap views to both 2D and 3D layouts:

1. Add a MapView for 3D mode:
   - ID: `minimap_3d`
   - Position: top-right corner
   - Size: 200dp x 200dp
   - Visibility: visible when in 3D mode

2. Add a MapView for 2D mode:
   - ID: `minimap_2d`
   - Position: top-right corner
   - Size: 200dp x 200dp
   - Visibility: visible when in 2D mode

### 3.4 Update GlassesPresentation Class

Modify the `GlassesPresentation.java` file to initialize and manage the minimap:

1. Add necessary imports for Maps and location
2. Add fields for the minimap views and manager
3. Update `initializeUIReferences()` to include the minimap views
4. Create a new method `initializeMinimap()` to:
   - Create the MinimapManager
   - Set the map views
   - Check and handle location permissions
5. Update `onCreate()` to call `initializeMinimap()`
6. Update `setDisplayMode()` to handle minimap visibility
7. Update `dismiss()` to clean up minimap resources
8. Add or update lifecycle methods to handle minimap lifecycle events

### 3.5 Update FullscreenActivity for Location Permissions

Modify the `FullscreenActivity.java` file to handle location permission requests:

1. Add code to request location permissions at runtime
2. Handle permission request results
3. Communicate permission status to the GlassesPresentation

### 3.6 Testing

Test the implementation on a device with the VITURE XR Glasses:

1. Verify the minimap appears in the correct position in both 2D and 3D modes
2. Verify the minimap shows the user's current location
3. Verify the minimap updates as the user moves
4. Verify the minimap is properly styled and visible
5. Verify the minimap doesn't interfere with other HUD elements

## 4. Implementation Notes

- The minimap should be visible but not distracting
- The map style should be dark to match the HUD aesthetic
- The user's location should be clearly marked at the center
- The minimap should work in both 2D and 3D modes
- The implementation should handle cases where location permissions are denied
- The minimap should properly handle lifecycle events to avoid memory leaks
