package com.sharewire.googlemapsclustering.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import net.sharewire.googlemapsclustering.ClusterManager;
import net.sharewire.googlemapsclustering.DefaultIconGenerator;
import net.sharewire.googlemapsclustering.MarkerCluster;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int MAP_ZOOM_LEVEL_CITY = 14;
    private float lastZoomLevel;

    private static final LatLngBounds NETHERLANDS = new LatLngBounds(
            new LatLng(50.77083, 3.57361), new LatLng(53.35917, 7.10833));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (savedInstanceState == null) {
            setupMapFragment();
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(NETHERLANDS, 0));
            }
        });

        ClusterManager<SampleClusterItem> clusterManager = new ClusterManager<SampleClusterItem>(this, googleMap) {
            @Override
            public void onCameraIdle() {
                super.onCameraIdle();
                handleZoomLevelChange(googleMap, this);
            }
        };
        clusterManager.setCallbacks(new ClusterManager.Callbacks<SampleClusterItem>() {
            @Override
            public boolean onClusterClick(@NonNull MarkerCluster<SampleClusterItem> cluster) {
                Log.d(TAG, "onClusterClick");
                return false;
            }

            @Override
            public boolean onClusterItemClick(@NonNull SampleClusterItem clusterItem) {
                Log.d(TAG, "onClusterItemClick");
                return false;
            }
        });
        clusterManager.setIconGenerator(new FlipIconGenerator(this, googleMap));
        googleMap.setOnCameraIdleListener(clusterManager);

        List<SampleClusterItem> clusterItems = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            clusterItems.add(new SampleClusterItem(RandomLocationGenerator.generate(NETHERLANDS)));
        }
        clusterManager.setItems(clusterItems);
    }

    private void handleZoomLevelChange(GoogleMap googleMap, ClusterManager clusterManager) {
        if ((googleMap.getCameraPosition().zoom > MAP_ZOOM_LEVEL_CITY && lastZoomLevel <= MAP_ZOOM_LEVEL_CITY) ||
            (googleMap.getCameraPosition().zoom <= MAP_ZOOM_LEVEL_CITY && lastZoomLevel > MAP_ZOOM_LEVEL_CITY)) {
            clusterManager.refreshMarkerIcons();
        }
        lastZoomLevel = googleMap.getCameraPosition().zoom;
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.setRetainInstance(true);
        mapFragment.getMapAsync(this);
    }

    private static class FlipIconGenerator extends DefaultIconGenerator<SampleClusterItem> {

        private final GoogleMap googleMap;
        private final BitmapDescriptor flippedMarkerIcon;

        FlipIconGenerator(@NonNull Context context, GoogleMap googleMap) {
            super(context);
            this.googleMap = googleMap;
            flippedMarkerIcon = BitmapDescriptorFactory.fromBitmap(
                    rotateBitmap(
                            BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.ic_map_marker), 90));
        }

        @NonNull
        @Override
        public BitmapDescriptor getClusterItemIcon(@NonNull SampleClusterItem clusterItem) {
            return googleMap.getCameraPosition().zoom > MAP_ZOOM_LEVEL_CITY ?
                    flippedMarkerIcon :
                    super.getClusterItemIcon(clusterItem);
        }

        @SuppressWarnings("SameParameterValue")
        static Bitmap rotateBitmap(Bitmap source, float angle) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        }
    }
}
