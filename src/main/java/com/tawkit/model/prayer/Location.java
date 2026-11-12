package com.tawkit.model.prayer;

/**
 * Geographic location used for prayer-time calculation.
 */
public class Location {

    private String name;
    private String country;
    private double latitude;
    private double longitude;
    private double timezone;  // offset from UTC in hours

    public Location() {
    }

    public Location(String name, String country, double latitude, double longitude, double timezone) {
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getTimezone() {
        return timezone;
    }

    public void setTimezone(double timezone) {
        this.timezone = timezone;
    }

    public String getDisplayName() {
        if (country != null && !country.isEmpty()) {
            return name + ", " + country;
        }
        return name;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
