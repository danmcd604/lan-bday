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
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
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
    private RelativeLayout mStatusContent;
    private TextView mStatusTextView;
    private Button mStopButton;

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
        mStatusContent = (RelativeLayout) findViewById(R.id.content_progress);
        mStatusTextView = (TextView) findViewById(R.id.tv_status);
        mStopButton = (Button) findViewById(R.id.btn_stop_progress);
        mStopButton.setOnClickListener(this);

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
        if(!isConnectedToService()) {
            startDiscoverServices();
        }

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
        // Disconnect from peer:
        disconnectFromDevice();
        // Remove group:
        removeGroupFromChannel();
        // Remove persistent groups:
        removePersistentGroups();
        // Remove service request:
        removeServiceRequest();
        // Remove local service:
        removeService();
        // Clear local services:
        clearLocalServices();
        // Stop handlers:
        if(mCommunicationThread instanceof Closeable) {
            try {
                ((Closeable) mCommunicationThread).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Clear response listeners:
        mWifiManager.setDnsSdResponseListeners(mChannel, null, null);
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
                // Update status:
                updateStatus("Searching for services...");
                // Show status content:
                showStatusContent(true);
                // Discover services:
                discoverServices();
                break;
            case R.id.btn_stop_progress:
                // Stop action currently associated w/ state:
                stopProcessingState(mCurrentState);
                // Set state to 'undetermined':
                setState(UNDETERMINED);
                // Show chooser content:
                showContentChooserArea(true);
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
        findViewById(R.id.content_progress).setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.content_frame).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showFragentContentArea(boolean show) {
        findViewById(R.id.content_frame).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.content_progress).setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.content_chooser).setVisibility(show ? View.GONE : View.VISIBLE);
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
            Log.i(TAG, "Back Pressed");
            // Disconnect from service:
            disconnectFromService();
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
        showFragentContentArea(true);
    }

    private void launchReceiverFragment() {
        // Launch receiver fragment:
        launchFragment(ReceiverFragment.newInstance(), ReceiverFragment.TAG);
        // Transition to fragment content:
        showFragentContentArea(true);
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

    private Fragment getActiveFragment() {
        return getFragmentManager().findFragmentById(R.id.content_frame);
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
        Log.i(TAG, "Connected to Service: "+isConnectedToService());
        Log.i(TAG, "Connected: "+connected);

        // Handle connected to service/not connected to peer:
        if(!connected && isConnectedToService()) {
            // Disconnect from serviceL
            disconnectFromService();
            // Set state:
            setState(UNDETERMINED);
            // Show chooser content:
            showContentChooserArea(true);
            // Notify user disconnected:
            Utils.showToast(this, "Disconnected from service");
        }
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

        // Handle state undetermined:
        if(mCurrentState == UNDETERMINED) {
            // Disconnect from peer:
            disconnectFromDevice();
            // Remove group:
            removeGroupFromChannel();
            // Remove persistent groups:
            removePersistentGroups();
            // Reset connection state:
            resetConnectionState();
            return;
        }

        /*
            Because we are enforcing a single P2P connection,
            we do not need to continue discovering services.
         */
        // Stop discovering services:
        stopDiscoveringServices();

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
        } else if(mCurrentState == RECEIVER) {
            launchReceiverFragment();
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
        if(mCurrentState == UNDETERMINED || mCurrentState == SENDER) {
            Log.w(TAG, "Found available service, but application state is not set to received data");
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
                // Handle message received:
                onBirthDayReceived(readMessage);
                break;
            case CommunicationManager.START:
                manager = (CommunicationManager) message.obj;
                break;
            case CommunicationManager.CONNECTION_REFUSED:
                Log.e(TAG, "Connection has been refused");
                /*
                    Connection to peer has been refused. At this point
                    the client should disconnect from the peer, and UI
                    should be updated.
                 */
                // Disconnect from peer:
                disconnectFromDevice();
                // Remove group:
                removeGroupFromChannel();
                // Remove persistent groups:
                removePersistentGroups();
                // Reset connection state:
                resetConnectionState();
                break;
        }
        return false;
    }

    /**
     * Stops any ongoing processes for the given state. For instance stopping the 'sender'
     * state will remove the local service.
     *
     * Assumptions:
     *      1) Client is not connected to a peer
     * @param state
     */
    private void stopProcessingState(int state) {
        switch (state) {
            case SENDER:
                removeService();
                break;
            case RECEIVER:
                break;
        }
    }


    /**
     * Registers local service that other devices are capable of finding and connecting to.
     */
    private void addLocalService() {
        Log.i(TAG, "Registering DNS service for discovery...");

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
            removeServiceRequest();
        }
        // Connect to service:
        updateStatus("Connecting to service...");
        connectToDevice(config);

    }

    private void disconnectFromService() {
        // Remove group:
        removeGroupFromChannel();
        removePersistentGroups();
        // Clear local services:
        clearLocalServices();
        // Disconnect from device:
        disconnectFromDevice();
        // Reset connection state:
        resetConnectionState();
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

    private void removeServiceRequest() {
        Log.i(TAG, "Removing service request");
        if(mServiceRequest == null) {
            Log.d(TAG, "Currently there is no active service request");
            return;
        }
        mWifiManager.removeServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Successfully start removing request");
            }

            @Override
            public void onFailure(int error) {
                Log.i(TAG, "Failed to remove request: "+error);
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

    /**
     * Stop any communciation thread and start discovering services again.
     */
    private void resetConnectionState() {
        Log.i(TAG, "Resetting connection state...");

        // Stop communication thread:
        if(mCommunicationThread != null) {
            mCommunicationThread.interrupt();
            mCommunicationThread = null;
        }

        // Set disconnected from services:
        setConnectedToService(false);

        // Start discovering services:
        startDiscoverServices();
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
        CommunicationManager manager = null;
        if(mCommunicationThread instanceof MemberSocketHandler) {
            manager = this.manager;
        } else if(mCommunicationThread instanceof OwnerSocketHandler){
            manager = ((OwnerSocketHandler) mCommunicationThread).getManager();
        }

        return manager;
    }

    ///////////////////////////////////////////////////
    /////////////// SEND COMMUNICATION ////////////////
    ///////////////////////////////////////////////////

    /**
     * Handles birthday information being received by the client. Birthday
     * information is formatted as "<name>|<birth_date>".
     * @param rawData
     */
    public void onBirthDayReceived(String rawData) {
        Log.i(TAG, "Received birthday information");
        if(TextUtils.isEmpty(rawData)) {
            Log.w(TAG, "Received empty data");
            return;
        }
        // Check to see if data matches expected format:
        if(!rawData.matches("\\.*|\\.*")){
            Log.w(TAG, "Data doesn't match expected format");
        }

        // Parse raw data:
        String[] splitData = rawData.split("\\|");

        String name = null;
        Date birthDate = null;
        try {
            name = splitData[0];
            birthDate = SenderFragment.BIRTHDAY_FORMAT.parse(splitData[1]);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Notify 'receiver' that data was obtained:
        ((ReceiverFragment) getActiveFragment()).setBirthdayInfo(name, birthDate);
    }


    @Override
    public void sendBirthday(String name, Date birthDay) {
        Log.i(TAG, "Attempting to send birthday...");
        Log.d(TAG, "Name: "+name);
        Log.d(TAG, "Birth Day: "+SenderFragment.BIRTHDAY_FORMAT.format(birthDay));

        // Build formatted message:
        String message = TextUtils.join("|", new String[] {name, SenderFragment.BIRTHDAY_FORMAT.format(birthDay)});
        //TODO: minimize message size

        // Get communication manager:
        CommunicationManager manager = getCommunicationManager();

        // Execute write task:
        new WriteTask(manager, message).execute(null, null);
        //TODO: this task shoudl be managed in the lifecycle of the activity

        // Notify birthday was sent:
        Utils.showToast(this, "Sent Birthday");
    }
}
