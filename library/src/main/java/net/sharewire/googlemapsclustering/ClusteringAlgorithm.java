package net.sharewire.googlemapsclustering;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

public interface ClusteringAlgorithm<T extends ClusterItem> {
    void setMinClusterSize(int minClusterSize);
    void setItems(List<T> items);
    List<MarkerCluster<T>> getClusters(LatLngBounds latLngBounds, double zoomLevel);
}
