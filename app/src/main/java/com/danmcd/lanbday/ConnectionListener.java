package com.danmcd.lanbday;

import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by dan on 5/19/18.
 */

public interface ConnectionListener extends WifiP2pManager.ConnectionInfoListener {
    void onConnectionStateChanged(boolean connected);
}
