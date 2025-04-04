package com.eden.demo.sensor;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.eden.demo.sensor.databinding.GlassesDisplayBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Custom Presentation class for displaying content on the XR glasses
 */
public class GlassesPresentation extends Presentation {
    private TextView mGlassesRightEyeText;
    private TextView mGlasses2DText;
    private GlassesDisplayBinding mGlassesBinding;
    private boolean mCurrentlyIn3DMode = true;
    private Handler mTimeUpdateHandler;
    private SimpleDateFormat mDateTimeFormat;
    
    public GlassesPresentation(Context context, Display display) {
        super(context, display);
        mDateTimeFormat = new SimpleDateFormat("MMM dd, yyyy\nHH:mm:ss", Locale.getDefault());
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Request full screen for the presentation
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Inflate the glasses layout
        mGlassesBinding = GlassesDisplayBinding.inflate(getLayoutInflater());
        setContentView(mGlassesBinding.getRoot());
        
        // Get references to the green box text views
        mGlassesRightEyeText = mGlassesBinding.glassesRightEyeText; // For 3D mode
        mGlasses2DText = mGlassesBinding.glasses2dText; // For 2D mode
        
        // Apply the custom Rajdhani font to both text views
        Typeface rajdhaniTypeface = ResourcesCompat.getFont(getContext(), R.font.rajdhani_medium);
        if (rajdhaniTypeface != null) {
            mGlassesRightEyeText.setTypeface(rajdhaniTypeface);
            mGlasses2DText.setTypeface(rajdhaniTypeface);
        }
        
        // Set initial display mode
        setDisplayMode(true); // Default to 3D mode
        
        // Enable immersive mode for the presentation
        enablePresentationImmersiveMode();
        
        // Start the timer to update date and time
        startTimeUpdates();
    }
    
    /**
     * Start periodic updates for time display
     */
    private void startTimeUpdates() {
        mTimeUpdateHandler = new Handler();
        updateDateTime(); // Update immediately
        
        // Schedule periodic updates
        mTimeUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                // Schedule next update in 1 second
                mTimeUpdateHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    /**
     * Stop time updates when presentation is dismissed
     */
    @Override
    public void dismiss() {
        if (mTimeUpdateHandler != null) {
            mTimeUpdateHandler.removeCallbacksAndMessages(null);
        }
        super.dismiss();
    }
    
    /**
     * Update the date and time text in both text views
     */
    private void updateDateTime() {
        String currentDateTime = mDateTimeFormat.format(new Date());
        
        if (mGlassesRightEyeText != null) {
            mGlassesRightEyeText.setText(currentDateTime);
        }
        
        if (mGlasses2DText != null) {
            mGlasses2DText.setText(currentDateTime);
        }
    }
    
    /**
     * Set the display mode (2D or 3D)
     */
    public void setDisplayMode(boolean is3DMode) {
        mCurrentlyIn3DMode = is3DMode;
        
        if (is3DMode) {
            // In 3D mode: 3840x1080, only right text visible
            if (mGlassesRightEyeText != null) {
                mGlassesRightEyeText.setVisibility(View.VISIBLE);
            }
            if (mGlasses2DText != null) {
                mGlasses2DText.setVisibility(View.GONE);
            }
        } else {
            // In 2D mode: 1920x1080, both eyes see the same text
            if (mGlassesRightEyeText != null) {
                mGlassesRightEyeText.setVisibility(View.GONE);
            }
            if (mGlasses2DText != null) {
                mGlasses2DText.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Set the visibility of the green box
     */
    public void setGreenBoxVisibility(boolean visible) {
        // Update visibility based on current mode
        if (mCurrentlyIn3DMode) {
            if (mGlassesRightEyeText != null) {
                mGlassesRightEyeText.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        } else {
            if (mGlasses2DText != null) {
                mGlasses2DText.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    /**
     * Check if the green box is currently visible
     * 
     * @return True if the green box is visible in the current display mode
     */
    public boolean isGreenBoxVisible() {
        if (mCurrentlyIn3DMode) {
            return mGlassesRightEyeText != null && 
                   mGlassesRightEyeText.getVisibility() == View.VISIBLE;
        } else {
            return mGlasses2DText != null && 
                   mGlasses2DText.getVisibility() == View.VISIBLE;
        }
    }
    
    /**
     * Enable immersive mode for the presentation
     */
    private void enablePresentationImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | 
                                WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // For Android 10 and below
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
            View decorView = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(flags);
        }
    }
}
