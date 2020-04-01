package com.chriscarr.railwaymap;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;


/**
 * Class used to manage downloading the JSON data from the server.
 * @dependency LocationManager
 */
public class DownloadManager extends Thread
{
    //we must have a location for the download to not be malformed.
    //thus downloadmanager is dependant upon locationmanager
    private LocationManager lm;

    private Context context;
    private static final String server_url = "http://10.0.2.2:8080/stations";
    private ArrayList<JSONObject> nearbyStations;

    private boolean finishedDownload = false;

    public DownloadManager(LocationManager lm, Context context)
    {
        this.lm = lm;
        this.context = context;
    }


    /**
     * Main method for this Thread.
     * Called via Start.
     */
    @Override
    public void run()
    {
        //wait for latitude and longitude to be updated within the location manager.
        while(lm.getLatitude() == null || lm.getLongitude() == null)
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //check connection
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                //build url
                String strUrl = server_url + "?lat=" + lm.getLatitude() + "&lng=" + lm.getLongitude();
                URL url = new URL(strUrl);
                URLConnection connection = url.openConnection();
                InputStreamReader ins = new InputStreamReader(connection.getInputStream());
                BufferedReader in = new BufferedReader(ins);


                this.nearbyStations = new ArrayList<>();


                //build nearby stations from server response json
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

                //todo exception handling
                in.close();
            } catch (Exception e) {
                Log.e("error_logs", e.getLocalizedMessage(), e);
            }

        }
        this.finishedDownload = true;
    }

    public ArrayList<JSONObject> getNearbyStations()
    {
        return this.nearbyStations;
    }

    public boolean isFinishedDownload() {
        return finishedDownload;
    }
}
