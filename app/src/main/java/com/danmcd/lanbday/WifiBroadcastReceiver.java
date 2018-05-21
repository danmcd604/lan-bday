package com.danmcd.lanbday;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.util.Log;

/**
 * Created by dan on 5/19/18.
 */

public class WifiBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiBroadcastReceiver";

    // Communication //
    private WifiP2pManager mWifiManager;
    private Channel mChannel;
    private ConnectionListener mListener;

    public WifiBroadcastReceiver(
            WifiP2pManager manager, Channel channel, ConnectionListener listener) {
        mWifiManager = manager;
        mChannel = channel;
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Receiving broadcast...");
        // Get intent action:
        String action = intent.getAction();

        Log.d(TAG, "Action: "+action);

        // Handle intent action:
        if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Received connection changed action");
            if (mWifiManager == null) {
                return;
            }
            // Get network info:
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            // Handle network info:
            if (networkInfo.isConnected()) {
                Log.i(TAG,
                        "Connected to p2p network. Requesting network details");
                // Request information:
                mWifiManager.requestConnectionInfo(mChannel,
                        mListener);
            }

            // Notify listener of connection state:
            notifyConnected(networkInfo.isConnected());
        } else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Received Device Changed Action");
        } else if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "Received P2P State Change Action");
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            //TODO: notify user of device P2P capabilities
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(TAG, "P2P State Enabled");
            } else {
                Log.i(TAG, "P2P State Disabled");
            }
        }

    }

    private void notifyConnected(boolean connected) {
        if(mListener == null) {
            return;
        }

        mListener.onConnectionStateChanged(connected);
    }
}
