package net.sharewire.googlemapsclustering;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * An object representing a cluster of items (markers) on the map.
 */
public class MapCluster<T extends ClusterItem> implements MarkerCluster<T> {

    private final LatLng position;
    private final List<T> items;
    private final double north;
    private final double west;
    private final double south;
    private final double east;

    @SuppressWarnings("WeakerAccess")
    public MapCluster(double latitude, double longitude, @NonNull List<T> items,
                      double north, double west, double south, double east) {
        position = new LatLng(latitude, longitude);
        this.items = items;
        this.north = north;
        this.west = west;
        this.south = south;
        this.east = east;
    }

    /**
     * The latitude of the cluster.
     *
     * @return the latitude of the cluster
     */
    public double getLatitude() {
        return position.latitude;
    }

    /**
     * The longitude of the cluster.
     *
     * @return the longitude of the cluster
     */
    public double getLongitude() {
        return position.longitude;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    /**
     * The items contained in the cluster.
     *
     * @return the items contained in the cluster
     */
    @NonNull
    public List<T> getItems() {
        return items;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public boolean contains(double latitude, double longitude) {
        return longitude >= west && longitude <= east
                && latitude <= north && latitude >= south;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapCluster cluster = (MapCluster) o;
        return Double.compare(cluster.position.latitude, position.latitude) == 0 &&
                Double.compare(cluster.position.longitude, position.longitude) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(position.latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(position.longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
