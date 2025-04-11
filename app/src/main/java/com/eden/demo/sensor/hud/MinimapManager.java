package com.eden.demo.sensor.hud;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.eden.demo.sensor.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Manages the minimap functionality for the HUD display
 */
public class MinimapManager implements OnMapReadyCallback {
    
    private static final String TAG = "MinimapManager";
    
    // Default location (will be updated when actual location is available)
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.7749, -122.4194); // San Francisco
    private static final float DEFAULT_ZOOM = 15f;
    
    // Location update interval in milliseconds
    private static final long LOCATION_UPDATE_INTERVAL = 5000;
    private static final long LOCATION_UPDATE_FASTEST_INTERVAL = 2000;
    
    // Context
    private final Context mContext;
    
    // Map views for 2D and 3D modes
    private MapView mMapView2D;
    private MapView mMapView3D;
    
    // Google Map instance
    private GoogleMap mMap;
    
    // Location services
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    
    // Current location and marker
    private Location mCurrentLocation;
    private Marker mCurrentLocationMarker;
    
    // Flag to track if map is ready
    private boolean mIsMapReady = false;
    
    /**
     * Constructor
     * 
     * @param context Application context
     */
    public MinimapManager(Context context) {
        mContext = context;
        
        // Initialize location services
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        
        // Create location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update current location
                    mCurrentLocation = location;
                    
                    // Update map with new location
                    updateMapLocation();
                }
            }
        };
    }
    
    /**
     * Set the map views for both 2D and 3D modes
     * 
     * @param mapView2D MapView for 2D mode
     * @param mapView3D MapView for 3D mode
     */
    public void setMapViews(MapView mapView2D, MapView mapView3D) {
        mMapView2D = mapView2D;
        mMapView3D = mapView3D;
        
        // Always initialize the 2D map view
        mMapView2D.getMapAsync(this);
    }
    
    /**
     * Called when the map is ready to be used
     * 
     * @param googleMap The GoogleMap instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Map is ready");
        mMap = googleMap;
        mIsMapReady = true;
        
        // Configure map settings
        configureMap();
        
        // Start location updates
        startLocationUpdates();
        
        // Set initial map location
        updateMapLocation();
    }
    
    /**
     * Configure map appearance and settings
     */
    private void configureMap() {
        if (mMap == null) return;
        
        try {
            // Apply custom map style
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(mContext, R.raw.map_style_dark));
            
            if (!success) {
                Log.e(TAG, "Style parsing failed");
            }
            
            // Configure map UI settings
            mMap.getUiSettings().setCompassEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setScrollGesturesEnabled(false);
            mMap.getUiSettings().setTiltGesturesEnabled(false);
            mMap.getUiSettings().setZoomGesturesEnabled(false);
            
            // Set map type
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }
    
    /**
     * Start location updates
     */
    public void startLocationUpdates() {
        try {
            // Create location request
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                    .setMinUpdateIntervalMillis(LOCATION_UPDATE_FASTEST_INTERVAL)
                    .build();
            
            // Request location updates
            mFusedLocationClient.requestLocationUpdates(
                    locationRequest, mLocationCallback, Looper.getMainLooper());
            
            // Get last known location
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    mCurrentLocation = location;
                    updateMapLocation();
                }
            });
            
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }
    
    /**
     * Stop location updates
     */
    public void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    
    /**
     * Update the map with the current location
     */
    private void updateMapLocation() {
        if (!mIsMapReady || mMap == null) return;
        
        LatLng position;
        
        // Use current location if available, otherwise use default
        if (mCurrentLocation != null) {
            position = new LatLng(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
        } else {
            position = DEFAULT_LOCATION;
        }
        
        // Update camera position
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)
                .zoom(DEFAULT_ZOOM)
                .build();
        
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        
        // Update or add marker for current location
        if (mCurrentLocationMarker == null) {
            // Create new marker
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .icon(getBitmapDescriptorFromVector(R.drawable.location_marker));
            
            mCurrentLocationMarker = mMap.addMarker(markerOptions);
        } else {
            // Update existing marker
            mCurrentLocationMarker.setPosition(position);
        }
    }
    
    /**
     * Convert a vector drawable to a BitmapDescriptor for use as a marker icon
     * 
     * @param vectorResId Resource ID of the vector drawable
     * @return BitmapDescriptor created from the vector drawable
     */
    private BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(mContext, vectorResId);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }
        
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        
        // Use default size if intrinsic size is not available
        if (width <= 0) width = 40;
        if (height <= 0) height = 40;
        
        vectorDrawable.setBounds(0, 0, width, height);
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    
    /**
     * Switch to the appropriate map view based on display mode
     * Always uses 2D mode regardless of parameter
     * 
     * @param is3DMode Ignored - always uses 2D mode
     */
    public void setDisplayMode(boolean is3DMode) {
        // Always use 2D mode map regardless of parameter
        if (mMapView3D != null) mMapView3D.setVisibility(android.view.View.GONE);
        if (mMapView2D != null) mMapView2D.setVisibility(android.view.View.VISIBLE);
        
        // If map is not ready, initialize it
        if (mIsMapReady && mMapView2D != null) {
            mMapView2D.getMapAsync(this);
        }
    }
    
    /**
     * Handle lifecycle events
     */
    
    public void onCreate(Bundle savedInstanceState) {
        if (mMapView2D != null) mMapView2D.onCreate(savedInstanceState);
        if (mMapView3D != null) mMapView3D.onCreate(savedInstanceState);
    }
    
    public void onResume() {
        if (mMapView2D != null) mMapView2D.onResume();
        if (mMapView3D != null) mMapView3D.onResume();
        startLocationUpdates();
    }
    
    public void onPause() {
        stopLocationUpdates();
        if (mMapView2D != null) mMapView2D.onPause();
        if (mMapView3D != null) mMapView3D.onPause();
    }
    
    public void onDestroy() {
        if (mMapView2D != null) mMapView2D.onDestroy();
        if (mMapView3D != null) mMapView3D.onDestroy();
    }
    
    public void onLowMemory() {
        if (mMapView2D != null) mMapView2D.onLowMemory();
        if (mMapView3D != null) mMapView3D.onLowMemory();
    }
    
    public void onSaveInstanceState(Bundle outState) {
        if (mMapView2D != null) mMapView2D.onSaveInstanceState(outState);
        if (mMapView3D != null) mMapView3D.onSaveInstanceState(outState);
    }
}
