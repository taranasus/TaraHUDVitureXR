package com.eden.demo.sensor.hud;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.eden.demo.sensor.R;

/**
 * Class responsible for creating and updating segment bars in the HUD
 */
public class SegmentBarsManager {
    private static final String TAG = "SegmentBarsManager";
    
    // Bar segments constants
    private static final int MAX_SEGMENT_BARS = 10;
    
    private Context mContext;
    private Drawable mSignalSegmentDrawable;
    private Drawable mSegmentItemDrawable;
    
    /**
     * Constructor
     * 
     * @param context Application context
     */
    public SegmentBarsManager(Context context) {
        mContext = context;
        
        // Initialize drawables
        mSignalSegmentDrawable = context.getDrawable(R.drawable.signal_bar_item);
        mSegmentItemDrawable = context.getDrawable(R.drawable.segment_bar_item);
    }
    
    /**
     * Create signal strength bar segments
     * 
     * @param container LinearLayout container for the signal bars
     */
    public void createSignalBars(LinearLayout container) {
        if (container == null) {
            Log.e(TAG, "Signal bar container is null");
            return;
        }
        
        container.removeAllViews();
        
        // No spacing at the beginning to align with bars above
        View spacer = new View(mContext);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(spacer, spacerParams);
        
        // Create individual signal segments with small gaps between them
        for (int i = 0; i < HudConstants.MAX_SIGNAL_BARS; i++) {
            View segment = new View(mContext);
            segment.setBackground(mSignalSegmentDrawable.getConstantState().newDrawable());
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    5, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMarginEnd(2);
            
            container.addView(segment, params);
        }
    }
    
    /**
     * Create segment bar items
     * 
     * @param container LinearLayout container for the segment bars
     */
    public void createSegmentBars(LinearLayout container) {
        if (container == null) {
            Log.e(TAG, "Segment bar container is null");
            return;
        }
        
        container.removeAllViews();
        
        // Add spacing at the beginning
        View spacer = new View(mContext);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                5, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(spacer, spacerParams);
        
        // Create individual segments with gaps between them
        for (int i = 0; i < MAX_SEGMENT_BARS; i++) {
            View segment = new View(mContext);
            segment.setBackground(mSegmentItemDrawable.getConstantState().newDrawable());
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    8, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMarginEnd(3);
            
            container.addView(segment, params);
        }
        
        // Set 50% of segments visible by default
        updateSegmentBars(container, MAX_SEGMENT_BARS / 2);
    }
    
    /**
     * Update the segment bars visibility
     * 
     * @param container LinearLayout container for the segment bars
     * @param visibleCount Number of segments to make visible
     */
    public void updateSegmentBars(LinearLayout container, int visibleCount) {
        if (container == null) {
            Log.e(TAG, "Segment bar container is null");
            return;
        }
        
        if (container.getChildCount() <= 1) {
            Log.e(TAG, "Segment bar container has too few children: " + container.getChildCount());
            return;
        }
        
        // Skip the first child which is the spacer
        for (int i = 1; i < container.getChildCount(); i++) {
            View segment = container.getChildAt(i);
            
            // Show segments up to the visible count
            if (i - 1 < visibleCount) {
                segment.setVisibility(View.VISIBLE);
            } else {
                segment.setVisibility(View.INVISIBLE);
            }
        }
    }
    
    /**
     * Update the signal strength bars
     * 
     * @param container LinearLayout container for the signal bars
     * @param signalLevel Signal level (0 to HudConstants.MAX_SIGNAL_BARS)
     */
    public void updateSignalBars(LinearLayout container, int signalLevel) {
        if (container == null) {
            Log.e(TAG, "Signal bar container is null");
            return;
        }
        
        if (container.getChildCount() <= 1) {
            Log.e(TAG, "Signal bar container has too few children: " + container.getChildCount());
            return;
        }
        
        Log.d(TAG, "Updating signal bars in container with " + container.getChildCount() + 
              " children, signal level: " + signalLevel);
        
        int visibleCount = 0;
        
        // Skip the first child which is the spacer
        for (int i = 1; i < container.getChildCount(); i++) {
            View segment = container.getChildAt(i);
            
            // Activate segments up to the signal level
            // (i - 1 because the first segment is index 1 after spacer)
            if (i - 1 < signalLevel) {
                segment.setVisibility(View.VISIBLE);
                visibleCount++;
            } else {
                segment.setVisibility(View.INVISIBLE);
            }
        }
        
        Log.d(TAG, "Signal bars updated: " + visibleCount + " segments visible out of " + 
              (container.getChildCount() - 1));
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
     * Get the maximum number of segment bars
     * 
     * @return Maximum number of segment bars
     */
    public int getMaxSegmentBars() {
        return MAX_SEGMENT_BARS;
    }
}
