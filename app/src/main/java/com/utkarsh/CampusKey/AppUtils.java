package com.utkarsh.CampusKey;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AppUtils {

    // Method to get the version code as an int
    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            // Convert longVersionCode to int for API 28+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode(); // Cast long to int
            } else {
                return packageInfo.versionCode; // For older versions, versionCode is already int
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1; // Return -1 in case of an error
        }
    }
}
