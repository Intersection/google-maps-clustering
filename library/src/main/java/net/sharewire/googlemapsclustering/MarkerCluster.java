package net.sharewire.googlemapsclustering;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface MarkerCluster<T extends ClusterItem> {
    LatLng getPosition();

    List<T> getItems();

    int getSize();

    boolean contains(double latitude, double longitude);
}
