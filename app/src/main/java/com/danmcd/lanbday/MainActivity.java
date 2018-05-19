package com.danmcd.lanbday;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SenderFragment.SenderInteractions, WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.DnsSdTxtRecordListener,
        Handler.Callback{

    private static final String TAG = "MainActivity";

    // Service //
    private static final String SERVICE_INSTANCE = "_landbay";
    private static final String SERVICE_TYPE = "_presence._tcp";

    public static int SERVER_PORT = 8989;

    private WifiP2pDnsSdServiceRequest mServiceRequest;

    // Communication //
    private IntentFilter mIntentFilter = new IntentFilter();
    private WifiBroadcastReceiver mWifiReceiver;
    private WifiP2pManager mWifiManager;
    private Channel mChannel;


    private Handler mServiceHandler;
    private Handler handler = new Handler(this);

    CommunicationManager manager;

    // Views //
    private Button mSenderButton;
    private Button mReceiverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup intent filters for receiver:
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Initialize wifi manager:
        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiManager.initialize(this, getMainLooper(), null);

        // Configure wifi manager:
        mWifiManager.setDnsSdResponseListeners(mChannel, this, this);

        // Initialize wifi receiver:
        mWifiReceiver = new WifiBroadcastReceiver(mWifiManager, mChannel, this);

        // Setup chooser buttons:
        mSenderButton = (Button) findViewById(R.id.btn_sender);
        mSenderButton.setOnClickListener(this);

        mReceiverButton = (Button) findViewById(R.id.btn_receiver);
        mReceiverButton.setOnClickListener(this);

        mServiceHandler = new Handler(getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start discovering peers:
        discoverPeers();
        // Register wifi receiver:
        registerReceiver(mWifiReceiver, mIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister wifi receiver:
        unregisterReceiver(mWifiReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onClick(View view) {
        // Handle view clicks:
        switch (view.getId()) {
            case R.id.btn_sender:
                Log.i(TAG, "Clicked sender button");
                // Launch sender fragment:
                launchSenderFragment();
                // Register service for discovery:
                registerDnsService();
                //TODO: initiate communication
                break;
            case R.id.btn_receiver:
                Log.i(TAG, "Clicked receiver button");
                //TODO: launch ReceiverFragment
                // Discover services:
                discoverServices();
                break;
        }
    }

    /**
     * Switches UI visibility to either show the fragment content or 'chooser' content.
     *      show = true -> Shows 'chooser' content
     *      show = false -> Shows fragment content
     * @param show
     */
    private void showContentChooserArea(boolean show) {
        // Transition to appropriate content:
        findViewById(R.id.content_chooser).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.content_frame).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /////////////////////////////////////////////////
    ////////////////// NAVIGATION ///////////////////
    /////////////////////////////////////////////////

    @Override
    public void onBackPressed() {
        if(getFragmentManager().getBackStackEntryCount() > 0) {
            // Pop fragment stack:
            getFragmentManager().popBackStack();
            // Show 'chooser' content:
            showContentChooserArea(true);
        } else {
            super.onBackPressed();
        }
    }

    private void launchSenderFragment() {
        // Launch sender fragment:
        launchFragment(SenderFragment.newInstance(), SenderFragment.TAG);
        // Transition to fragment content:
        showContentChooserArea(false);
    }

    private void launchReceiverFragment() {
        //TODO: implement launch receiver
    }

    /**
     * Launches fragment with a given tag adding it to the backstack.
     * @param fragment
     * @param tag
     */
    private void launchFragment(Fragment fragment, String tag) {
        if(fragment == null) {
            return;
        }

        // Launch fragment w/ tag:
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    ///////////////////////////////////////////////////
    ////////////////// CONNECTIONS ////////////////////
    ///////////////////////////////////////////////////

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        //TODO: handle connection info available
        Log.i(TAG, "Received connection info...");
        // Initialize handler:
        Thread handler = null;

        // Determine handler type:
        if(wifiP2pInfo.isGroupOwner) {
            Log.i(TAG, "Phone is owner of group!");
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new ReceiverSocketHandler(this.handler);
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }

        } else {
            Log.i(TAG, "Phone is not owner of group");
            Log.d(TAG, "Connected as peer");
            Log.d(TAG, "Handler: "+this.handler);
            handler = new SenderSocketHandler(
                    this.handler,
                    wifiP2pInfo.groupOwnerAddress);
            handler.start();
        }
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String type, WifiP2pDevice device) {
        Log.i(TAG, "Discovered Service!!!");
        if(StringUtils.equals(instanceName, SERVICE_INSTANCE)) {
            Log.i(TAG, "Service discovered was our service!!!");
            Log.i(TAG, "Device Name: "+device.deviceName);
            // Connect to service:
            connectToService(instanceName, type, device);
        }
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String s, Map<String, String> map, WifiP2pDevice wifiP2pDevice) {
        Log.i(TAG , "Got HEre");
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.i(TAG, "Message: "+message.what);
        switch (message.what) {
            case CommunicationManager.RECEIVED:
                byte[] readBuf = (byte[]) message.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, message.arg1);
                Log.d(TAG, readMessage);
                break;
            case CommunicationManager.START:
                manager = (CommunicationManager) message.obj;
                break;
        }
        return false;
    }

    public class WriteTask extends AsyncTask<Void, Void, Object> {
        CommunicationManager manager;
        String message;
        public WriteTask(CommunicationManager manager, String message) {
            this.manager = manager;
            this.message = message;
        }

        @Override
        protected Object doInBackground(Void... voids) {

            manager.write(message.getBytes());
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {

        }
    }


    private void registerDnsService() {
        Log.i(TAG, "Registering DNS service for discovery...");

        mWifiManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Cleared local services");
                // Initialize record:
                Map<String, String> record = new HashMap<String, String>();
                //TODO: add necessary arguments to record? might not need

                // Define dns service:
                WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                        SERVICE_INSTANCE, SERVICE_TYPE, record);

                // Add service:
                mWifiManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Successfully added service!");
//                        discoverPeers();
                    }

                    @Override
                    public void onFailure(int error) {
                        Log.i(TAG, "Failed to add service: "+error);
                    }
                });
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "Failed to clear local services: "+i);
            }
        });
    }

    private void discoverPeers() {
        Log.i(TAG, "Discovering peers...");
        mWifiManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
//        mServiceHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                discoverPeers();
//            }
//        }, 10000);
    }

    private void discoverServices() {
        // Define service request:
        if(mServiceRequest != null) {
            mWifiManager.removeServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

                    // Add request:
                    mWifiManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "Successfully added service request");
                            mWifiManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Log.i(TAG, "Started discovering services...");
                                }

                                @Override
                                public void onFailure(int error) {
                                    Log.i(TAG, "Failed to start discovering services");
                                }
                            });
                        }

                        @Override
                        public void onFailure(int error) {
                            Log.i(TAG, "Failed to add service discovery request: " + error);
                        }
                    });
                }

                @Override
                public void onFailure(int i) {

                }
            });
        } else {
            mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

            // Add request:
            mWifiManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Successfully added service request");
                    mWifiManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "Started discovering services...");
                        }

                        @Override
                        public void onFailure(int error) {
                            Log.i(TAG, "Failed to start discovering services");
                        }
                    });
                }

                @Override
                public void onFailure(int error) {
                    Log.i(TAG, "Failed to add service discovery request: " + error);
                }
            });
        }

    }

    private void connectToService(String instanceName, String type, WifiP2pDevice device) {
        Log.i(TAG, "Connecting to service...");
        Log.d(TAG, "Service Name: "+instanceName);
        Log.d(TAG, "Service Type: "+type);
        Log.d(TAG, "Device Name: "+device.deviceName);
        // Define wifi configuration:
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if(mServiceRequest != null) {
            mWifiManager.removeServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Removed service request after connecting");
                }

                @Override
                public void onFailure(int i) {
                    Log.i(TAG, "Unable to remove request: "+i);
                }
            });


            // Connect to device:
            mWifiManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Connected to device!");
                }

                @Override
                public void onFailure(int i) {
                    Log.i(TAG, "Unable to connect to device: "+i);
                }
            });
        }
    }

    ///////////////////////////////////////////////////
    /////////////// SEND COMMUNICATION ////////////////
    ///////////////////////////////////////////////////


    @Override
    public void sendBirthday(String name, Date birthDay) {
        //TODO: implement sending birthday
        Log.d(TAG, "Name: "+name);
        Log.d(TAG, "Birth Day: "+SenderFragment.BIRTHDAY_FORMAT.format(birthDay));

        String message = TextUtils.join("|", new String[] {name, SenderFragment.BIRTHDAY_FORMAT.format(birthDay)});
        new WriteTask(manager, message).execute(null, null);
    }
}
