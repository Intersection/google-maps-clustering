package net.sharewire.googlemapsclustering;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sharewire.googlemapsclustering.Preconditions.checkNotNull;

class ClusterRenderer<T extends ClusterItem> implements GoogleMap.OnMarkerClickListener {

    private static final int BACKGROUND_MARKER_Z_INDEX = 0;

    private static final int FOREGROUND_MARKER_Z_INDEX = 1;

    private final GoogleMap mGoogleMap;

    private final List<MarkerCluster<T>> mClusters = new ArrayList<>();

    private final Map<MarkerCluster<T>, Marker> mMarkers = new HashMap<>();

    private IconGenerator<T> mIconGenerator;

    private ClusterManager.Callbacks<T> mCallbacks;

    ClusterRenderer(@NonNull Context context, @NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMarkerClickListener(this);
        mIconGenerator = new DefaultIconGenerator<>(context);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Object markerTag = marker.getTag();
        if (markerTag instanceof MarkerCluster) {
            //noinspection unchecked
            MarkerCluster<T> cluster = (MarkerCluster<T>) markerTag;
            //noinspection ConstantConditions
            List<T> clusterItems = cluster.getItems();

            if (mCallbacks != null) {
                return clusterItems.size() == 1 ?
                        mCallbacks.onClusterItemClick(clusterItems.get(0)) :
                        mCallbacks.onClusterClick(cluster);
            }
        }

        return false;
    }

    void setCallbacks(@Nullable ClusterManager.Callbacks<T> listener) {
        mCallbacks = listener;
    }

    void setIconGenerator(@NonNull IconGenerator<T> iconGenerator) {
        mIconGenerator = iconGenerator;
    }

    void render(@NonNull List<MarkerCluster<T>> clusters) {
        List<MarkerCluster<T>> clustersToAdd = new ArrayList<>();
        List<MarkerCluster<T>> clustersToRemove = new ArrayList<>();

        for (MarkerCluster<T> cluster : clusters) {
            if (!mMarkers.containsKey(cluster)) {
                clustersToAdd.add(cluster);
            }
        }

        for (MarkerCluster<T> cluster : mMarkers.keySet()) {
            if (!clusters.contains(cluster)) {
                clustersToRemove.add(cluster);
            }
        }

        mClusters.addAll(clustersToAdd);
        mClusters.removeAll(clustersToRemove);

        // Remove the old clusters.
        for (MarkerCluster<T> clusterToRemove : clustersToRemove) {
            Marker markerToRemove = mMarkers.get(clusterToRemove);
            markerToRemove.setZIndex(BACKGROUND_MARKER_Z_INDEX);

            MarkerCluster<T> parentCluster = findParentCluster(mClusters, 
                    clusterToRemove.getPosition().latitude,
                    clusterToRemove.getPosition().longitude);
            
            if (parentCluster != null) {
                animateMarkerToLocation(markerToRemove, parentCluster.getPosition(), true);
            } else {
                markerToRemove.remove();
            }

            mMarkers.remove(clusterToRemove);
        }

        // Add the new clusters.
        for (MarkerCluster<T> clusterToAdd : clustersToAdd) {
            Marker markerToAdd;

            BitmapDescriptor markerIcon = getMarkerIcon(clusterToAdd);
            String markerTitle = getMarkerTitle(clusterToAdd);
            String markerSnippet = getMarkerSnippet(clusterToAdd);

            MarkerCluster parentCluster = findParentCluster(clustersToRemove, 
                    clusterToAdd.getPosition().latitude,
                    clusterToAdd.getPosition().longitude);
            if (parentCluster != null) {
                markerToAdd = mGoogleMap.addMarker(new MarkerOptions()
                        .position(parentCluster.getPosition())
                        .icon(markerIcon)
                        .title(markerTitle)
                        .snippet(markerSnippet)
                        .zIndex(FOREGROUND_MARKER_Z_INDEX));
                animateMarkerToLocation(markerToAdd, clusterToAdd.getPosition(), false);
            } else {
                markerToAdd = mGoogleMap.addMarker(new MarkerOptions()
                        .position(clusterToAdd.getPosition())
                        .icon(markerIcon)
                        .title(markerTitle)
                        .snippet(markerSnippet)
                        .alpha(0.0F)
                        .zIndex(FOREGROUND_MARKER_Z_INDEX));
                animateMarkerAppearance(markerToAdd);
            }
            markerToAdd.setTag(clusterToAdd);

            mMarkers.put(clusterToAdd, markerToAdd);
        }
    }

    public void refreshMarkers() {
        synchronized (mClusters) {
            for (MarkerCluster<T> cluster : mClusters) {
                Marker marker = mMarkers.get(cluster);
                marker.setIcon(getMarkerIcon(cluster));
            }
        }
    }

    @NonNull
    private BitmapDescriptor getMarkerIcon(@NonNull MarkerCluster<T> cluster) {
        BitmapDescriptor clusterIcon;

        List<T> clusterItems = cluster.getItems();
        clusterIcon = clusterItems.size() == 1 ?
                mIconGenerator.getClusterItemIcon(clusterItems.get(0)) :
                mIconGenerator.getClusterIcon(cluster);

        return checkNotNull(clusterIcon);
    }

    @Nullable
    private String getMarkerTitle(@NonNull MarkerCluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        return clusterItems.size() == 1 ? clusterItems.get(0).getTitle() : null;
    }

    @Nullable
    private String getMarkerSnippet(@NonNull MarkerCluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        return clusterItems.size() == 1 ? clusterItems.get(0).getSnippet() : null;
    }

    @Nullable
    private MarkerCluster<T> findParentCluster(@NonNull List<MarkerCluster<T>> clusters,
                                            double latitude, double longitude) {
        for (MarkerCluster<T> cluster : clusters) {
            if (cluster.contains(latitude, longitude)) {
                return cluster;
            }
        }

        return null;
    }

    private void animateMarkerToLocation(@NonNull final Marker marker, @NonNull LatLng targetLocation,
                                         final boolean removeAfter) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(marker, "position",
                new LatLngTypeEvaluator(), targetLocation);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (removeAfter) {
                    marker.remove();
                }
            }
        });
        objectAnimator.start();
    }

    private void animateMarkerAppearance(@NonNull Marker marker) {
        ObjectAnimator.ofFloat(marker, "alpha", 1.0F).start();
    }

    Collection<Marker> getMarkers() {
        return mMarkers.values();
    }

    private static class LatLngTypeEvaluator implements TypeEvaluator<LatLng> {

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            double latitude = (endValue.latitude - startValue.latitude) * fraction + startValue.latitude;
            double longitude = (endValue.longitude - startValue.longitude) * fraction + startValue.longitude;
            return new LatLng(latitude, longitude);
        }
    }
}
