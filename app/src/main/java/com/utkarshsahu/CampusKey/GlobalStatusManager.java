package com.utkarshsahu.CampusKey;

import java.util.ArrayList;
import java.util.List;

public class GlobalStatusManager {
    
    public interface StatusListener {
        void onConnectionStarted();
        void onConnectionFinished();
    }

    private static final List<StatusListener> listeners = new ArrayList<>();
    private static boolean isConnecting = false;

    public static synchronized void setConnecting(boolean connecting) {
        isConnecting = connecting;
        for (StatusListener listener : listeners) {
            if (connecting) listener.onConnectionStarted();
            else           listener.onConnectionFinished();
        }
    }

    public static boolean isConnecting() {
        return isConnecting;
    }

    public static synchronized void addListener(StatusListener listener) {
        listeners.add(listener);
        // Immediately notify of current state
        if (isConnecting) listener.onConnectionStarted();
        else              listener.onConnectionFinished();
    }

    public static synchronized void removeListener(StatusListener listener) {
        listeners.remove(listener);
    }
}
