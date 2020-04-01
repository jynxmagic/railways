package com.chriscarr.railwaymap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


/***
 * LocationManager gets either
 * A) Location from android system or
 * B) Hardcoded location if no permissions given
 */
public class LocationManager {

    private String latitude;
    private String longitude;
    private boolean hasPermissions;
    private Context applicationContext;


    /**
     * As soon as the location manager is instatiated it begins to request location from android system (if it has permissions)
     * @param withPermissions are permissions given or are we using kris's office?
     * @param context
     */
    public LocationManager(boolean withPermissions, Context context)
    {
        this.hasPermissions = withPermissions;
        this.applicationContext = context;
        instantiateLocation();
    }

    /**
     * Calls android system service to get current location using GPS.
     * If no permission is given, hardcoded values are used.
     */
    @SuppressLint("MissingPermission")
    private void instantiateLocation()
    {
        if(hasPermissions)
        {
            //get service
            android.location.LocationManager lm = (android.location.LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);

            //1 use Global Position System (GPS) to get location
            //2 check for updates every 100s
            //3 if phone has not moved 5m, do not send update
            //pass locationlistener inner class
            lm.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 100, 5, new LocationListener());
        }
        else
        {
            //if we don't have permission, instantiate lat/long as kris's office
            latitude = "53.472";
            longitude = "-2.244";
        }
    }


    /**
     * Inner class locationlistener handles android callback event "onLocationChange".
     * updates lat and long when location is changed.
     */
    class LocationListener implements android.location.LocationListener
    {
        @Override
        public void onLocationChanged(Location location)
        {
            latitude = String.valueOf(location.getLatitude());
            longitude = String.valueOf(location.getLongitude());
        }

        //unused but required
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }

    public String getLatitude()
    {
        return latitude;
    }
    public String getLongitude()
    {
        return longitude;
    }
}
