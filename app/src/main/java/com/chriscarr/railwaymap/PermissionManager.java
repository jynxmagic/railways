package com.chriscarr.railwaymap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

/**
 * permission manager checks requred permissions and handles user requests for permissions
 */
public class PermissionManager {

    //all dangerous permissions the app requires are listed here
    private static final String[] requiredPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private Context context;
    private Activity activity;

    public PermissionManager(Context context, Activity activity)
    {
        this.context = context;
        this.activity = activity;
    }


    /**
     * Requests ALL required permissions from user
     */
    public void requestRequiredPermissionsFromUser()
    {
        ActivityCompat.requestPermissions(activity, requiredPermissions, 1);
    }


    /**
     * Checks if ALL required permissions have been granted
     * @return true if all permissions granted
     */
    public boolean checkPermissions()
    {
        //iterate over each permission and check if we have it
        for (String requiredPermission : requiredPermissions) {
            int result = ActivityCompat.checkSelfPermission(context, requiredPermission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                //we don't have x permission
                return false;
            }
        }
        //all permissions granted
        return true;
    }
}
