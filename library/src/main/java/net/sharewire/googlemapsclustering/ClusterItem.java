package net.sharewire.googlemapsclustering;

import com.google.android.gms.maps.model.LatLng;

public interface ClusterItem {
    LatLng getPosition();

    String getTitle();

    String getSnippet();
}
