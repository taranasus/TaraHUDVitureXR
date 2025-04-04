package com.eden.demo.sensor.components;

import android.view.View;

/**
 * Interface for modular UI components in the glasses display
 */
public interface GlassesComponent {
    /**
     * Called when the component should initialize itself
     */
    void initialize();
    
    /**
     * Called when the display mode changes between 2D and 3D
     * @param is3DMode True if in 3D mode, false if in 2D mode
     */
    void setDisplayMode(boolean is3DMode);
    
    /**
     * Set the visibility of this component
     * @param visible True to show the component, false to hide it
     */
    void setVisibility(boolean visible);
    
    /**
     * Update the component's state
     */
    void update();
    
    /**
     * Clean up resources when the component is no longer needed
     */
    void cleanup();
    
    /**
     * Get the root view for 3D mode (right eye)
     */
    View get3DView();
    
    /**
     * Get the root view for 2D mode
     */
    View get2DView();
}
