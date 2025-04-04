package com.eden.demo.sensor.components;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.eden.demo.sensor.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Component for displaying a minimap in the top-right corner of the glasses UI
 */
public class MiniMapComponent implements GlassesComponent, OnMapReadyCallback {
    private final Context mContext;
    private final FragmentManager mFragmentManager;
    private final LayoutInflater mInflater;
    
    // Views
    private View mRootView3D;
    private View mRootView2D;
    
    // Map components
    private MapFragment mMapFragment3D;
    private MapFragment mMapFragment2D;
    private GoogleMap mMap3D;
    private GoogleMap mMap2D;
    
    // State
    private boolean mIs3DMode = true;
    private boolean mIsVisible = true;
    private boolean mIsInitialized = false;
    
    // Default location (can be updated with actual location)
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.7749, -122.4194); // San Francisco
    
    public MiniMapComponent(Context context, FragmentManager fragmentManager) {
        mContext = context;
        mFragmentManager = fragmentManager;
        mInflater = LayoutInflater.from(context);
    }
    
    @Override
    public void initialize() {
        // Initialize views
        mRootView3D = mInflater.inflate(R.layout.component_minimap_3d, null);
        mRootView2D = mInflater.inflate(R.layout.component_minimap_2d, null);
        
        // We'll initialize map fragments when the views are attached to the window
        mIsInitialized = true;
        
        // Update visibility based on current state
        updateVisibility();
    }
    
    /**
     * Initialize map fragments once the views are attached to the window
     */
    private void initializeMapFragments() {
        // Only initialize once the views are attached to the window
        if (mRootView3D.isAttachedToWindow() && mMapFragment3D == null) {
            mMapFragment3D = (MapFragment) mFragmentManager.findFragmentById(R.id.map_fragment_3d);
            if (mMapFragment3D != null) {
                mMapFragment3D.getMapAsync(this);
            }
        }
        
        if (mRootView2D.isAttachedToWindow() && mMapFragment2D == null) {
            mMapFragment2D = (MapFragment) mFragmentManager.findFragmentById(R.id.map_fragment_2d);
            if (mMapFragment2D != null) {
                mMapFragment2D.getMapAsync(this);
            }
        }
    }
    
    @Override
    public void setDisplayMode(boolean is3DMode) {
        mIs3DMode = is3DMode;
        updateVisibility();
    }
    
    @Override
    public void setVisibility(boolean visible) {
        mIsVisible = visible;
        updateVisibility();
    }
    
    private void updateVisibility() {
        if (!mIsInitialized) return;
        
        if (mRootView3D != null) {
            mRootView3D.setVisibility(
                mIsVisible && mIs3DMode ? View.VISIBLE : View.GONE);
        }
        
        if (mRootView2D != null) {
            mRootView2D.setVisibility(
                mIsVisible && !mIs3DMode ? View.VISIBLE : View.GONE);
        }
        
        // Initialize map fragments if needed and visible
        if (mIsVisible) {
            initializeMapFragments();
        }
    }
    
    @Override
    public void update() {
        // Maps update automatically
    }
    
    @Override
    public View get3DView() {
        return mRootView3D;
    }
    
    @Override
    public View get2DView() {
        return mRootView2D;
    }
    
    @Override
    public void cleanup() {
        // Maps will be cleaned up by the fragment manager
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap == null) return;
        
        // Determine which map was initialized
        MapFragment callingFragment = null;
        try {
            callingFragment = (MapFragment) mFragmentManager.findFragmentById(
                googleMap == mMap3D ? R.id.map_fragment_3d : R.id.map_fragment_2d);
        } catch (Exception e) {
            // Handle exception
        }
        
        if (callingFragment == mMapFragment3D) {
            mMap3D = googleMap;
            configureMap(mMap3D);
        } else if (callingFragment == mMapFragment2D) {
            mMap2D = googleMap;
            configureMap(mMap2D);
        } else {
            // Try to determine by checking if 3D map is null
            if (mMap3D == null) {
                mMap3D = googleMap;
                configureMap(mMap3D);
            } else {
                mMap2D = googleMap;
                configureMap(mMap2D);
            }
        }
    }
    
    /**
     * Configure map settings for both 2D and 3D modes
     */
    private void configureMap(GoogleMap map) {
        if (map == null) return;
        
        try {
            // Try to apply a dark map style to match the cyberpunk theme
            boolean success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(mContext, R.raw.map_style_dark));
            
            if (!success) {
                // If style fails, fall back to normal style
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        } catch (Exception e) {
            // If there's an error loading the style, use default
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        
        // Disable UI controls (this is just a minimap)
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setAllGesturesEnabled(false);
        uiSettings.setCompassEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        
        // Set initial position and zoom level
        CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(DEFAULT_LOCATION)
            .zoom(15)  // Street level zoom
            .tilt(45)  // Tilted view for video game style
            .build();
            
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        
        // Add a marker for current position
        map.addMarker(new MarkerOptions()
            .position(DEFAULT_LOCATION)
            .title("Current Position")
            .alpha(0.7f)); // Semi-transparent
    }
    
    /**
     * Update the map location (can be called from outside to update position)
     * 
     * @param location The new location to display
     */
    public void updateLocation(LatLng location) {
        if (mMap3D != null) {
            mMap3D.clear();
            mMap3D.addMarker(new MarkerOptions()
                .position(location)
                .title("Current Position")
                .alpha(0.7f));
                
            mMap3D.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
        
        if (mMap2D != null) {
            mMap2D.clear();
            mMap2D.addMarker(new MarkerOptions()
                .position(location)
                .title("Current Position")
                .alpha(0.7f));
                
            mMap2D.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
    }
}
