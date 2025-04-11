package com.eden.demo.sensor;

import android.app.Application;
import android.os.Build;
import android.util.Log;

/**
 * Custom Application class for TaraHUD VirtuoXR
 * 
 * This class serves two main purposes:
 * 1. Disables Android Studio startup agents that can cause crashes on certain devices
 *    (see StartupAgentHelper.java for detailed explanation of the issue)
 * 2. Initializes app-wide components
 * 
 * The static initializer block ensures that startup agents are disabled as early as
 * possible in the app lifecycle, before any other code runs. This is critical for
 * preventing crashes on devices with fs-verity or other security mechanisms that
 * conflict with the instrumentation agents.
 */
public class TaraHUDApplication extends Application {
    private static final String TAG = "TaraHUDApp";

    // Static initialization block to disable startup agents as early as possible
    static {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Try to disable startup agents before any other code runs using reflection
                Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
                java.lang.reflect.Method getServiceMethod = activityManagerClass.getDeclaredMethod("getService");
                Object activityManagerService = getServiceMethod.invoke(null);
                
                java.lang.reflect.Method setStartupAgentsEnabledMethod = 
                    activityManagerService.getClass().getDeclaredMethod("setStartupAgentsEnabled", boolean.class);
                setStartupAgentsEnabledMethod.invoke(activityManagerService, false);
            }
        } catch (Exception e) {
            // Can't log here yet, but we'll try again in onCreate
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Log device information for debugging
        StartupAgentHelper.logDeviceInfo();
        
        // Disable startup agents that can cause crashes
        boolean agentsDisabled = StartupAgentHelper.disableStartupAgents();
        Log.i(TAG, "Startup agents disabled: " + agentsDisabled);
        
        // If this is a problematic device, log additional warning
        if (StartupAgentHelper.isProblematicDevice()) {
            Log.w(TAG, "Running on a device that may have issues with startup agents");
        }
        
        // Initialize other app-wide components here
        Log.i(TAG, "TaraHUD Application initialized");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory condition detected");
        // Handle low memory condition if needed
    }
}
