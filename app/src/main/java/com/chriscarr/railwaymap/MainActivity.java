package com.chriscarr.railwaymap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * The one and only Activity for the app.
 * Any methods which directly update the UI are within this activity.
 */
public class MainActivity extends AppCompatActivity {

    PermissionManager permissionManager;
    LocationManager locationManager;
    DownloadManager downloadManager;


    boolean hasPermissions = true;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);


        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);


        this.permissionManager = new PermissionManager(getApplicationContext(), this);

        //if we don't have dangerous permissions, ask for them
        if (!permissionManager.checkPermissions()) {
            permissionManager.requestRequiredPermissionsFromUser();
        }

        this.locationManager = new LocationManager(this.hasPermissions, getApplicationContext());
        this.downloadManager = new DownloadManager(this.locationManager, getApplicationContext());
        downloadManager.start();

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


    /**
     * Event listener for when "Find Me!" button is clicked.
     * This method:
     * 1. updates latitude and longitude text view
     * 2. re-downloads nearest train stations json
     * 3. centers map on position
     * 4. adds markers to the map
     * @param view
     */
    public void findMeButtonOnClickEventHandler(View view)
    {
        updateLatLong();
        this.downloadManager = new DownloadManager(locationManager, getApplicationContext());
        downloadManager.start();
        updateMap();
        try {
            updateNearbyTrainStations();
        } catch (Exception e) {
            Log.e("exceptions", "error calculating distance", e);
        }
    }


    /**
     * Updates latitude and longitude Textview to the latest value in the locationmanager.
     *
     */
    private void updateLatLong()
    {
        TextView latitudeTextView = (TextView)findViewById(R.id.Latitude);
        TextView longitutdeTextView = (TextView)findViewById(R.id.Longitude);

        //getString method helps with any future translation for hardcoded strings
        String latitudeString = getString(R.string.latitude) + locationManager.getLatitude();
        String longitudeString = getString(R.string.longitude) + locationManager.getLongitude();

        latitudeTextView.setText(latitudeString);
        longitutdeTextView.setText(longitudeString);

        //todo do the text views need invalidating after update?

    }


    /**
     * 1. Centres map on current lat/long of locationmanager.
     * 2. adds markers based on closest train stations to current position
     * 3. adds marker of current position
     */
    private void updateMap() {

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {

                //1 Centres map on current lat/long of locationmanager.
                mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(locationManager.getLatitude()), Double.parseDouble(locationManager.getLongitude())), 13d), 2000);

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {


                        //2 adds markers based on closest train stations to current position
                        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.mapbox_marker_icon_default);

                        //the code is taken from mapbox docs and uses the documentations supplied id's.
                        style.addImage("my-marker-image", icon);

                        SymbolLayer symbolLayer = new SymbolLayer("layer-id", "source-id");
                        symbolLayer.setProperties(PropertyFactory.iconImage("my-marker-image"));

                        //for each nearby station
                        for(JSONObject stationArray : downloadManager.getNearbyStations())
                        {
                            //create a symbol manager to manage the symbol
                            SymbolManager sm = new SymbolManager(mapView, mapboxMap, style);
                            try {
                                //use symbol manager builder method to build its own symbol
                                Symbol s = sm.create(new SymbolOptions()
                                                        .withIconSize(1f)
                                                        .withIconImage("my-marker-image")
                                                        .withLatLng(new LatLng((Double) stationArray.getDouble("Latitude"), (Double)stationArray.getDouble("Longitude")))
                                                        .withTextField(stationArray.getString("StationName"))
                                                        .withTextSize(15f)
                                                        .withIconColor("red"));

                                //tell symbol manager to add the symbol
                                sm.update(s);

                            } catch (Exception e) {
                                Log.e("error", "error generating map data", e);
                            }
                        }

                        style.addLayer(symbolLayer);


                        //3 adds marker of current position - just blue circle

                        CircleManager cm = new CircleManager(mapView, mapboxMap, style);

                        Circle circle = cm.create(new CircleOptions()
                                .withLatLng(new LatLng(Double.parseDouble(locationManager.getLatitude()), Double.parseDouble(locationManager.getLongitude())))
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

    /**
     * Updates the linearlayout with multiple text views based on information gathered from the server by the download manager.
     * There is a race condition here (possible for the method to be called before download completes).  There is a downloadCompleted flag in downloadmanager object for this.
     *
     * @throws JSONException
     */
    private void updateNearbyTrainStations() throws JSONException {

        //get the data in scrollview and delete eveything
        LinearLayout layout = (LinearLayout)findViewById(R.id.vertical_scroll_texts);

        //f
        layout.removeAllViews();


        //flag to ensure race condition is finished as expected
        while(!downloadManager.isFinishedDownload())
        {
            try {
                //it crashes for 0.1s but who notices
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e("thread sleep", "Error when trying to stall race condtion", e);
            }
        }

        //get our newly downloaded stations
        for(JSONObject stationArray : this.downloadManager.getNearbyStations())
        {
            //make text view with station name
            TextView tvm = new TextView(getApplicationContext());
            tvm.setText("Station Name: " + stationArray.getString("StationName"));
            tvm.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            //make text view with distance
            TextView tvt = new TextView(getApplicationContext());
            float[] results = new float[2];
            Location.distanceBetween(Double.parseDouble(locationManager.getLatitude()), Double.parseDouble(locationManager.getLongitude()), stationArray.getDouble("Latitude"),stationArray.getDouble("Longitude"), results);

            //results[0] assumes user is facing the correct direction (?)
            tvt.setText("Distance: "+results[0]+"m");
            tvt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            //add views
            layout.addView(tvm);
            layout.addView(tvt);
        }

        //invalidate the scroll to ensure it is reloaded next frame.
        layout.setBackgroundColor(Color.TRANSPARENT);
        layout.invalidate();

    }











}
