package org.opentripplanner.middleware.utils;

import io.leonard.Position;

import java.util.Objects;

/**
 * Helper class to contain lat/lon values.
 */
public class Coordinates {
    public Double lon;
    public Double lat;

    /** Required for JSON serialization */
    public Coordinates() {
    }

    public Coordinates(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public Coordinates(Position position) {
        this.lat = position.getLatitude();
        this.lon = position.getLongitude();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return sameCoordinates((Coordinates) o);
    }

    public boolean sameCoordinates(Coordinates that) {
        return Objects.equals(lon, that.lon) && Objects.equals(lat, that.lat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lon, lat);
    }

    @Override
    public String toString() {
        return String.format("Coordinates{lat=%s, lon=%s}", lat, lon);
    }
}
