package com.utkarsh.CampusKey;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalData {
    static final String MAIN_TEXT = "7489927";
    static final String DESCRIPTION = "6354773";
    static final String URL = "864383";
    static final String FORCE = "92747";
    static final String MINIMUM_VERSION_CODE = "36656";
    static final String LAST_CHECK_TIME= "67533";
    Context context;

    public LocalData(Context context) {
        this.context = context;
    }

    // Method to get a String from SharedPreferences
    public String getString(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, "Default Value");
    }

    // Method to get an int from SharedPreferences
    public int getInt(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getInt(key, -1);
    }
    public long getLong(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getLong(key, -1);
    }

    // Method to save a String in SharedPreferences
    public void save(String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Method to save an int in SharedPreferences
    public void save(String key, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    // Method to save a boolean in SharedPreferences
    public void save(String key, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Method to get a boolean from SharedPreferences
    public boolean getBoolean(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, false); // default is false if not found
    }
    public void save(String key, long value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }
}
