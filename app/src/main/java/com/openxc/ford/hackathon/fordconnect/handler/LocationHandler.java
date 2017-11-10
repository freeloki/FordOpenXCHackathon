package com.openxc.ford.hackathon.fordconnect.handler;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.openxc.ford.hackathon.fordconnect.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by codemania on 11/4/17.
 */
 
/**
 * @author Yavuz Erzurumlu
 * @author Ceyhun Ertürk
 */

public class LocationHandler {

    private Context appContext;
    private LocationManager mLocationManager;
    private Location lastLocation;

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            // makeUseOfNewLocation(location);
            lastLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    public LocationHandler(Context ctx) {
        this.appContext = ctx;
        mLocationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // return TODO;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    public void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return TODO;
        }
        lastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    public String getPrettyLocation() {
        JSONObject locJSON = new JSONObject();

        try {
            locJSON.put(Constants.LATITUDE, lastLocation.getLatitude());
            locJSON.put(Constants.LONGITUDE, lastLocation.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return locJSON.toString();

    }
}
