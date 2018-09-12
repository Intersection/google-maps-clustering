package net.sharewire.googlemapsclustering;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClusterAlgorithm<T extends ClusterItem> implements ClusteringAlgorithm<T> {

    private static final int QUAD_TREE_BUCKET_CAPACITY = 4;
    private static final int DEFAULT_MIN_CLUSTER_SIZE = 1;

    private final QuadTree<T> mQuadTree;
    private int mMinClusterSize;

    @SuppressWarnings("WeakerAccess")
    public ClusterAlgorithm() {
        this.mMinClusterSize = DEFAULT_MIN_CLUSTER_SIZE;
        mQuadTree = new QuadTree<>(QUAD_TREE_BUCKET_CAPACITY);
    }

    @Override
    public void setItems(List<T> items) {
        mQuadTree.clear();
        for (T clusterItem : items) {
            mQuadTree.insert(clusterItem);
        }
    }

    @Override
    public List<MarkerCluster<T>> getClusters(LatLngBounds latLngBounds, double zoomLevel) {
        List<MarkerCluster<T>> clusters = new ArrayList<>();

        long tileCount = (long) (Math.pow(2, zoomLevel) * 2);

        double startLatitude = latLngBounds.northeast.latitude;
        double endLatitude = latLngBounds.southwest.latitude;

        double startLongitude = latLngBounds.southwest.longitude;
        double endLongitude = latLngBounds.northeast.longitude;

        double stepLatitude = 180.0 / tileCount;
        double stepLongitude = 360.0 / tileCount;

        if (startLongitude > endLongitude) { // Longitude +180°/-180° overlap.
            // [start longitude; 180]
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    startLongitude, 180.0, stepLatitude, stepLongitude);
            // [-180; end longitude]
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    -180.0, endLongitude, stepLatitude, stepLongitude);
        } else {
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    startLongitude, endLongitude, stepLatitude, stepLongitude);
        }

        return clusters;
    }

    private void getClustersInsideBounds(@NonNull List<MarkerCluster<T>> clusters,
                                         double startLatitude, double endLatitude,
                                         double startLongitude, double endLongitude,
                                         double stepLatitude, double stepLongitude) {
        long startX = (long) ((startLongitude + 180.0) / stepLongitude);
        long startY = (long) ((90.0 - startLatitude) / stepLatitude);

        long endX = (long) ((endLongitude + 180.0) / stepLongitude) + 1;
        long endY = (long) ((90.0 - endLatitude) / stepLatitude) + 1;

        for (long tileX = startX; tileX <= endX; tileX++) {
            for (long tileY = startY; tileY <= endY; tileY++) {
                double north = 90.0 - tileY * stepLatitude;
                double west = tileX * stepLongitude - 180.0;
                double south = north - stepLatitude;
                double east = west + stepLongitude;

                List<T> points = mQuadTree.queryRange(north, west, south, east);

                if (points.isEmpty()) {
                    continue;
                }

                if (points.size() >= mMinClusterSize) {
                    double totalLatitude = 0;
                    double totalLongitude = 0;

                    for (T point : points) {
                        totalLatitude += point.getPosition().latitude;
                        totalLongitude += point.getPosition().longitude;
                    }

                    double latitude = totalLatitude / points.size();
                    double longitude = totalLongitude / points.size();

                    clusters.add(new MapCluster<>(latitude, longitude,
                            points, north, west, south, east));
                } else {
                    for (T point : points) {
                        clusters.add(new MapCluster<>(point.getPosition().latitude, point.getPosition().longitude,
                                Collections.singletonList(point), north, west, south, east));
                    }
                }
            }
        }
    }

    @Override
    public void setMinClusterSize(int minClusterSize) {
        mMinClusterSize = minClusterSize;
    }
}
