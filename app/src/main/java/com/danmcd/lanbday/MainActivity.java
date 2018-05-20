package com.danmcd.lanbday;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SenderFragment.SenderInteractions, ConnectionListener,
        WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.DnsSdTxtRecordListener,
        Handler.Callback{

    private static final String TAG = "MainActivity";

    // Service //
    private static final int SERVICE_DISCOVERY_TIMEOUT = 20000; // Every 20 seconds
    private static final String SERVICE_INSTANCE = "_landbay";
    private static final String SERVICE_TYPE = "_presence._tcp";

    public static int SERVER_PORT = 8989;

    private WifiP2pDnsSdServiceRequest mServiceRequest;
    private WifiP2pServiceInfo mServiceInfo;
    private final ArrayList<ServiceDiscoveryTask> mServiceDiscoveryTasks = new ArrayList<>();
    private boolean mIsDiscoveringServices = false;
    private boolean mConnectedToService = false;

    // Communication //
    private IntentFilter mIntentFilter = new IntentFilter();
    private WifiBroadcastReceiver mWifiReceiver;
    private WifiP2pManager mWifiManager;
    private Channel mChannel;


    private Handler mServiceHandler;
    private Handler handler = new Handler(this);
    private Thread mCommunicationThread;
    private CommunicationManager manager;

    // General //
    private static final int UNDETERMINED = 0; // Undetermined state (neither sender nor receiver)
    private static final int SENDER = 1; // Configured for sending data.
    private static final int RECEIVER = 2; // Configured for receiving data

    private int mCurrentState = UNDETERMINED; // Current config state of the app

    // Views //
    private LinearLayout mStatusContent;
    private TextView mStatusTextView;

    private Button mSenderButton;
    private Button mReceiverButton;

    // TODO: need to involve service managament in the activity lifecycle
    // TODO: need to involve communication w.r.t. fragment navigation

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

        // Setup status content:
        mStatusContent = (LinearLayout) findViewById(R.id.content_progress);
        mStatusTextView = (TextView) findViewById(R.id.tv_status);

        mServiceHandler = new Handler(getMainLooper());

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "On Start");
        // Register wifi receiver:
        registerReceiver(mWifiReceiver, mIntentFilter);
        // Begin to continuously discover services:
        startDiscoverServices();

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
        // Stop discovering services:
        stopDiscoveringServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove group:
        removeGroupFromChannel();
        // Remove persistent groups:
        removePersistentGroups();
        // Remove local service:
        removeService();
        // Disconnect from peer:
        disconnectFromDevice();
    }

    @Override
    public void onClick(View view) {
        // Handle view clicks:
        switch (view.getId()) {
            case R.id.btn_sender:
                // Set state to 'sender':
                setState(SENDER);
                // Show status content:
                showStatusContent(true);
                // Register service for discovery:
                addLocalService();
                break;
            case R.id.btn_receiver:
                // Set state to 'receiver':
                setState(RECEIVER);
                // Show status content:
                showStatusContent(true);
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
        findViewById(R.id.content_progress).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.content_frame).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void setState(int state) {
        mCurrentState = state;
    }

    /////////////////////////////////////////////////
    //////////////////// STATUS /////////////////////
    /////////////////////////////////////////////////
    private void showStatusContent(boolean show) {
        // Transition to appropriate content:
        findViewById(R.id.content_progress).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.content_chooser).setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.content_frame).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Updates the status to show the user the current state of their local connection.
     * @param status
     */
    private void updateStatus(String status) {
        if(mStatusTextView == null) {
            return;
        }

        // Set status text:
        mStatusTextView.setText(status);
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

    /**
     * Timed task to initiate a new services discovery. Will recursively submit
     * a new task as long as isDiscovering is true
     */
    private class ServiceDiscoveryTask extends TimerTask {
        public void run() {
            discoverServices();
            // Submit the next task if a stop call hasn't been made
            if (mIsDiscoveringServices) {
                submitServiceDiscoveryTask();
            }
            // Remove this task from the list since it's complete
            mServiceDiscoveryTasks.remove(this);
        }
    }


    @Override
    public void onConnectionStateChanged(boolean connected) {
        Log.i(TAG, "Connected: "+connected);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.i(TAG, "Received connection info...");

        /*
            Currently we'll restrict communication between 2 devices w/ a
            single service.
         */
        if(mCommunicationThread != null) {
            Log.i(TAG, "Communication thread has already been set up");
            return;
        }


        // Determine handler type:
        if(wifiP2pInfo.isGroupOwner) {
            Log.i(TAG, "Phone is owner of group");
            Log.d(TAG, "Connected as group owner");
            try {
                mCommunicationThread = new OwnerSocketHandler(this.handler);
                mCommunicationThread.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create owner handler - " + e.getMessage());
                return;
            }
        } else {
            Log.i(TAG, "Phone is not owner of group");
            Log.d(TAG, "Connected as member");
            Log.d(TAG, "Group owner address: "+wifiP2pInfo.groupOwnerAddress);
            /*
                Communication w/ a peer has been established. At this
                point status content should be hidden and we should
                transition to the SenderFragment.
             */
            // Define communication thread:
            mCommunicationThread = new MemberSocketHandler(
                    this.handler,
                    wifiP2pInfo.groupOwnerAddress);

            mCommunicationThread.start();
        }

        // Set connected to service:
        setConnectedToService(true);

        // Launch sender/receiver fragment:
        if(mCurrentState == SENDER) {
            launchSenderFragment();
        }
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String type, WifiP2pDevice device) {
        Log.i(TAG, "Discovered Service!!!");
        /*
            Prevent the client from connecting to services until the
            user has decided if they are going to be the sender or
            receiver of data.
         */
        if(mCurrentState == UNDETERMINED) {
            Log.w(TAG, "Found available service, but no state selected");
            return;
        }

        /*
            We would only like to connect to services that match our
            communication service.
         */
        if(StringUtils.equals(instanceName, SERVICE_INSTANCE)) {
            Log.i(TAG, "Service discovered was our service!!!");
            Log.i(TAG, "Device Name: "+device.deviceName);
            Log.i(TAG, "Device Address: "+device.deviceAddress);
            Log.i(TAG, "Group Owner: "+device.isGroupOwner());
            // Connect to service:
            connectToService(instanceName, type, device);
        }
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String instanceName, Map<String, String> record, WifiP2pDevice wifiP2pDevice) {
        Log.i(TAG, "Received dns text record...");
        Log.d(TAG, "Instance Name: "+instanceName);
        for(String key : record.keySet()) {
            Log.d(TAG, "Key: "+key);
            Log.d(TAG, "Value: "+record.get(key));
        }
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


    /**
     * Registers local service that other devices are capable of finding and connecting to.
     */
    private void addLocalService() {
        Log.i(TAG, "Registering DNS service for discovery...");

        // Clear any other local services in channel:
        updateStatus("Clearing unused local services...");
        mWifiManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Initialize record:
                Map<String, String> record = new HashMap<String, String>();

                // Define dns service:
                mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                        SERVICE_INSTANCE, SERVICE_TYPE, record);

                // Add local service:
                updateStatus("Adding service...");
                mWifiManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        updateStatus("Waiting for people who want to know your birthday");
                        Log.i(TAG, "Successfully added service!");
                    }

                    @Override
                    public void onFailure(int error) {
                        Log.i(TAG, "Failed to add service: "+error);
                        //TODO: handle failure to add local service
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
                Log.i(TAG, "Able to start discovering peers");
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "Unable to start discovering peers");
            }
        });
