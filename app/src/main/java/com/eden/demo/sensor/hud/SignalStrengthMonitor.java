package com.eden.demo.sensor.hud;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Class responsible for monitoring phone signal strength
 */
public class SignalStrengthMonitor {
    private static final String TAG = "SignalMonitor";
    
    // Phone state monitoring
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private int mSignalStrength = 0;
    private Context mContext;
    
    // Callback interface for signal strength changes
    public interface SignalStrengthListener {
        void onSignalStrengthChanged(int signalStrength, int maxBars);
    }
    
    private SignalStrengthListener mListener;
    
    /**
     * Constructor
     * 
     * @param context Application context
     * @param listener Listener for signal strength changes
     */
    public SignalStrengthMonitor(Context context, SignalStrengthListener listener) {
        mContext = context;
        mListener = listener;
    }
    
    /**
     * Initialize monitoring of signal strength
     */
    public void initialize() {
        Log.d(TAG, "Initializing signal strength monitoring");
        
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        
        if (mTelephonyManager != null) {
            Log.d(TAG, "TelephonyManager obtained successfully");
            
            // Try to get current signal strength directly first
            updateSignalStrengthFromCellInfo();
            
            // Set up listener for signal strength changes
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    // Default to a low-medium level in case we can't determine signal strength
                    int level = 2; 
                    int dBm = -999; // Invalid value to indicate not set
                    String signalMethod = "default";
                    
                    if (signalStrength != null) {
                        Log.d(TAG, "Signal strength changed: " + signalStrength.toString());
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // On Android 10+, use getCellSignalStrengths method
                            try {
                                if (signalStrength.getCellSignalStrengths() != null && 
                                    !signalStrength.getCellSignalStrengths().isEmpty()) {
                                    
                                    Log.d(TAG, "Cell signal strengths: " + signalStrength.getCellSignalStrengths().toString());
                                    
                                    // Get the primary signal strength
                                    level = signalStrength.getCellSignalStrengths().get(0).getLevel();
                                    signalMethod = "getCellSignalStrengths().getLevel()";
                                    Log.d(TAG, "Signal level from primary cell: " + level);
                                    
                                    // Try to get a more accurate reading using dBm values if available
                                    try {
                                        dBm = signalStrength.getCellSignalStrengths().get(0).getDbm();
                                        Log.d(TAG, "Signal dBm from primary cell: " + dBm);
                                        
                                        // Convert dBm to level (typically -120 to -50 dBm range)
                                        if (dBm != Integer.MAX_VALUE) {
                                            // Map dBm range to 0-4 level
                                            // -120 or lower = 0, -50 or higher = 4
                                            int dBmLevel = Math.max(0, Math.min(4, (dBm + 120) / 14));
                                            Log.d(TAG, "Signal level calculated from dBm: " + dBmLevel + " (from dBm: " + dBm + ")");
                                            level = dBmLevel;
                                            signalMethod = "dBm calculation";
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error getting dBm value: " + e.getMessage());
                                        // If we can't get dBm, stick with the level we already have
                                    }
                                } else {
                                    Log.d(TAG, "Cell signal strengths list is null or empty");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error accessing cell signal strengths: " + e.getMessage());
                                // Fallback to getLevel() if getCellSignalStrengths() fails
                                try {
                                    level = signalStrength.getLevel();
                                    signalMethod = "signalStrength.getLevel()";
                                    Log.d(TAG, "Signal level from getLevel() fallback: " + level);
                                } catch (Exception e2) {
                                    Log.e(TAG, "Error getting signal level: " + e2.getMessage());
                                    // Keep default level if both methods fail
                                }
                            }
                        } else {
                            // Older versions, use getLevel() method
                            try {
                                level = signalStrength.getLevel();
                                signalMethod = "signalStrength.getLevel() (older Android)";
                                Log.d(TAG, "Signal level from getLevel() (older Android): " + level);
                            } catch (Exception e) {
                                Log.e(TAG, "Error getting signal level on older Android: " + e.getMessage());
                                // Keep default level if getLevel() fails
                            }
                        }
                    } else {
                        Log.d(TAG, "Signal strength is null, using default level: " + level);
                    }
                    
                    // Ensure level is within 0-4 range
                    int originalLevel = level;
                    level = Math.max(0, Math.min(4, level));
                    if (originalLevel != level) {
                        Log.d(TAG, "Signal level clamped from " + originalLevel + " to " + level);
                    }
                    
                    // Scale from 0-4 to 0-15 range for our display
                    int oldSignalStrength = mSignalStrength;
                    mSignalStrength = (level * HudConstants.MAX_SIGNAL_BARS) / 4;
                    
                    Log.d(TAG, "Final signal values - Method: " + signalMethod + 
                               ", Level (0-4): " + level + 
                               ", dBm: " + (dBm != -999 ? dBm : "N/A") + 
                               ", Display bars (0-" + HudConstants.MAX_SIGNAL_BARS + "): " + mSignalStrength + 
                               ", Changed from: " + oldSignalStrength);
                    
                    // Notify listener of signal strength change
                    if (mListener != null) {
                        mListener.onSignalStrengthChanged(mSignalStrength, HudConstants.MAX_SIGNAL_BARS);
                    }
                }
            };
            
            // Start listening for signal strength changes
            startMonitoring();
        } else {
            Log.e(TAG, "TelephonyManager is null");
            // If TelephonyManager is not available, use default
            mSignalStrength = HudConstants.MAX_SIGNAL_BARS / 2;
            Log.d(TAG, "Using default signal strength (no TelephonyManager): " + mSignalStrength);
            
            // Notify listener of default signal strength
            if (mListener != null) {
                mListener.onSignalStrengthChanged(mSignalStrength, HudConstants.MAX_SIGNAL_BARS);
            }
        }
    }
    
    /**
     * Start monitoring signal strength
     */
    public void startMonitoring() {
        if (mTelephonyManager != null && mPhoneStateListener != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ requires explicit permission check
                    try {
                        Log.d(TAG, "Registering phone state listener (Android 12+)");
                        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception registering listener: " + e.getMessage());
                        // If permission is not granted, try to get current signal strength once
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                Log.d(TAG, "Attempting one-time signal strength check");
                                SignalStrength signalStrength = mTelephonyManager.getSignalStrength();
                                if (signalStrength != null) {
                                    Log.d(TAG, "Got one-time signal strength: " + signalStrength);
                                    mPhoneStateListener.onSignalStrengthsChanged(signalStrength);
                                } else {
                                    Log.d(TAG, "One-time signal strength check returned null");
                                }
                            }
                        } catch (Exception ignored) {
                            Log.e(TAG, "Error in one-time signal check: " + ignored.getMessage());
                            // If we can't get signal strength, use default
                            mSignalStrength = HudConstants.MAX_SIGNAL_BARS / 2;
                            Log.d(TAG, "Using default signal strength: " + mSignalStrength);
                            
                            // Notify listener of default signal strength
                            if (mListener != null) {
                                mListener.onSignalStrengthChanged(mSignalStrength, HudConstants.MAX_SIGNAL_BARS);
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Registering phone state listener (pre-Android 12)");
                    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error registering phone state listener: " + e.getMessage());
                // If we can't listen for signal strength changes, use default
                mSignalStrength = HudConstants.MAX_SIGNAL_BARS / 2;
                Log.d(TAG, "Using default signal strength due to error: " + mSignalStrength);
                
                // Notify listener of default signal strength
                if (mListener != null) {
                    mListener.onSignalStrengthChanged(mSignalStrength, HudConstants.MAX_SIGNAL_BARS);
                }
            }
        }
    }
    
    /**
     * Stop monitoring signal strength
     */
    public void stopMonitoring() {
        if (mTelephonyManager != null && mPhoneStateListener != null) {
            try {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                Log.d(TAG, "Stopped signal strength monitoring");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping signal monitoring: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reinitialize signal strength monitoring
     * This is called when the READ_PHONE_STATE permission is granted
     */
    public void reinitialize() {
        Log.d(TAG, "Reinitializing signal strength monitoring");
        
        // Stop any existing monitoring
        stopMonitoring();
        
        // Restart monitoring with new permissions
        initialize();
        
        // Try to get the current signal strength directly
        updateSignalStrengthFromCellInfo();
    }
    
    /**
     * Get the current signal strength
     * 
     * @return Current signal strength (0 to HudConstants.MAX_SIGNAL_BARS)
     */
    public int getSignalStrength() {
        return mSignalStrength;
    }
    
    /**
     * Get the maximum number of signal bars
     * 
     * @return Maximum number of signal bars
     */
    public int getMaxSignalBars() {
        return HudConstants.MAX_SIGNAL_BARS;
    }
    
    /**
     * Update signal strength using getAllCellInfo() API
     * This is the modern approach to get signal strength directly
     */
    private void updateSignalStrengthFromCellInfo() {
        if (mTelephonyManager == null) {
            Log.e(TAG, "TelephonyManager is null, cannot update signal strength");
            return;
        }
        
        try {
            // This requires ACCESS_FINE_LOCATION permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.d(TAG, "Attempting to get signal strength from getAllCellInfo()");
                
                java.util.List<android.telephony.CellInfo> cellInfoList = mTelephonyManager.getAllCellInfo();
                
                if (cellInfoList != null && !cellInfoList.isEmpty()) {
                    Log.d(TAG, "Got cell info list with " + cellInfoList.size() + " items");
                    
                    // Default level (0-4)
                    int level = 0;
                    int dBm = -999;
                    String cellType = "unknown";
                    
                    // Find the first active cell with signal strength info
                    for (android.telephony.CellInfo cellInfo : cellInfoList) {
                        if (cellInfo.isRegistered()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Android 10+ (API 29+)
                                if (cellInfo instanceof android.telephony.CellInfoLte) {
                                    android.telephony.CellSignalStrengthLte signalStrengthLte = 
                                        ((android.telephony.CellInfoLte) cellInfo).getCellSignalStrength();
                                    level = signalStrengthLte.getLevel();
                                    dBm = signalStrengthLte.getDbm();
                                    cellType = "LTE";
                                    break;
                                } else if (cellInfo instanceof android.telephony.CellInfoGsm) {
                                    android.telephony.CellSignalStrengthGsm signalStrengthGsm = 
                                        ((android.telephony.CellInfoGsm) cellInfo).getCellSignalStrength();
                                    level = signalStrengthGsm.getLevel();
                                    dBm = signalStrengthGsm.getDbm();
                                    cellType = "GSM";
                                    break;
                                } else if (cellInfo instanceof android.telephony.CellInfoWcdma) {
                                    android.telephony.CellSignalStrengthWcdma signalStrengthWcdma = 
                                        ((android.telephony.CellInfoWcdma) cellInfo).getCellSignalStrength();
                                    level = signalStrengthWcdma.getLevel();
                                    dBm = signalStrengthWcdma.getDbm();
                                    cellType = "WCDMA";
                                    break;
                                } else if (cellInfo instanceof android.telephony.CellInfoCdma) {
                                    android.telephony.CellSignalStrengthCdma signalStrengthCdma = 
                                        ((android.telephony.CellInfoCdma) cellInfo).getCellSignalStrength();
                                    level = signalStrengthCdma.getLevel();
                                    dBm = signalStrengthCdma.getDbm();
                                    cellType = "CDMA";
                                    break;
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                                           cellInfo instanceof android.telephony.CellInfoNr) {
                                    // 5G NR (New Radio)
                                    android.telephony.CellSignalStrength signalStrength = 
                                        ((android.telephony.CellInfoNr) cellInfo).getCellSignalStrength();
                                    level = signalStrength.getLevel();
                                    dBm = signalStrength.getDbm();
                                    cellType = "5G NR";
                                    break;
                                }
                            } else {
                                // Older Android versions
                                if (cellInfo instanceof android.telephony.CellInfoLte) {
                                    android.telephony.CellSignalStrengthLte signalStrengthLte = 
                                        ((android.telephony.CellInfoLte) cellInfo).getCellSignalStrength();
                                    level = signalStrengthLte.getLevel();
                                    dBm = signalStrengthLte.getDbm();
                                    cellType = "LTE";
                                    break;
                                } else if (cellInfo instanceof android.telephony.CellInfoGsm) {
                                    android.telephony.CellSignalStrengthGsm signalStrengthGsm = 
                                        ((android.telephony.CellInfoGsm) cellInfo).getCellSignalStrength();
                                    level = signalStrengthGsm.getLevel();
                                    dBm = signalStrengthGsm.getDbm();
                                    cellType = "GSM";
                                    break;
                                } else if (cellInfo instanceof android.telephony.CellInfoWcdma) {
                                    android.telephony.CellSignalStrengthWcdma signalStrengthWcdma = 
                                        ((android.telephony.CellInfoWcdma) cellInfo).getCellSignalStrength();
                                    level = signalStrengthWcdma.getLevel();
                                    dBm = signalStrengthWcdma.getDbm();
                                    cellType = "WCDMA";
                                    break;
                                } else if (cellInfo instanceof android.telephony.CellInfoCdma) {
                                    android.telephony.CellSignalStrengthCdma signalStrengthCdma = 
                                        ((android.telephony.CellInfoCdma) cellInfo).getCellSignalStrength();
                                    level = signalStrengthCdma.getLevel();
                                    dBm = signalStrengthCdma.getDbm();
                                    cellType = "CDMA";
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (dBm != -999) {
                        Log.d(TAG, "Got signal strength from " + cellType + " cell: level=" + level + 
                                   ", dBm=" + dBm);
                        
                        // Ensure level is within 0-4 range
                        level = Math.max(0, Math.min(4, level));
                        
                        // Scale from 0-4 to 0-15 range for our display
                        mSignalStrength = (level * HudConstants.MAX_SIGNAL_BARS) / 4;
                        
                        Log.d(TAG, "Signal strength updated from cell info: " + mSignalStrength + 
                                   " out of " + HudConstants.MAX_SIGNAL_BARS);
                        
                        // Notify listener of signal strength
                        if (mListener != null) {
                            mListener.onSignalStrengthChanged(mSignalStrength, HudConstants.MAX_SIGNAL_BARS);
                        }
                        
                        return; // Successfully updated
                    } else {
                        Log.d(TAG, "Could not get dBm value from cell info");
                    }
                } else {
                    Log.d(TAG, "Cell info list is null or empty");
                }
            } else {
                Log.d(TAG, "getAllCellInfo() not available on this Android version");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting cell info: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting cell info: " + e.getMessage());
        }
        
        // If we couldn't get signal strength from cell info, try the legacy method
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                SignalStrength signalStrength = mTelephonyManager.getSignalStrength();
                if (signalStrength != null) {
                    Log.d(TAG, "Using getSignalStrength() fallback");
                    mPhoneStateListener.onSignalStrengthsChanged(signalStrength);
                    return; // Successfully updated
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting signal strength: " + e.getMessage());
        }
        
        // If all else fails, we'll rely on the PhoneStateListener to update the signal strength
        Log.d(TAG, "Could not get signal strength directly, will rely on PhoneStateListener");
    }
    
    /**
     * Release resources
     */
    public void release() {
        stopMonitoring();
        mListener = null;
    }
}
