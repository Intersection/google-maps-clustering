package net.sharewire.googlemapsclustering;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static net.sharewire.googlemapsclustering.Preconditions.checkArgument;
import static net.sharewire.googlemapsclustering.Preconditions.checkNotNull;

/**
 * Groups multiple items on a map into clusters based on the current zoom level.
 * Clustering occurs when the map becomes idle, so an instance of this class
 * must be set as a camera idle listener using {@link GoogleMap#setOnCameraIdleListener}.
 *
 * @param <T> the type of an item to be clustered
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ClusterManager<T extends ClusterItem> implements GoogleMap.OnCameraIdleListener {

    private final GoogleMap mGoogleMap;

    private final ClusterRenderer<T> mRenderer;

    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    private ClusteringAlgorithm<T> mAlgorithm;

    private AsyncTask mQuadTreeTask;

    private AsyncTask mClusterTask;

    /**
     * Defines signatures for methods that are called when a cluster or a cluster item is clicked.
     *
     * @param <T> the type of an item managed by {@link ClusterManager}.
     */
    public interface Callbacks<T extends ClusterItem> {
        /**
         * Called when a marker representing a cluster has been clicked.
         *
         * @param cluster the cluster that has been clicked
         * @return <code>true</code> if the listener has consumed the event (i.e., the default behavior should not occur);
         * <code>false</code> otherwise (i.e., the default behavior should occur). The default behavior is for the camera
         * to move to the marker and an info window to appear.
         */
        boolean onClusterClick(@NonNull MarkerCluster<T> cluster);

        /**
         * Called when a marker representing a cluster item has been clicked.
         *
         * @param clusterItem the cluster item that has been clicked
         * @return <code>true</code> if the listener has consumed the event (i.e., the default behavior should not occur);
         * <code>false</code> otherwise (i.e., the default behavior should occur). The default behavior is for the camera
         * to move to the marker and an info window to appear.
         */
        boolean onClusterItemClick(@NonNull T clusterItem);
    }

    /**
     * Creates a new cluster manager using the default icon generator.
     * To customize marker icons, set a custom icon generator using
     * {@link ClusterManager#setIconGenerator(IconGenerator)}.
     *
     * @param googleMap the map instance where markers will be rendered
     */
    public ClusterManager(@NonNull Context context, @NonNull GoogleMap googleMap) {
        checkNotNull(context);
        mGoogleMap = checkNotNull(googleMap);
        mRenderer = new ClusterRenderer<>(context, googleMap);
        mAlgorithm = new ClusterAlgorithm<>();
    }

    /**
     * Sets a custom icon generator thus replacing the default one.
     *
     * @param iconGenerator the custom icon generator that's used for generating marker icons
     */
    public void setIconGenerator(@NonNull IconGenerator<T> iconGenerator) {
        checkNotNull(iconGenerator);
        mRenderer.setIconGenerator(iconGenerator);
    }

    /**
     * Sets a callback that's invoked when a cluster or a cluster item is clicked.
     *
     * @param callbacks the callback that's invoked when a cluster or an individual item is clicked.
     *                  To unset the callback, use <code>null</code>.
     */
    public void setCallbacks(@Nullable Callbacks<T> callbacks) {
        mRenderer.setCallbacks(callbacks);
    }

    /**
     * Sets items to be clustered thus replacing the old ones.
     *
     * @param clusterItems the items to be clustered
     */
    public void setItems(@NonNull List<T> clusterItems) {
        buildQuadTree(checkNotNull(clusterItems));
    }

    /**
     * Sets the minimum size of a cluster. If the cluster size
     * is less than this value, display individual markers.
     */
    public void setMinClusterSize(int minClusterSize) {
        checkArgument(minClusterSize > 0);
        mAlgorithm.setMinClusterSize(minClusterSize);
    }

    public void setAlgorithm(@NonNull ClusteringAlgorithm<T> algorithm) {
        mAlgorithm = checkNotNull(algorithm);
    }

    @Override
    public void onCameraIdle() {
        cluster();
    }

    public Collection<Marker> getMarkerCollection() {
        return this.mRenderer.getMarkers();
    }

    public void refreshMarkerIcons() {
        mRenderer.refreshMarkers();
    }

    private void buildQuadTree(@NonNull List<T> clusterItems) {
        if (mQuadTreeTask != null) {
            mQuadTreeTask.cancel(true);
        }

        mQuadTreeTask = new QuadTreeTask<>(clusterItems, this).executeOnExecutor(mExecutor);
    }

    private void cluster() {
        if (mClusterTask != null) {
            mClusterTask.cancel(true);
        }

        mClusterTask = new ClusterTask<>(mGoogleMap.getProjection().getVisibleRegion().latLngBounds,
                mGoogleMap.getCameraPosition().zoom, this).executeOnExecutor(mExecutor);
    }

    private static class QuadTreeTask<T extends ClusterItem> extends AsyncTask<Void, Void, Void> {

        private final List<T> mClusterItems;
        private final ClusterManager<T> mClusterManager;

        private QuadTreeTask(@NonNull List<T> clusterItems, ClusterManager<T> clusterManager) {
            mClusterItems = clusterItems;
            mClusterManager = clusterManager;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mClusterManager.mAlgorithm.setItems(mClusterItems);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mClusterManager.cluster();
            mClusterManager.mQuadTreeTask = null;
        }
    }

    private static class ClusterTask<T extends ClusterItem> extends AsyncTask<Void, Void, List<MarkerCluster<T>>> {

        private final LatLngBounds mLatLngBounds;
        private final float mZoomLevel;
        private final ClusterManager<T> mClusterManager;

        private ClusterTask(@NonNull LatLngBounds latLngBounds,
                            float zoomLevel,
                            ClusterManager<T> clusterManager) {
            mLatLngBounds = latLngBounds;
            mZoomLevel = zoomLevel;
            mClusterManager = clusterManager;
        }

        @Override
        protected List<MarkerCluster<T>> doInBackground(Void... params) {
            return mClusterManager.mAlgorithm.getClusters(mLatLngBounds, mZoomLevel);
        }

        @Override
        protected void onPostExecute(@NonNull List<MarkerCluster<T>> clusters) {
            mClusterManager.mRenderer.render(clusters);
            mClusterManager.mClusterTask = null;
        }
    }
}
