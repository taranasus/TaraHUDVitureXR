package com.eden.demo.sensor;

import android.app.Presentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.eden.demo.sensor.components.ComponentManager;
import com.eden.demo.sensor.components.HealthStatsComponent;
import com.eden.demo.sensor.components.MiniMapComponent;
import com.eden.demo.sensor.databinding.GlassesDisplayBinding;

/**
 * Custom Presentation class for displaying content on the XR glasses with Cyberpunk styled HUD
 * Uses a component-based architecture for modularity and maintainability
 */
public class GlassesPresentation extends Presentation {
    // Layout bindings
    private GlassesDisplayBinding mGlassesBinding;
    private boolean mCurrentlyIn3DMode = true;
    private boolean mHudVisible = true;
    
    // Component management
    private ComponentManager mComponentManager;
    private HealthStatsComponent mHealthStatsComponent;
    private MiniMapComponent mMiniMapComponent;
    
    public GlassesPresentation(Context context, Display display) {
        super(context, display);
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
        
        // Enable immersive mode for the presentation
        enablePresentationImmersiveMode();
        
        // Initialize component manager
        mComponentManager = new ComponentManager(
            getContext(),
            mGlassesBinding.container3d,
            mGlassesBinding.container2d
        );
        
        // Initialize and add components
        initializeComponents();
        
        // Set initial display mode
        setDisplayMode(true); // Default to 3D mode
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeComponents() {
        // Health stats component (top-left)
        mHealthStatsComponent = new HealthStatsComponent(getContext());
        mComponentManager.addComponent(mHealthStatsComponent);
        
        // Minimap component (top-right)
        mMiniMapComponent = new MiniMapComponent(getContext(), getFragmentManager());
        mComponentManager.addComponent(mMiniMapComponent);
    }
    
    /**
     * Set the display mode (2D or 3D)
     */
    public void setDisplayMode(boolean is3DMode) {
        mCurrentlyIn3DMode = is3DMode;
        
        if (mComponentManager != null) {
            mComponentManager.setDisplayMode(is3DMode);
        }
    }
    
    /**
     * Set the visibility of the HUD display
     */
    public void setGreenBoxVisibility(boolean visible) {
        mHudVisible = visible;
        
        if (mComponentManager != null) {
            mComponentManager.setVisibility(visible);
        }
    }
    
    /**
     * Check if the HUD is currently visible
     * 
     * @return True if the HUD is visible in the current display mode
     */
    public boolean isGreenBoxVisible() {
        return mHudVisible;
    }
    
    /**
     * Clean up resources when presentation is dismissed
     */
    @Override
    public void dismiss() {
        if (mComponentManager != null) {
            mComponentManager.cleanup();
        }
        
        super.dismiss();
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
