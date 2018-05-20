package com.danmcd.lanbday;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by dan on 5/19/18.
 */

public class MemberSocketHandler extends Thread {


    private static final String TAG = "MemberSocketHandler";

    // Connections //
    private Handler handler;
    private CommunicationManager manager;
    private InetAddress address;

    public MemberSocketHandler(Handler handler, InetAddress groupAddress) {
        this.handler = handler;
        address = groupAddress;

    }

    @Override
    public void run() {
        // Initialize socket:
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(address.getHostAddress(),
                    MainActivity.SERVER_PORT), 5000);

            // Define communication manager:
            manager = new CommunicationManager(socket, handler);
            new Thread(manager).start();
        } catch (IOException e) {
            e.printStackTrace();
            handler.obtainMessage(CommunicationManager.CONNECTION_REFUSED);
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    public CommunicationManager getManager() {
        return manager;
    }

}