//        mServiceHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                discoverPeers();
//            }
//        }, 10000);
    }

    private void startDiscoverServices() {
        Log.i(TAG, "Starting service discovery...");
        if (mIsDiscoveringServices){
            Log.w(TAG, "Services are still discovering, do not need to make this call");
        } else {
            addServiceRequest();
            // Make discover call and first discover task submission
            discoverServices();
            submitServiceDiscoveryTask();
        }
    }

    private void stopDiscoveringServices() {
        Log.i(TAG, "Stopping service discovery...");
        if (mIsDiscoveringServices) {
            // Cancel remaining discover tasks:
            for(ServiceDiscoveryTask task : mServiceDiscoveryTasks) {
                task.cancel();
            }
            // Clear discovery tasks:
            mServiceDiscoveryTasks.clear();
            // Set to NOT discovering:
            mIsDiscoveringServices = false;
            // Clear service request (if any exist):
            clearServiceRequests();
        }
    }

    private void discoverServices() {
        // Set discovering services:
        mIsDiscoveringServices = true;
        // Discover services:
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

    /**
     * Submits a new task to initiate service discovery after the discovery
     * timeout period has expired
     */
    private void submitServiceDiscoveryTask(){
        Log.i(TAG, "Submitting service discovery task");
        // Discover times out after 2 minutes so we set the timer to that
        int timeToWait = SERVICE_DISCOVERY_TIMEOUT;
        ServiceDiscoveryTask serviceDiscoveryTask = new ServiceDiscoveryTask();
        Timer timer = new Timer();
        // Submit the service discovery task and add it to the list
        timer.schedule(serviceDiscoveryTask, timeToWait);
        mServiceDiscoveryTasks.add(serviceDiscoveryTask);
    }



    private void addServiceRequest() {
        // Define new service request:
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        // Tell the framework we want to scan for services. Prerequisite for discovering services
        mWifiManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Service discovery request added");
            }

            @Override
            public void onFailure(int error) {
                Log.i(TAG, "Failed to add service request: "+error);
                mServiceRequest = null;
            }
        });
    }

    private void removeService() {
        if(mServiceInfo != null) {
            Log.i(TAG, "Removing local service...");
            mWifiManager.removeLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Reset service info:
                    mServiceInfo = null;
                }

                @Override
                public void onFailure(int i) {
                    Log.e(TAG, "Failed to remove local service...");
                }
            });
        }
    }

    private void removePersistentGroups() {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Remove any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mWifiManager, mChannel, netid, null);
                    }
                }
            }
            Log.i(TAG, "Persistent groups removed");
        } catch(Exception e) {
            Log.e(TAG, "Failure removing persistent groups: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connectToService(String instanceName, String type, WifiP2pDevice device) {
        Log.i(TAG, "Connecting to service...");
        Log.d(TAG, "Service Name: "+instanceName);
        Log.d(TAG, "Service Type: "+type);
        Log.d(TAG, "Device Name: "+device.deviceName);
        // Define wifi configuration:
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if(mServiceRequest != null) {
            mWifiManager.removeServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Removed service request after connecting");
                    updateStatus("Connecting to service...");
                    connectToDevice(config);
                }

                @Override
                public void onFailure(int i) {
                    Log.i(TAG, "Unable to remove request: "+i);
                }
            });
        }
    }

    private void connectToDevice(WifiP2pConfig config) {
        // Connect to device:
        mWifiManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Connected to device!");
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "Unable to connect to device: "+i);
                // Notify user unable to connect to service:
                Utils.showToast(MainActivity.this, "Unable to connect to service");
            }
        });
    }

    private void clearServiceRequests() {
        Log.i(TAG, "Clearing service requests...");
        mWifiManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mServiceRequest = null;
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "Failed to clear service request");
            }
        });
    }

    private void clearLocalServices() {
        mWifiManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Able to ");
            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    private void removeGroupFromChannel() {
        // Remove group in channel:
        mWifiManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Removed group!");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Failed to remove group: "+i);
            }
        });
    }

    private void disconnectFromDevice() {
        mWifiManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Cancelled connections");
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "Unable to cancel connections");
            }
        });
    }

    private void setConnectedToService(boolean connected) {
        mConnectedToService = connected;
    }

    private boolean isConnectedToService() {
        return mConnectedToService;
    }

    /**
     * Gets the communication manager depending on the clients current state in the group
     * i.e. if they are the group owner or not.
     * @return
     */
    private CommunicationManager getCommunicationManager() {
        return null;
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
        CommunicationManager manager = null;
        if(mCommunicationThread instanceof MemberSocketHandler) {
            manager = this.manager;
        } else if(mCommunicationThread instanceof OwnerSocketHandler){
            manager = ((OwnerSocketHandler) mCommunicationThread).getManager();
        }

        new WriteTask(manager, message).execute(null, null);
        // Notify birthday was sent:
        Utils.showToast(this, "Sent Birthday");
    }
}
