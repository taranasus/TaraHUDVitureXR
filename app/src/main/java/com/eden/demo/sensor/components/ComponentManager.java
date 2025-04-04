package com.eden.demo.sensor.components;

import android.content.Context;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all UI components in the glasses display
 */
public class ComponentManager {
    private final Context mContext;
    private final ViewGroup mContainer3D;
    private final ViewGroup mContainer2D;
    private final List<GlassesComponent> mComponents = new ArrayList<>();
    private boolean mIs3DMode = true;
    
    public ComponentManager(Context context, ViewGroup container3D, ViewGroup container2D) {
        mContext = context;
        mContainer3D = container3D;
        mContainer2D = container2D;
    }
    
    /**
     * Add a component to be managed
     */
    public void addComponent(GlassesComponent component) {
        if (!mComponents.contains(component)) {
            mComponents.add(component);
            component.initialize();
            component.setDisplayMode(mIs3DMode);
            
            // Add component views to containers
            if (component.get3DView() != null) {
                mContainer3D.addView(component.get3DView());
            }
            
            if (component.get2DView() != null) {
                mContainer2D.addView(component.get2DView());
            }
        }
    }
    
    /**
     * Update all components
     */
    public void updateComponents() {
        for (GlassesComponent component : mComponents) {
            component.update();
        }
    }
    
    /**
     * Set display mode for all components
     */
    public void setDisplayMode(boolean is3DMode) {
        mIs3DMode = is3DMode;
        
        // Update display mode for all components
        for (GlassesComponent component : mComponents) {
            component.setDisplayMode(is3DMode);
        }
        
        // Update container visibility
        mContainer3D.setVisibility(is3DMode ? ViewGroup.VISIBLE : ViewGroup.GONE);
        mContainer2D.setVisibility(is3DMode ? ViewGroup.GONE : ViewGroup.VISIBLE);
    }
    
    /**
     * Set visibility for all components
     */
    public void setVisibility(boolean visible) {
        for (GlassesComponent component : mComponents) {
            component.setVisibility(visible);
        }
    }
    
    /**
     * Clean up all components
     */
    public void cleanup() {
        for (GlassesComponent component : mComponents) {
            component.cleanup();
        }
        mComponents.clear();
        
        // Remove all views from containers
        mContainer3D.removeAllViews();
        mContainer2D.removeAllViews();
    }
}
