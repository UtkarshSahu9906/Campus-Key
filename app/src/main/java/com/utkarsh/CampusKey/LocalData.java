package com.utkarsh.CampusKey;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalData {

    // Keys
    public static final String URL                  = "url";
    public static final String MAIN_TEXT            = "main_text";
    public static final String DESCRIPTION          = "description";
    public static final String FORCE                = "force";
    public static final String MINIMUM_VERSION_CODE = "minimum_version_code";
    public static final String NETWORK_ERROR        = "network_error";
    public static final String LAST_CHECK_TIME    = "last_check_time";
    public static final String PROFILE_ASKED        = "profile_asked";

    private final SharedPreferences prefs;

    public LocalData(Context context) {
        prefs = context.getSharedPreferences("CampusKeyPrefs", Context.MODE_PRIVATE);
    }

    public void save(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public void save(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void save(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void save(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    public String getString(String key) {
        return prefs.getString(key, "");
    }

    public boolean getBoolean(String key) {
        return prefs.getBoolean(key, false);
    }

    public int getInt(String key) {
        return prefs.getInt(key, 0);
    }

    public long getLong(String key) {
        return prefs.getLong(key, 0L);
    }
}