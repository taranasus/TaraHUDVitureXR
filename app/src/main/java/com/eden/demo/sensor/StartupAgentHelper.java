package com.eden.demo.sensor;

import android.os.Build;
import android.util.Log;

/**
 * Helper class to manage Android Studio startup agents that can cause crashes
 * on certain Android devices, particularly those running Android 12+ or custom ROMs.
 * 
 * This class addresses the following error patterns seen in logs:
 * 1. fs-verity errors: "Failed to measure fs-verity, errno 1: /data/app/..."
 * 2. Startup agent crashes: "Fatal signal 11 (SIGSEGV)" in "code_cache/startup_agents/..."
 * 3. Memory access errors: References to "re-initialized>" in crash logs
 * 
 * These crashes are caused by Android Studio's instrumentation agents that are injected
 * into the app during development. These agents help with debugging and profiling but
 * can cause issues on certain devices due to filesystem security features, memory
 * protection mechanisms, and vendor-specific Android implementations.
 * 
 * No manual setup is required - this solution works automatically.
 */
public class StartupAgentHelper {
    private static final String TAG = "StartupAgentHelper";
    
    /**
     * Disables Android Studio startup agents to prevent crashes
     * This should be called as early as possible in the application lifecycle
     * 
     * @return true if agents were successfully disabled, false otherwise
     */
    public static boolean disableStartupAgents() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Disable startup agents via reflection
                Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
                java.lang.reflect.Method getServiceMethod = activityManagerClass.getDeclaredMethod("getService");
                Object activityManagerService = getServiceMethod.invoke(null);
                
                java.lang.reflect.Method setStartupAgentsEnabledMethod = 
                    activityManagerService.getClass().getDeclaredMethod("setStartupAgentsEnabled", boolean.class);
                setStartupAgentsEnabledMethod.invoke(activityManagerService, false);
                
                Log.i(TAG, "Successfully disabled startup agents");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to disable startup agents", e);
            }
        } else {
            Log.d(TAG, "Startup agent disabling not needed on Android < P");
        }
        return false;
    }
    
    /**
     * Checks if the device is likely to have issues with startup agents
     * This is based on known problematic device/OS combinations
     * 
     * @return true if the device is likely to have issues
     */
    public static boolean isProblematicDevice() {
        // Check for Android 12 or higher
        boolean isAndroid12OrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        
        // Check for known problematic device manufacturers
        boolean isProblematicManufacturer = false;
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("xiaomi") || 
            manufacturer.contains("samsung") || 
            manufacturer.contains("oppo") || 
            manufacturer.contains("vivo") ||
            manufacturer.contains("oneplus")) {
            isProblematicManufacturer = true;
        }
        
        // Check for custom ROM indicators
        boolean isLikelyCustomRom = false;
        String fingerprint = Build.FINGERPRINT.toLowerCase();
        if (fingerprint.contains("lineage") || 
            fingerprint.contains("cyanogen") || 
            fingerprint.contains("custom") ||
            fingerprint.contains("graphene") ||
            fingerprint.contains("pixel") ||
            fingerprint.contains("miui")) {
            isLikelyCustomRom = true;
        }
        
        return isAndroid12OrHigher && (isProblematicManufacturer || isLikelyCustomRom);
    }
    
    /**
     * Logs diagnostic information about the device
     * This can be helpful for debugging startup agent issues
     */
    public static void logDeviceInfo() {
        Log.d(TAG, "Device Information:");
        Log.d(TAG, "Manufacturer: " + Build.MANUFACTURER);
        Log.d(TAG, "Model: " + Build.MODEL);
        Log.d(TAG, "Android Version: " + Build.VERSION.RELEASE);
        Log.d(TAG, "SDK Level: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Build Fingerprint: " + Build.FINGERPRINT);
        Log.d(TAG, "Is Problematic Device: " + isProblematicDevice());
    }
}
