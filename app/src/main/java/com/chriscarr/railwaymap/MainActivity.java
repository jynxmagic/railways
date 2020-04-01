package com.chriscarr.railwaymap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.FillManager;
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions;
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    String[] requiredPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    boolean hasPermissions;

    private String latitude;
    private String longitude;
    private String server_url = "http://10.0.2.2:8080/stations";
    ArrayList<JSONObject> nearbyStations;


    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);


        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);


        //if we don't have dangerous permissions, ask for them
        if (!checkPermissions()) {
            requestRequiredPermissionsFromUser();
        }

        //if the user does not grant us permissions after asking, we need to know this
        this.hasPermissions = checkPermissions();

        instantiateLocation();

        downloadRailwaysJSONData();

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                    }
                });

            }

        });

    }


    private void downloadRailwaysJSONData() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                while(latitude == null || longitude == null)
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                nearbyStations = new ArrayList<>();

                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {
                    try {
                        String strUrl = server_url + "?lat=" + latitude + "&lng=" + longitude;
                        URL url = new URL(strUrl);
                        URLConnection connection = url.openConnection();
                        InputStreamReader ins = new InputStreamReader(connection.getInputStream());
                        BufferedReader in = new BufferedReader(ins);




                        String line = "";
                        while ((line = in.readLine()) != null) {
                            Log.println(Log.DEBUG, "debug_logs", line);
                            JSONArray ja = new JSONArray(line);
                            for(int i = 0; i < ja.length(); i++)
                            {
                                JSONObject jo = (JSONObject) ja.get(i);
                                nearbyStations.add(jo);
                            }
                        }
                        in.close();
                    } catch (Exception e) {
                        Log.e("error_logs", e.getLocalizedMessage(), e);
                    }

                }
            }
        }).start();
    }

    private void updateMap() {

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)), 13d), 2000);

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.mapbox_marker_icon_default);

                        style.addImage("my-marker-image", icon);

                        SymbolLayer symbolLayer = new SymbolLayer("layer-id", "source-id");
                        symbolLayer.setProperties(PropertyFactory.iconImage("my-marker-image"));

                        for(JSONObject stationArray : nearbyStations)
                        {
                            SymbolManager sm = new SymbolManager(mapView, mapboxMap, style);
                            try {
                                Symbol s = sm.create(new SymbolOptions()
                                                        .withIconSize(1f)
                                                        .withIconImage("my-marker-image")
                                                        .withLatLng(new LatLng((Double) stationArray.getDouble("Latitude"), (Double)stationArray.getDouble("Longitude")))
                                                        .withTextField(stationArray.getString("StationName"))
                                                        .withTextSize(15f)
                                                        .withIconColor("red"));

                                sm.update(s);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        style.addLayer(symbolLayer);

                        CircleManager cm = new CircleManager(mapView, mapboxMap, style);

                        Circle circle = cm.create(new CircleOptions()
                                .withLatLng(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)))
                                .withCircleColor("blue")
                                .withCircleRadius(5f)
                        );

                        cm.update(circle);

                        cm.updateSource();


                    }
                });

            }

        });
    }

    public void findMeButtonOnClickEventHandler(View view)
    {
        downloadRailwaysJSONData();
        updateMap();
        try {
            updateNearbyTrainStations();
        } catch (Exception e) {
            Log.e("exceptions", "error calculating distance", e);
        }
    }

    private void updateNearbyTrainStations() throws JSONException {
        LinearLayout layout = (LinearLayout)findViewById(R.id.vertical_scroll_texts);

        layout.removeAllViews();

        for(JSONObject stationArray : nearbyStations)
        {
            TextView tvm = new TextView(getApplicationContext());
            tvm.setText("Station Name: " + stationArray.getString("StationName"));
            tvm.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));


            TextView tvt = new TextView(getApplicationContext());
            float[] results = new float[2];
            Location.distanceBetween(Double.parseDouble(latitude), Double.parseDouble(longitude), stationArray.getDouble("Latitude"),stationArray.getDouble("Longitude"), results);
            tvt.setText("Distance: "+results[0]+"m");
            tvt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            layout.addView(tvm);
            layout.addView(tvt);
        }

        layout.setBackgroundColor(Color.TRANSPARENT);
        layout.invalidate();

    }




    @SuppressLint("MissingPermission")
    private void instantiateLocation()
    {
        if(this.hasPermissions)
        {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 5, new LocationListener());
        }
        else
        {
            //if we don't have permission, instantiate lat/long as kris's office
            latitude = "53.472";
            longitude = "-2.244";
        }
    }



    class LocationListener implements android.location.LocationListener
    {
        @Override
        public void onLocationChanged(Location location)
        {
            Log.d("debug", "location updated");
            latitude = String.valueOf(location.getLatitude());
            longitude = String.valueOf(location.getLongitude());
            String viewLatitude = getString(R.string.latitude) + latitude;
            String viewLongitude = getString(R.string.longitude) + longitude;
            ((TextView) findViewById(R.id.Latitude)).setText(viewLatitude);
            ((TextView) findViewById(R.id.Longitude)).setText(viewLongitude);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }

    private void requestRequiredPermissionsFromUser()
    {
            //request permission
            ActivityCompat.requestPermissions(this, requiredPermissions, 1);
      //      System.exit(0);
    }

    private boolean checkPermissions()
    {
        //iterate over each permission and check if we have it
        for(int i=0; i<requiredPermissions.length; i++)
        {
            int result = ActivityCompat.checkSelfPermission(this, requiredPermissions[i]);
            if(result != PackageManager.PERMISSION_GRANTED)
            {
                //we don't have x permission
                return false;
            }
        }
        //all permissions granted
        return true;
    }
}
